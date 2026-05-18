package com.example.weathertrackerapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    // This tells Retrofit we want to perform an HTTP GET request to the "weather" path
    @GET("weather")
    fun getWeatherData(
        // Passes the city name dynamically to the query parameter "q"
        @Query("q") cityName: String,

        // Passes our secure API authentication token key to parameter "appid"
        @Query("appid") apiKey: String,

        // Tells the server to return temperature in Celsius metric instead of Fahrenheit
        @Query("units") units: String = "metric"
    ): Call<WeatherResponse> // Returns our pre-configured data model blueprint wrapper
}