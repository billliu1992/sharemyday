package com.sharemyday

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.PlaceAutocompleteRequest.SessionToken
import com.google.maps.PlacesApi
import com.google.maps.model.AddressComponent
import com.google.maps.model.AddressComponentType
import com.google.maps.model.GeocodingResult
import com.google.maps.model.PlaceAutocompleteType
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MapEndpoints : EndpointGroup, KoinComponent {
    private val geoApiContext: GeoApiContext by inject()
    private val jsonMapper: ObjectMapper by inject()

    override fun addEndpoints() {
        get("search", { ctx ->
            val query = ctx.queryParam("query")
            val sessionToken = ctx.queryParam("session_token")

            if (query == null || sessionToken == null || query.isEmpty() || sessionToken.isEmpty()) {
                ctx.status(400)
                    .result("'query' and 'session_token' must both be populated. Got: $query, $sessionToken")
            }

            val results = PlacesApi.placeAutocomplete(geoApiContext, query, SessionToken(sessionToken))
                .language("en")
                .types(PlaceAutocompleteType.REGIONS)
                .await()

            ctx.json(
                jsonMapper.writeValueAsString(
                    MapSearchResponse(
                        results.map {
                            MapSearchEntry(
                                text = it.description,
                                entryId = it.placeId
                            )
                        })
                )
            )
        }, RouteRoles.AUTHENTICATED)

        get("details", { ctx ->
            val entryId = ctx.queryParam("entry_id")
            val results = GeocodingApi.newRequest(geoApiContext).place(entryId).await()

            if (results.isEmpty()) {
                throw RuntimeException("Somehow got no results from places ID: " + entryId)
            }


            results.first().run {
                MapDetailsResponse(
                    name = buildName(this),
                    lat = geometry.location.lat,
                    long = geometry.location.lng,
                )
            }.run {
                ctx.json(jsonMapper.writeValueAsString(this))
            }
        }, RouteRoles.AUTHENTICATED)
    }
}

private fun buildName(result: GeocodingResult): String {
    val maybeSpecificComponent = with(result.addressComponents) {
        findAddressComponentWithType(this, AddressComponentType.SUBLOCALITY) ?: findAddressComponentWithType(
            this,
            AddressComponentType.LOCALITY
        ) ?: findAddressComponentWithType(this, AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_2)
        ?: findAddressComponentWithType(this, AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1)
    }

    val maybeCountryComponent = findAddressComponentWithType(
        result.addressComponents, AddressComponentType.COUNTRY
    )

    return if (maybeSpecificComponent != null && maybeCountryComponent != null) {
        "${maybeSpecificComponent.longName}, ${maybeCountryComponent.shortName}"
    } else if (maybeSpecificComponent != null) {
        "${maybeSpecificComponent.longName}"
    } else if (maybeCountryComponent != null) {
        "${maybeCountryComponent.longName}"
    } else {
        throw RuntimeException("Got bad result with no names. Formatted address:\n" + result.formattedAddress)
    }
}

private fun findAddressComponentWithType(
    addressComponents: Array<AddressComponent>,
    type: AddressComponentType
): AddressComponent? {
    return addressComponents.find { it.types.contains(type) }
}

data class MapSearchRequest(
    val query: String,
    val sessionToken: String,
)

data class MapSearchResponse(
    val entries: List<MapSearchEntry>,
)

data class MapSearchEntry(
    val text: String,
    val entryId: String,
)

data class MapDetailsRequest(
    val entryId: String,
)

data class MapDetailsResponse(
    val name: String,
    val lat: Double,
    val long: Double,
)