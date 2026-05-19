package com.example.weathertrackerapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val apiKey = "90b07aff560023aa2b1fa6eb5695c91d"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etCitySearch: EditText = findViewById(R.id.etCitySearch)
        val btnSearch: ImageButton = findViewById(R.id.btnSearch)
        val tvCityName: TextView = findViewById(R.id.tvCityName)
        val tvTemperature: TextView = findViewById(R.id.tvTemperature)
        val tvCondition: TextView = findViewById(R.id.tvCondition)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val ivWeatherIcon: ImageView = findViewById(R.id.ivWeatherIcon)

        btnSearch.setOnClickListener {
            val query = etCitySearch.text.toString().trim()
            if (query.isNotEmpty()) {
                hideKeyboard()
                etCitySearch.text.clear()
                etCitySearch.clearFocus()

                tvCityName.text = getString(R.string.searching)
                tvTemperature.text = "--°C"
                tvCondition.text = ""
                
                fetchWeather(query, tvCityName, tvTemperature, tvCondition, ivIcon = ivWeatherIcon, progress = progressBar)
            } else {
                Toast.makeText(this, getString(R.string.enter_city), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun fetchWeather(
        query: String,
        tvCity: TextView,
        tvTemp: TextView,
        tvCond: TextView,
        ivIcon: ImageView,
        progress: ProgressBar
    ) {
        progress.visibility = View.VISIBLE

        // Use direct search first because OpenWeatherMap's /weather endpoint
        // handles names exactly as users type them (e.g., "Pakistan" returns "Pakistan").
        RetrofitClient.instance.getWeatherData(query, apiKey, "metric")
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    progress.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        val weatherData = response.body()!!
                        
                        // We use the exact name from the response. If the user searched for "Pakistan", 
                        // the API usually returns "Pakistan" in weatherData.cityName.
                        tvCity.text = getString(R.string.city_country_format, weatherData.cityName, weatherData.sys.country)
                        tvTemp.text = getString(R.string.temp_format, weatherData.mainData.temperature.toInt())
                        
                        val rawCondition = weatherData.weatherDescriptionList.firstOrNull()?.description ?: "clear"
                        tvCond.text = rawCondition.replaceFirstChar { it.uppercase() }
                        updateWeatherIcon(rawCondition, ivIcon)
                    } else {
                        // If direct search fails, it might be a specific city that needs geocoding resolution
                        handleErrorWithGeocoding(query, tvCity, tvTemp, tvCond, ivIcon, progress)
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    progress.visibility = View.GONE
                    tvCity.text = getString(R.string.no_connection)
                }
            })
    }

    private fun handleErrorWithGeocoding(
        query: String,
        tvCity: TextView,
        tvTemp: TextView,
        tvCond: TextView,
        ivIcon: ImageView,
        progress: ProgressBar
    ) {
        progress.visibility = View.VISIBLE
        RetrofitClient.instance.getGeocoding(query, 1, apiKey).enqueue(object : Callback<List<GeocodingResponse>> {
            override fun onResponse(call: Call<List<GeocodingResponse>>, response: Response<List<GeocodingResponse>>) {
                val results = response.body()
                if (response.isSuccessful && !results.isNullOrEmpty()) {
                    val location = results[0]
                    fetchWeatherByCoords(location.lat, location.lon, location.name, tvCity, tvTemp, tvCond, ivIcon, progress)
                } else {
                    progress.visibility = View.GONE
                    tvCity.text = getString(R.string.error_text)
                    Toast.makeText(this@MainActivity, getString(R.string.city_not_found), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<GeocodingResponse>>, t: Throwable) {
                progress.visibility = View.GONE
                tvCity.text = getString(R.string.no_connection)
            }
        })
    }

    private fun fetchWeatherByCoords(
        lat: Double,
        lon: Double,
        displayName: String,
        tvCity: TextView,
        tvTemp: TextView,
        tvCond: TextView,
        ivIcon: ImageView,
        progress: ProgressBar
    ) {
        RetrofitClient.instance.getWeatherDataByCoords(lat, lon, apiKey)
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    progress.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        val weatherData = response.body()!!
                        tvCity.text = getString(R.string.city_country_format, displayName, weatherData.sys.country)
                        tvTemp.text = getString(R.string.temp_format, weatherData.mainData.temperature.toInt())
                        
                        val rawCondition = weatherData.weatherDescriptionList.firstOrNull()?.description ?: "clear"
                        tvCond.text = rawCondition.replaceFirstChar { it.uppercase() }
                        updateWeatherIcon(rawCondition, ivIcon)
                    } else {
                        tvCity.text = getString(R.string.error_text)
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    progress.visibility = View.GONE
                    tvCity.text = getString(R.string.no_connection)
                }
            })
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
