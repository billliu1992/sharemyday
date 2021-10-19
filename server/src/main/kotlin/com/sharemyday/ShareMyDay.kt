package com.sharemyday

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sharemyday.Users.nullable
import com.sharemyday.Users.uniqueIndex
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.get
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import java.time.Instant
import java.util.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.SecureRandom
import java.time.Duration

const val REDDIT_CLIENT_ID = "-xHId8au1RQpKyqWsKs-wQ"
private val REDDIT_CLIENT_SECRET = System.getenv("REDDIT_CLIENT_SECRET")
const val REDDIT_REDIRECT_URL = "http://localhost:8000/auth/reddit/redirect"
const val REDDIT_SCOPE = "identity"

private val SESSION_ID_VALID_CHARS = ((48..57) + (65..90) + (97..122)).map { it.toChar() }

private val httpClient = OkHttpClient()
private val jsonMapper = jacksonObjectMapper().findAndRegisterModules()
    .setPropertyNamingStrategy(PropertyNamingStrategies.SnakeCaseStrategy())
private val base64Encoder = Base64.getUrlEncoder()
private val db = Database.connect(
    "jdbc:postgresql://localhost:5433/sharemyday",
    driver = "org.postgresql.Driver", user = "sharemydayadmin", password = "testpass")
private val random = SecureRandom();



fun main() {
    transaction {
        SchemaUtils.create(Sessions, Users);
    }

    val app = Javalin.create().start(8000)
    app.get("/") { ctx ->
        val sessionId = ctx.cookie("sessionId");
        if (sessionId == null) {
            ctx.status(401).result("Please login")
            return@get
        }
        val redditUsername = transaction {
            return@transaction Session.findById(sessionId)?.load(Session::user)?.user?.redditUsername
        }
        if (redditUsername == null) {
            ctx.status(401)
                .cookie("sessionId", "", 0)
                .result("Couldn't find session.")
            return@get
        }

        ctx.result("Got user $redditUsername")
    }

    app.routes {
        path("auth/reddit") {
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

                val nameRequest = Request.Builder()
                    .url("https://oauth.reddit.com/api/v1/me")
                    .header(
                        "Authorization",
                        "bearer ${resp.accessToken}")
                    .build()

                val username = httpClient.newCall(nameRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        ctx.status(400).result(
                            "Fetching code not successful, got error: $response")
                        return@get
                    }
                    return@use jsonMapper.readValue<Map<String, Any>>(response.body!!.string())["name"]!! as String
                }

                val newSessionId = (1..32).map {
                    SESSION_ID_VALID_CHARS[random.nextInt(SESSION_ID_VALID_CHARS.size)]
                }.joinToString("")

                println(SESSION_ID_VALID_CHARS)
                println(newSessionId)

                val user = transaction {
                    return@transaction User.find {Users.redditUsername eq username}.firstOrNull()
                }

                transaction {
                    Session.new(newSessionId) {
                        this.user = user ?:
                            User.new(UUID.randomUUID()) {
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

object Sessions: IdTable<String>() {
    override val id = text("id").entityId()
    override val primaryKey = PrimaryKey(id)
    val user = reference("user", Users)
}

class Session(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Session>(Sessions)
    var user by User referencedOn Sessions.user
}

object Users: UUIDTable() {
    val name = text("name").nullable()
    val occupation = text("occupation").nullable()
    val location = text("location").nullable()

    val redditUsername = text("reddit_username").uniqueIndex().nullable()
    val redditAccessToken = text("reddit_access_token").uniqueIndex().nullable()
    val redditRefreshToken =  text("reddit_refresh_token").nullable()
    val redditTokenExpiresAt = timestamp("reddit_token_expires_at").nullable()
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var name by Users.name
    var occupation by Users.occupation
    var location by Users.location
    var redditUsername by Users.redditUsername
    var redditAccessToken by Users.redditAccessToken
    var redditRefreshToken by Users.redditRefreshToken
    var redditTokenExpiresAt by Users.redditTokenExpiresAt
}

data class RedditAuthTokenResponse(
    val accessToken: String, val tokenType: String, val expiresIn: Int,
    val scope: String, val refreshToken: String)