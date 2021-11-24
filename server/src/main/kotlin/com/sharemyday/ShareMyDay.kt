package com.sharemyday

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.core.security.RouteRole
import okhttp3.OkHttpClient
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.startKoin
import org.koin.dsl.module

private val CORS_ORIGIN_FOR_DEBUG = System.getenv("CORS_ORIGIN_FOR_DEBUG")

fun main() {
    Database.connect(
        "jdbc:postgresql://localhost:5433/sharemyday",
        driver = "org.postgresql.Driver", user = "sharemydayadmin", password = "testpass"
    )

    transaction {
        SchemaUtils.create(Sessions, Users, Days, DayPhotos);
    }

    startKoin {
        modules(
            module {
                single { OkHttpClient() }
                single<ObjectMapper> { jacksonObjectMapper().findAndRegisterModules() }
                single<PhotoService> { LocalPhotoService("/home/bill/tmp") }
            })
    }

    val app = Javalin.create { config ->
        config.enableCorsForOrigin(CORS_ORIGIN_FOR_DEBUG)
        config.accessManager { handler, ctx, routeRoles ->
            if (routeRoles.isEmpty()) {
                handler.handle(ctx)
                return@accessManager
            }
            val sessionId = ctx.cookie("sessionId");
            if (sessionId == null) {
                ctx.status(401).result("{\"error\": \"Please login\"}")
                return@accessManager
            }
            val user = transaction {
                return@transaction Session.findById(sessionId)?.load(Session::user)?.user
            }
            if (user == null) {
                ctx.status(401).result("{\"error\": \"Please login again.\"}")
                return@accessManager
            }
            ctx.attribute("user", user)
            handler.handle(ctx)
        }
    }.start(8000)

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
        path("api") {
            path("user", UserEndpoints())
            path("days", DaysEndpoints())
        }
        path("auth", AuthEndpoints())
    }
}

enum class RouteRoles : RouteRole {
    AUTHENTICATED, ADMIN
}

