package com.example.weathertrackerapp

import android.widget.ImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object WeatherUtils {
    fun updateWeatherIcon(condition: String?, ivIcon: ImageView) {
        val rawCondition = condition ?: "clear"
        when {
            rawCondition.contains("rain", ignoreCase = true) -> ivIcon.setImageResource(R.drawable.ic_weather_rainy)
            rawCondition.contains("cloud", ignoreCase = true) -> ivIcon.setImageResource(R.drawable.ic_weather_cloudy)
            rawCondition.contains("haze", ignoreCase = true) || rawCondition.contains("mist", ignoreCase = true) -> ivIcon.setImageResource(R.drawable.ic_weather_haze)
            else -> ivIcon.setImageResource(R.drawable.ic_weather_sunny)
        }
    }

    // 🏆 ADD THIS NEW UTILITY METHOD RIGHT HERE:
    fun formatLocalTimeWithOffset(timezoneOffsetInSeconds: Int): String {
        val sdf = SimpleDateFormat("EEEE, h:mm a", Locale.getDefault())

        // Convert the API's raw seconds shift into milliseconds
        val offsetMillis = timezoneOffsetInSeconds * 1000L

        // Force the layout formatter to utilize a clean GMT offset tracking scheme
        val customTimeZone = TimeZone.getTimeZone("GMT").apply {
            rawOffset = offsetMillis.toInt()
        }

        sdf.timeZone = customTimeZone
        return sdf.format(Date())
    }
}