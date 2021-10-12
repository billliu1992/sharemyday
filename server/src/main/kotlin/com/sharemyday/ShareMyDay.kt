package com.sharemyday

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.get
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

const val REDDIT_CLIENT_ID = "-xHId8au1RQpKyqWsKs-wQ"
private val REDDIT_CLIENT_SECRET = System.getenv("REDDIT_CLIENT_SECRET")
const val REDDIT_REDIRECT_URL = "http://localhost:8000/api/auth/reddit/redirect"
const val REDDIT_SCOPE = "identity"

private val httpClient = OkHttpClient()
private val jsonMapper = jacksonObjectMapper().findAndRegisterModules()
    .setPropertyNamingStrategy(PropertyNamingStrategies.SnakeCaseStrategy())
private val base64Encoder = Base64.getUrlEncoder()

fun main() {
    val app = Javalin.create().start(8000)
    app.get("/") { ctx -> ctx.result("Hello World") }

    app.routes {
        path("api/auth/reddit") {
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
                    .post(FormBody.Builder()
                        .add("grant_type", "authorization_code")
                        .add("code", code)
                        .add("redirect_uri", REDDIT_REDIRECT_URL)
                        .build())
                    .header(
                        "Authorization",
                        "Basic ${
                            base64Encoder.encodeToString("$REDDIT_CLIENT_ID:$REDDIT_CLIENT_SECRET".toByteArray())
                        }")
                    .build()

                val resp = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        ctx.status(400).result(
                            "Fetching code not successful, got error: $response")
                        return@get
                    }
                    return@use jsonMapper.readValue<RedditAuthTokenResponse>(response.body!!.string())
                }
                ctx.result("Got 'em: $resp")
            }
        }
    }
}

data class RedditAuthTokenResponse(
    val accessToken: String, val tokenType: String, val expiresIn: Int,
    val scope: String, val refreshToken: String)