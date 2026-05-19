package com.example.weathertrackerapp

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("main") val mainData: Main,
    @SerializedName("weather") val weatherDescriptionList: List<Weather>,
    @SerializedName("name") val cityName: String,
    @SerializedName("sys") val sys: Sys
)

data class Main(
    @SerializedName("temp") val temperature: Double
)

data class Weather(
    @SerializedName("description") val description: String
)

data class Sys(
    @SerializedName("country") val country: String
)
