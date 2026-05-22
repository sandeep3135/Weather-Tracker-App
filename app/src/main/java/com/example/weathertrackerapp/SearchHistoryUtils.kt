package com.example.weathertrackerapp

import android.content.Context
import android.content.SharedPreferences

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
        getPrefs(context).edit().putString(KEY_HISTORY, serializedString).apply()
    }
}