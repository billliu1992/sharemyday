package com.sharemyday

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserEndpoints : EndpointGroup, KoinComponent {
    private val jsonMapper: ObjectMapper by inject()

    override fun addEndpoints() {
        get("me") { ctx ->
            val sessionId = ctx.cookie("sessionId");
            if (sessionId == null) {
                ctx.status(401).result("Please login")
                return@get
            }
            val user = transaction {
                return@transaction Session.findById(sessionId)?.load(Session::user)?.user
            }
            if (user == null) {
                ctx.status(401)
                    .cookie("sessionId", "", 0)
                    .result("Please login")
                return@get
            }
            ctx.json(
                jsonMapper.writeValueAsString(
                    MeResponse(
                        id = user.id.toString(),
                        name = user.name,
                        occupation = user.occupation,
                        location = user.location,
                        redditUsername = user.redditUsername,
                    )
                )
            )
        }
    }
}

data class MeResponse(
    val id: String,
    val name: String?, val occupation: String?, val location: String?, val redditUsername: String?)