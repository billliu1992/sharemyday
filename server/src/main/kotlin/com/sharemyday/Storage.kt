package com.sharemyday

import com.sharemyday.DayPhotos.nullable
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
    val name = text("name")
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

object Days: UUIDTable() {
    val user = reference("user", Users)

    val created = timestamp("created")
    val edited = timestamp("edited").nullable()

    // Longitude has max 180 degrees (3 digits), latitude has max 90 degrees (2 digits).
    // We cap the scale at 3, which should be more than enough precision to locate the city or country.
    val longitude = decimal("longitude", 6, 3)
    val latitude = decimal("latitude", 5, 3)

    val occupation = text("occupation")
    val occasion = text("occasion")
}

class Day(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Day>(Days)

    var user by User referencedOn Days.user
    var created by Days.created
    var edited by Days.edited
    var longitude  by Days.longitude
    var latitude  by Days.latitude
    var occupation by Days.occupation
    var occasion by Days.occasion
    val photos by DayPhoto referrersOn DayPhotos.day
}

object DayPhotos: UUIDTable() {
    val day = reference("day", Days)
    val created = timestamp("created")
    val edited = timestamp("edited").nullable()
    val fileType = text("fileType")
    val describedTime = text("described_time").nullable()
    val description = text("description").nullable()
    val order = integer("order")
}

class DayPhoto(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<DayPhoto>(DayPhotos)

    var day by Day referencedOn DayPhotos.day
    var fileType by DayPhotos.fileType
    var describedTime by DayPhotos.describedTime
    var created by DayPhotos.created
    var edited by DayPhotos.edited
    var description by DayPhotos.description
    var order by DayPhotos.order
}