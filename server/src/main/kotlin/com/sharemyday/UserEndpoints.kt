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
        get("me", { ctx ->
            val user = ctx.attribute<User>("user")!!
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
        }, RouteRoles.AUTHENTICATED)
    }
}

data class MeResponse(
    val id: String,
    val name: String?, val occupation: String?, val location: String?, val redditUsername: String?)