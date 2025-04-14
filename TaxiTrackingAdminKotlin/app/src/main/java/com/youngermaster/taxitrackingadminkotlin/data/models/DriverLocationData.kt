package com.youngermaster.taxitrackingadminkotlin.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DriverLocationData(
    @Json(name = "driverLocation") val driverLocation: Location,
    @Json(name = "timestamp") val timestamp: String,
    @Json(name = "route_interestPoints") val routeInterestPoints: List<String>,
    @Json(name = "admService_name") val admServiceName: String,
    @Json(name = "vehicle_number_id") val vehicleNumberId: String,
    @Json(name = "vehicle_iconMetro") val vehicleIconMetro: Boolean,
    @Json(name = "vehicle_color") val vehicleColor: Boolean,
    @Json(name = "driver_id") val driverId: String,
    @Json(name = "driver_name") val driverName: String,
    @Json(name = "route_id") val routeId: String,
    @Json(name = "route_name") val routeName: String,
    @Json(name = "route_directions") val routeDirections: List<String>,
    @Json(name = "route_isProcessed") val routeIsProcessed: Boolean,
    @Json(name = "route_isFinished") val routeIsFinished: Boolean
)

@JsonClass(generateAdapter = true)
data class Location(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double
) 