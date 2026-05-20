package com.example.weathertrackerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ForecastAdapter(private var forecastList: List<ForecastItem>) :
    RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    class ForecastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tvForecastDay)
        val ivIcon: ImageView = view.findViewById(R.id.ivForecastIcon)
        val tvTemp: TextView = view.findViewById(R.id.tvForecastTemp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forecast, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val item = forecastList[position]
        
        // Format day (e.g., "Wed")
        val date = Date(item.dt * 1000L)
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        holder.tvDay.text = sdf.format(date)
        
        // Temp
        holder.tvTemp.text = "${item.main.temperature?.toInt() ?: 0}°"
        
        // Icon
        val condition = item.weather.firstOrNull()?.description ?: "clear"
        updateWeatherIcon(condition, holder.ivIcon)
    }

    override fun getItemCount() = forecastList.size

    fun updateData(newList: List<ForecastItem>) {
        forecastList = newList
        notifyDataSetChanged()
    }

    private fun updateWeatherIcon(condition: String, ivIcon: ImageView) {
        when {
            condition.contains("rain", ignoreCase = true) -> ivIcon.setImageResource(R.drawable.ic_weather_rainy)
            condition.contains("cloud", ignoreCase = true) -> ivIcon.setImageResource(R.drawable.ic_weather_cloudy)
            condition.contains("haze", ignoreCase = true) || condition.contains("mist", ignoreCase = true) -> ivIcon.setImageResource(R.drawable.ic_weather_haze)
            else -> ivIcon.setImageResource(R.drawable.ic_weather_sunny)
        }
    }
}
