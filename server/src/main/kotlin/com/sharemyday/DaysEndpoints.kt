package com.sharemyday

import io.javalin.apibuilder.ApiBuilder.put
import io.javalin.apibuilder.EndpointGroup
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal
import java.time.Instant
import java.util.*

private val base64Decoder = Base64.getDecoder()

class DaysEndpoints : EndpointGroup, KoinComponent {

    private val photoService: PhotoService by inject()

    override fun addEndpoints() {
        put("/new", { ctx ->
            val user = ctx.attribute<User>("user")!!
            val response = ctx.bodyStreamAsClass(NewDayRequest::class.java)
            val now = Instant.now()
            transaction {
                val dayId = UUID.randomUUID()
                val day = Day.new(dayId) {
                    this.user = user
                    created = now
                    longitude = BigDecimal(response.longitude)
                    latitude = BigDecimal(response.latitude)
                    occupation = response.occupation
                    occasion = response.occasion
                }
                response.photos.forEachIndexed { index, newPhotoResponse ->
                    val photoId = UUID.randomUUID()

                    val photoSplit = newPhotoResponse.imageBase64.split(",")

                    photoService.saveDayPhoto(
                        user.id.toString(), dayId.toString(), photoId.toString(), base64Decoder.decode(photoSplit[1])
                    )
                    DayPhoto.new(photoId) {
                        this.day = day
                        fileType = photoSplit[0].slice(photoSplit[0].indexOf(':')..photoSplit[0].indexOf(';'))
                        describedTime = newPhotoResponse.describedTime
                        created = now
                        description = newPhotoResponse.description
                        order = index
                    }
                }
            }
        }, RouteRoles.AUTHENTICATED)
    }
}

data class NewDayRequest(
    val occupation: String,
    val occasion: String,
    val longitude: Double,
    val latitude: Double,
    val photos: List<NewPhoto>
)

data class NewPhoto(
    val imageBase64: String,
    val describedTime: String?,
    val description: String?,
)