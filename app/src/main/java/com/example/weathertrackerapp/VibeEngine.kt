package com.example.weathertrackerapp

import java.util.*

object VibeEngine {

    enum class WeatherVibe {
        RAINY, MORNING, AFTERNOON, EVENING, NIGHT
    }

    /**
     * Determines the UI "Vibe" based on weather conditions and local time.
     */
    fun calculateVibe(mainCondition: String?, description: String?, hour: Int): WeatherVibe {
        val main = mainCondition?.lowercase(Locale.getDefault()) ?: ""
        val desc = description?.lowercase(Locale.getDefault()) ?: ""

        return when {
            // Priority 1: Severe Weather (Rain/Storm)
            main.contains("thunderstorm") || main.contains("rain") ||
                    main.contains("drizzle") || desc.contains("rain") -> {
                WeatherVibe.RAINY
            }

            // Priority 2: Time of Day Vibe
            hour in 5..11 -> WeatherVibe.MORNING
            hour in 12..16 -> WeatherVibe.AFTERNOON
            hour in 17..20 -> WeatherVibe.EVENING
            else -> WeatherVibe.NIGHT
        }
    }

    /**
     * Calculates the local hour for a given timezone offset from UTC.
     * @param timezoneOffsetSeconds Offset in seconds from UTC.
     * @param currentTimeMillis Current time in milliseconds (UTC).
     */
    fun getLocalHour(timezoneOffsetSeconds: Int, currentTimeMillis: Long): Int {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = currentTimeMillis
        calendar.add(Calendar.SECOND, timezoneOffsetSeconds)
        return calendar.get(Calendar.HOUR_OF_DAY)
    }
}
