package com.example.weathertrackerapp

import com.google.gson.annotations.SerializedName

/**
 * The master data model blueprint that maps the incoming server JSON response.
 */

data class WeatherResponse(
    // Maps to the "main" nested object block
    @SerializedName("main") val mainData: Main,
    @SerializedName("weather") val weatherDescriptionList: List<Weather>,
    @SerializedName("name") val cityName: String
    )

    // Maps to the "weather" array block list
    data class Main(
        @SerializedName("temp") val temperature: Double
    )

    // Maps directly to the city name string text
    data class Weather(
        @SerializedName("description") val description: String
    )
