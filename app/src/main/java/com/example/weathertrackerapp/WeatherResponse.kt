package com.example.weathertrackerapp

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("main") val mainData: Main?,
    @SerializedName("weather") val weatherDescriptionList: List<Weather>?,
    @SerializedName("name") val cityName: String?,
    @SerializedName("sys") val sys: Sys?,
    @SerializedName("wind") val wind: Wind?,
    @SerializedName("coord") val coord: Coord?,
    @SerializedName("dt") val dt: Long?,
    @SerializedName("timezone") val timezone: Int?,
    @SerializedName("visibility") val visibility: Int?
)

data class Coord(
    val lat: Double,
    val lon: Double
)

data class Main(
    @SerializedName("temp") val temperature: Double?,
    @SerializedName("humidity") val humidity: Int?,
    @SerializedName("temp_min") val tempMin: Double?,
    @SerializedName("temp_max") val tempMax: Double?,
    @SerializedName("pressure") val pressure: Int?,
    @SerializedName("feels_like") val feelsLike: Double?
)

data class Weather(
    @SerializedName("main") val main: String?,
    @SerializedName("description") val description: String?
)

data class Sys(
    @SerializedName("country") val country: String?,
    @SerializedName("sunrise") val sunrise: Long?,
    @SerializedName("sunset") val sunset: Long?
)

data class Wind(
    @SerializedName("speed") val speed: Double?
)
