package com.sharemyday

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup;
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.*

const val REDDIT_CLIENT_ID = "-xHId8au1RQpKyqWsKs-wQ"
private val REDDIT_CLIENT_SECRET = System.getenv("REDDIT_CLIENT_SECRET")
const val REDDIT_REDIRECT_URL = "http://localhost:8000/auth/reddit/redirect"
const val REDDIT_SCOPE = "identity"

private val SESSION_ID_VALID_CHARS = ((48..57) + (65..90) + (97..122)).map { it.toChar() }

private val base64Encoder = Base64.getUrlEncoder()
private val random = SecureRandom()

class AuthEndpoints : EndpointGroup, KoinComponent {

    private val jsonMapper: ObjectMapper by inject()
    private val httpClient: OkHttpClient by inject()

    override fun addEndpoints() {
        path("reddit") {
            get("start") { ctx ->
                val randomString = "nicestate"
                val redirect =
                    "https://www.reddit.com/api/v1/authorize" +
                            "?client_id=$REDDIT_CLIENT_ID&response_type=code" +
                            "&state=$randomString&redirect_uri=$REDDIT_REDIRECT_URL" +
                            "&duration=permanent&scope=$REDDIT_SCOPE"
                ctx.redirect(redirect, 303)
            }

            get("redirect") { ctx ->
                val error = ctx.queryParam("error")
                val code = ctx.queryParam("code")
                val state = ctx.queryParam("state")

                if (error != null) {
                    ctx.status(400).result("Redirect got error code: $error")
                    return@get
                }
                if (code == null) {
                    throw RuntimeException("Got a null code")
                }

                val request = Request.Builder()
                    .url("https://www.reddit.com/api/v1/access_token")
                    .post(
                        FormBody.Builder()
                            .add("grant_type", "authorization_code")
                            .add("code", code)
                            .add("redirect_uri", REDDIT_REDIRECT_URL)
                            .build()
                    )
                    .header(
                        "Authorization",
                        "Basic ${
                            base64Encoder.encodeToString("$REDDIT_CLIENT_ID:$REDDIT_CLIENT_SECRET".toByteArray())
                        }"
                    )
                    .build()

                val resp = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        ctx.status(400).result(
                            "Fetching code not successful, got error: $response"
                        )
                        return@get
                    }
                    return@use jsonMapper.readValue<RedditAuthTokenResponse>(response.body!!.string())
                }

                val nameRequest = Request.Builder()
                    .url("https://oauth.reddit.com/api/v1/me")
                    .header(
                        "Authorization",
                        "bearer ${resp.accessToken}"
                    )
                    .build()

                val username = httpClient.newCall(nameRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        ctx.status(400).result(
                            "Fetching code not successful, got error: $response"
                        )
                        return@get
                    }
                    return@use jsonMapper.readValue<Map<String, Any>>(response.body!!.string())["name"]!! as String
                }

                val newSessionId = (1..32).map {
                    SESSION_ID_VALID_CHARS[random.nextInt(SESSION_ID_VALID_CHARS.size)]
                }.joinToString("")

                val user = transaction {
                    return@transaction User.find { Users.redditUsername eq username }.firstOrNull()
                }

                transaction {
                    Session.new(newSessionId) {
                        this.user = user ?: User.new(UUID.randomUUID()) {
                            redditAccessToken = resp.accessToken
                            redditUsername = username
                            redditRefreshToken = resp.refreshToken
                            redditTokenExpiresAt = Instant.now() + Duration.ofSeconds(resp.expiresIn.toLong())
                        }
                    }
                }


                ctx.cookie("sessionId", newSessionId, 3600)
                    .redirect("/", 303)
            }
        }
    }
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RedditAuthTokenResponse(
    val accessToken: String, val tokenType: String, val expiresIn: Int,
    val scope: String, val refreshToken: String)