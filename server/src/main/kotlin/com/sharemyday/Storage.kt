package com.sharemyday

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.`java-time`.timestamp
import java.util.*

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