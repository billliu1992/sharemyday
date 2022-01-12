package com.sharemyday

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.put
import io.javalin.apibuilder.EndpointGroup
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal
import java.time.Instant
import java.util.*

private val base64Decoder = Base64.getDecoder()
private val base64Encoder = Base64.getEncoder()

const val COUNT_PER_PAGE = 10

class DaysEndpoints : EndpointGroup, KoinComponent {

    private val photoService: PhotoService by inject()
    private val jsonMapper: ObjectMapper by inject()

    override fun addEndpoints() {
        get("/photo/{photoId}", { ctx ->
            val photoId = ctx.pathParam("photoId")

            val photo = transaction {
                DayPhoto.findById(UUID.fromString(photoId))
            }

            if (photo == null) {
                ctx.status(404)
                return@get
            }

            var dayId: String? = null
            var userId: String? = null
            transaction {
                dayId = photo.day.id.toString()
                userId = photo.day.user.id.toString()
            }

            ctx.contentType(photo.fileType).result(photoService.getDayPhoto(userId!!, dayId!!, photoId))
        }, RouteRoles.AUTHENTICATED)

        get("/list", { ctx ->
            val since = ctx.queryParam("since")
            val page = ctx.queryParam("page")

            if (since == null || page == null) {
                ctx.status(400).result("'since' and 'page' query params required")
                return@get
            }

            ctx.json(jsonMapper.writeValueAsString(
                transaction {
                    val days = Day.find {
                        Days.created lessEq Instant.ofEpochSecond(since.toLong())
                    }.limit(COUNT_PER_PAGE, page.toLong() * COUNT_PER_PAGE)

                    ListDayResponse(
                        days.map { day ->
                            DayListEntry(
                                id = day.id.toString(),
                                author = Author(
                                    day.user.id.toString(),
                                    day.user.name,
                                ),
                                occupation = day.occupation,
                                occasion = day.occasion,
                                longitude = day.longitude.toDouble(),
                                latitude = day.latitude.toDouble(),
                                previewIds = day.photos.orderBy(DayPhotos.order to SortOrder.ASC).limit(5)
                                    .map { it.id.toString() })
                        })
                }))
        }, RouteRoles.AUTHENTICATED)

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
                        fileType = photoSplit[0].slice(photoSplit[0].indexOf(':')+1 until photoSplit[0].indexOf(';'))
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

data class Author(
    val id: String,
    val name: String
)

data class ListDayResponse(
    val days: List<DayListEntry>
)

data class DayListEntry(
    val id: String,
    val author: Author,
    val occupation: String,
    val occasion: String,
    val longitude: Double,
    val latitude: Double,
    val previewIds: List<String>
)

data class NewDayRequest(
    val occupation: String,
    val occasion: String,
    val longitude: Double,
    val latitude: Double,
    val photos: List<Photo>
)

data class Photo(
    val imageBase64: String,
    val describedTime: String?,
    val description: String?,
)