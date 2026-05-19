package com.example.weathertrackerapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    // This tells Retrofit we want to perform an HTTP GET request to the "weather" path
    @GET("weather")
    fun getWeatherData(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Call<WeatherResponse>

    // Search by coordinates (lat/lon) which is more accurate for results from Geocoding
    @GET("weather")
    fun getWeatherDataByCoords(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Call<WeatherResponse>

    // Geocoding API to resolve names (cities/countries) to coordinates
    @GET("https://api.openweathermap.org/geo/1.0/direct")
    fun getGeocoding(
        @Query("q") query: String,
        @Query("limit") limit: Int = 1,
        @Query("appid") apiKey: String
    ): Call<List<GeocodingResponse>>
}

data class GeocodingResponse(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String? = null
)
