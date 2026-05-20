package com.example.weathertrackerapp

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("main") val mainData: Main?,
    @SerializedName("weather") val weatherDescriptionList: List<Weather>?,
    @SerializedName("name") val cityName: String?,
    @SerializedName("sys") val sys: Sys?,
    @SerializedName("wind") val wind: Wind?,
    @SerializedName("coord") val coord: Coord?
)

data class Coord(
    val lat: Double,
    val lon: Double
)

data class Main(
    @SerializedName("temp") val temperature: Double?,
    @SerializedName("humidity") val humidity: Int?
)

data class Weather(
    @SerializedName("description") val description: String?
)

data class Sys(
    @SerializedName("country") val country: String?
)

data class Wind(
    @SerializedName("speed") val speed: Double?
)
