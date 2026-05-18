package com.example.weathertrackerapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // The core server address base URL for the OpenWeatherMap services
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    val instance: WeatherApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Automatically runs our Gson model converter
            .build()

        retrofit.create(WeatherApiService::class.java)
    }
}