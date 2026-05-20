package com.example.weathertrackerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class DailyForecastAdapter(private var dailyList: List<ForecastItem>) :
    RecyclerView.Adapter<DailyForecastAdapter.DailyViewHolder>() {

    class DailyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDailyDate)
        val ivIcon: ImageView = view.findViewById(R.id.ivDailyIcon)
        val tvTempSpread: TextView = view.findViewById(R.id.tvDailyTempSpread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyViewHolder {
        // 🏆 FIX: Point this to your new wide daily layout file!
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_forecast, parent, false)
        return DailyViewHolder(view)
    }

    override fun onBindViewHolder(holder: DailyViewHolder, position: Int) {
        val item = dailyList[position]
        val context = holder.itemView.context

        // 1. Format Day Name & Date (e.g., "May 21 • Tomorrow")
        val date = Date(item.dt * 1000L)
        val dateString = SimpleDateFormat(context.getString(R.string.day_month_format), Locale.getDefault()).format(date)

        holder.tvDate.text = when (position) {
            0 -> context.getString(R.string.daily_date_format, dateString, context.getString(R.string.today))
            1 -> context.getString(R.string.daily_date_format, dateString, context.getString(R.string.tomorrow))
            else -> {
                val dayName = SimpleDateFormat(context.getString(R.string.day_name_format), Locale.getDefault()).format(date)
                context.getString(R.string.daily_date_format, dateString, dayName)
            }
        }

        // 2. Set Temperature Spread
        val maxTemp = item.main.tempMax?.toInt() ?: item.main.temperature?.toInt() ?: 0
        val minTemp = item.main.tempMin?.toInt() ?: (maxTemp - 4)
        holder.tvTempSpread.text = context.getString(R.string.temp_spread_format, minTemp, maxTemp)

        // 3. Exact Condition-Icon Mapping using WeatherUtils
        val condition = item.weather.firstOrNull()?.description
        WeatherUtils.updateWeatherIcon(condition, holder.ivIcon)
    }

    override fun getItemCount(): Int = dailyList.size

    fun updateData(newList: List<ForecastItem>) {
        dailyList = newList
        notifyDataSetChanged()
    }
}
