package com.example.weathertrackerapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson

class WeatherRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 1. If device is completely dark offline, stop immediately and retry later
        if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
            return Result.retry()
        }

        // 2. Extract the user's saved home city from preferences
        val prefs = applicationContext.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
        val homeCity = prefs.getString("home_city", "Raipur") ?: "Raipur"
        val apiKey = BuildConfig.WEATHER_API_KEY

        return try {
            // 3. Make a direct, synchronous network pass to fetch fresh data
            val response = RetrofitClient.instance.getWeatherData(homeCity, apiKey, "metric").execute()

            if (response.isSuccessful && response.body() != null) {
                val weatherData = response.body()!!

                // 4. Update the local offline disk cache silently in the background
                val rawJsonString = Gson().toJson(weatherData)
                SearchHistoryUtils.saveLastCachedWeather(applicationContext, rawJsonString, homeCity)

                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry() // Reschedules safely if a network timeout occurs
        }
    }
}