package com.example.weathertrackerapp

import android.os.Bundle
import android.util.Log
import android.view.View
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

    // IMPORTANT: OpenWeatherMap keys are 32 characters. Ensure yours is active.
    private val apiKey = "90b07aff560023aa2b1fa6eb5695c91d"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        Log.d("WeatherApp", "MainActivity started. Using API Key: $apiKey")

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
                // Clear old data while searching
                tvCityName.text = getString(R.string.searching)
                tvTemperature.text = "--°C"
                tvCondition.text = ""
                
                Log.d("WeatherApp", "Searching for: '$query'")
                searchAndFetchWeather(query, tvCityName, tvTemperature, tvCondition, ivWeatherIcon, progressBar)
            } else {
                Toast.makeText(this, getString(R.string.enter_city), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchAndFetchWeather(
        query: String,
        tvCity: TextView,
        tvTemp: TextView,
        tvCond: TextView,
        ivIcon: ImageView,
        progress: ProgressBar
    ) {
        progress.visibility = View.VISIBLE

        // Step 1: Use Geocoding API to resolve the search query (city or country) to coordinates.
        // This fixes the issue where searching for "India" returned "Innichen" by resolving
        // the name to a specific location (lat/lon) first.
        RetrofitClient.instance.getGeocoding(query, 1, apiKey).enqueue(object : Callback<List<GeocodingResponse>> {
            override fun onResponse(call: Call<List<GeocodingResponse>>, response: Response<List<GeocodingResponse>>) {
                val geocodingResults = response.body()
                if (response.isSuccessful && !geocodingResults.isNullOrEmpty()) {
                    val location = geocodingResults[0]
                    Log.d("WeatherApp", "Geocoding success: Found ${location.name}, ${location.country}")
                    
                    // Step 2: Fetch weather using the precise coordinates
                    fetchWeatherByCoords(location.lat, location.lon, location.name, tvCity, tvTemp, tvCond, ivIcon, progress)
                } else {
                    Log.e("WeatherApp", "Geocoding failed or no results. Falling back to direct search.")
                    // Fallback to direct search if geocoding yields nothing
                    fetchWeatherDataDirectly(query, tvCity, tvTemp, tvCond, ivIcon, progress)
                }
            }

            override fun onFailure(call: Call<List<GeocodingResponse>>, t: Throwable) {
                progress.visibility = View.GONE
                Log.e("WeatherApp", "Geocoding Network Failure: ${t.message}")
                tvCity.text = getString(R.string.no_connection)
                Toast.makeText(this@MainActivity, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
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
        RetrofitClient.instance.getWeatherDataByCoords(lat, lon, apiKey, "metric")
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    progress.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body() != null) {
                        val weatherData = response.body()!!
                        // Use the display name from geocoding (e.g., "India") instead of the API's default city name
                        tvCity.text = displayName
                        tvTemp.text = getString(R.string.temp_format, weatherData.mainData.temperature.toInt())
                        
                        val rawCondition = weatherData.weatherDescriptionList.firstOrNull()?.description ?: "clear"
                        tvCond.text = rawCondition.replaceFirstChar { it.uppercase() }

                        updateWeatherIcon(rawCondition, ivIcon)
                    } else {
                        handleErrorResponse(response, tvCity)
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    progress.visibility = View.GONE
                    tvCity.text = getString(R.string.no_connection)
                    Toast.makeText(this@MainActivity, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchWeatherDataDirectly(
        city: String,
        tvCity: TextView,
        tvTemp: TextView,
        tvCond: TextView,
        ivIcon: ImageView,
        progress: ProgressBar
    ) {
        RetrofitClient.instance.getWeatherData(city, apiKey, "metric")
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    progress.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        val weatherData = response.body()!!
                        tvCity.text = weatherData.cityName
                        tvTemp.text = getString(R.string.temp_format, weatherData.mainData.temperature.toInt())
                        val rawCondition = weatherData.weatherDescriptionList.firstOrNull()?.description ?: "clear"
                        tvCond.text = rawCondition.replaceFirstChar { it.uppercase() }
                        updateWeatherIcon(rawCondition, ivIcon)
                    } else {
                        handleErrorResponse(response, tvCity)
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    progress.visibility = View.GONE
                    tvCity.text = getString(R.string.no_connection)
                    Toast.makeText(this@MainActivity, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
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

    private fun handleErrorResponse(response: Response<WeatherResponse>, tvCity: TextView) {
        val errorBody = response.errorBody()?.string()
        val serverMessage = try {
            errorBody?.let { JSONObject(it).getString("message") } ?: "Unknown error"
        } catch (e: Exception) {
            "Request failed"
        }
        
        Log.e("WeatherApp", "Server Error: $serverMessage (Code: ${response.code()})")
        
        val displayMessage = when (response.code()) {
            401 -> getString(R.string.invalid_api_key)
            404 -> getString(R.string.city_not_found)
            else -> serverMessage.replaceFirstChar { it.uppercase() }
        }
        
        tvCity.text = getString(R.string.error_text)
        Toast.makeText(this@MainActivity, displayMessage, Toast.LENGTH_LONG).show()
    }
}
