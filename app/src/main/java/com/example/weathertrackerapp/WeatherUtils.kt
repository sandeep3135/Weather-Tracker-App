package com.example.weathertrackerapp

import android.widget.ImageView

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
}
