package com.example.weathertrackerapp

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SearchHistoryUtils {
    private const val PREFS_NAME = "WeatherPrefs"
    private const val KEY_HISTORY = "search_history"
    private const val MAX_HISTORY_SIZE = 5

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 🏆 Retrieve the saved history list from local storage
    fun getSearchHistory(context: Context): List<String> {
        val historyString = getPrefs(context).getString(KEY_HISTORY, "") ?: ""
        if (historyString.isEmpty()) return emptyList()
        return historyString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    // 🏆 Add a new city to the queue, remove duplicates, and cap it at 5 items
    fun addCityToHistory(context: Context, city: String) {
        val cleanCity = city.trim().replaceFirstChar { it.uppercase() }
        if (cleanCity.isEmpty()) return

        val currentHistory = getSearchHistory(context).toMutableList()

        // Remove if it already exists to avoid duplicates, then push to the front
        currentHistory.remove(cleanCity)
        currentHistory.add(0, cleanCity)

        // Trim the tail if it exceeds our professional portfolio cap of 5
        val trimmedHistory = if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.subList(0, MAX_HISTORY_SIZE)
        } else {
            currentHistory
        }

        // Serialize back into a single clean string
        val serializedString = trimmedHistory.joinToString(",")
        getPrefs(context).edit { putString(KEY_HISTORY, serializedString) }
    }

    private const val KEY_CACHED_WEATHER = "cached_weather_response"
    private const val KEY_CACHED_CITY = "cached_weather_city"
    private const val KEY_CACHED_FORECAST = "cached_forecast_response"

    // 🏆 Save the raw weather string and displayName to disk on successful network pass
    fun saveLastCachedWeather(context: Context, rawJsonString: String, displayName: String) {
        getPrefs(context).edit {
            putString(KEY_CACHED_WEATHER, rawJsonString)
            putString(KEY_CACHED_CITY, displayName)
        }
    }

    // 🏆 Save the raw forecast string to disk
    fun saveLastCachedForecast(context: Context, rawJsonString: String) {
        getPrefs(context).edit { putString(KEY_CACHED_FORECAST, rawJsonString) }
    }

    // 🏆 Retrieve the cached city pair back when the network is completely dark
    fun getCachedWeatherData(context: Context): Pair<String, String>? {
        val prefs = getPrefs(context)
        val rawJson = prefs.getString(KEY_CACHED_WEATHER, "") ?: ""
        val displayName = prefs.getString(KEY_CACHED_CITY, "") ?: ""

        if (rawJson.isEmpty() || displayName.isEmpty()) return null
        return Pair(rawJson, displayName)
    }

    // 🏆 Retrieve the cached forecast back
    fun getCachedForecastData(context: Context): String? {
        val rawJson = getPrefs(context).getString(KEY_CACHED_FORECAST, "") ?: ""
        return rawJson.ifEmpty { null }
    }

    // 🏆 Clear all history and cache for a fresh start
    fun clearAll(context: Context) {
        getPrefs(context).edit { clear() }
    }
}
