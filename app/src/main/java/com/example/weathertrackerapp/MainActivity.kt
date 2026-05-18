package com.example.weathertrackerapp

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    // Declare a secure API key token to authenticate our network requests
    private val API_KEY = "df4acdc7e3180313bd8f669507ef4b30"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI view elements
        val etCitySearch: EditText = findViewById(R.id.etCitySearch)
        val btnSearch: ImageButton = findViewById(R.id.btnSearch)
        val tvCityName: TextView = findViewById(R.id.tvCityName)
        val tvTemperature: TextView = findViewById(R.id.tvTemperature)
        val tvCondition: TextView = findViewById(R.id.tvCondition)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)

        // Set up the click action listener for the search input
        btnSearch.setOnClickListener {
            val city = etCitySearch.text.toString().trim()
            if (city.isNotEmpty()) {
                // Trigger the live internet fetch routine
                fetchWeatherData(city, tvCityName, tvTemperature, tvCondition, progressBar)
            } else {
                Toast.makeText(this, "Please enter a valid city name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Reaches out to the cloud server on a background thread to fetch real-world stats.
     */
    private fun fetchWeatherData(
        city: String,
        tvCity: TextView,
        tvTemp: TextView,
        tvCond: TextView,
        progress: ProgressBar
    ) {
        // 1. Show the loading spinner and hide text views while loading data
        progress.visibility = View.VISIBLE

        // 2. Queue up the asynchronous network call using our Retrofit engine instance
        RetrofitClient.instance.getWeatherData(city, API_KEY)
            .enqueue(object : Callback<WeatherResponse> {

                // RUNS AUTOMATICALLY IF THE SERVER RESPONDS SUCCESSFUL
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    // Turn off the loading spinner
                    progress.visibility = View.GONE

                    if (response.isSuccessful && response.body() != null) {
                        val weatherData = response.body()!!

                        // Extract metrics from our nested data model blueprints
                        val currentTemp = weatherData.mainData.temperature
                        val currentCondition = weatherData.weatherDescriptionList.firstOrNull()?.description ?: "No description"
                        val parsedCityName = weatherData.cityName

                        // Bind the raw network data directly onto our application UI screen!
                        tvCity.text = parsedCityName
                        tvTemp.text = "${currentTemp.toInt()}°C"
                        tvCond.text = currentCondition.replaceFirstChar { it.uppercase() }
                    } else {
                        // FIX: Show the exact numeric error code from the server (e.g., 401 or 404)
                        val errorCode = response.code()
                        Toast.makeText(this@MainActivity, "Server Error ($errorCode): Check city spelling or key status", Toast.LENGTH_LONG).show()
                    }
                }

                // RUNS AUTOMATICALLY IF THERE IS NO INTERNET CONNECTION
                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    progress.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Network error: Check your internet connection", Toast.LENGTH_LONG).show()
                }
            })
    }
}