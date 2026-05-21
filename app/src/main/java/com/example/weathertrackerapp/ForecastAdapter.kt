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

        // 1. Smart Timeline Labels
        if (position == 0) {
            holder.tvDay.text = holder.itemView.context.getString(R.string.now)
        } else {
            val date = Date(item.dt * 1000L)
            val sdf = SimpleDateFormat(holder.itemView.context.getString(R.string.hourly_format), Locale.getDefault())
            holder.tvDay.text = sdf.format(date)
        }

        // 2. Exact Temperature
        holder.tvTemp.text = "${item.main.temperature?.toInt() ?: 0}°"

        // 3. Accurate Condition-Icon Mapping per Data Block
        // Extracting the main condition category directly from the current interval packet
        val conditionDescription = item.weather.firstOrNull()?.description ?: "clear"

        when {
            conditionDescription.contains("rain", ignoreCase = true) || conditionDescription.contains("drizzle", ignoreCase = true) -> {
                holder.ivIcon.setImageResource(R.drawable.ic_weather_rainy)
            }
            conditionDescription.contains("cloud", ignoreCase = true) -> {
                holder.ivIcon.setImageResource(R.drawable.ic_weather_cloudy)
            }
            conditionDescription.contains("haze", ignoreCase = true) || conditionDescription.contains("mist", ignoreCase = true) || conditionDescription.contains("fog", ignoreCase = true) -> {
                holder.ivIcon.setImageResource(R.drawable.ic_weather_haze)
            }
            else -> {
                holder.ivIcon.setImageResource(R.drawable.ic_weather_sunny)
            }
        }
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
