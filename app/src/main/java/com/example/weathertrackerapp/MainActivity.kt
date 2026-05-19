package com.example.weathertrackerapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val apiKey = "90b07aff560023aa2b1fa6eb5695c91d"


    // Registers the system permission dialogue callback
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            Toast.makeText(this, "Permission granted. Fetching weather...", Toast.LENGTH_SHORT).show()
            fetchLocationAndWeather()
        } else {
            Toast.makeText(this, "Permission denied. Search manually.", Toast.LENGTH_LONG).show()
        }
    }

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

        // Check if permissions are already granted to avoid unnecessary popup refresh
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndWeather()
        } else {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchLocationAndWeather() {
        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Successfully got the last known location
                fetchWeatherByCoords(
                    location.latitude,
                    location.longitude,
                    "Current Location",
                    findViewById(R.id.tvCityName),
                    findViewById(R.id.tvTemperature),
                    findViewById(R.id.tvCondition),
                    findViewById(R.id.ivWeatherIcon),
                    findViewById(R.id.progressBar)
                )
            } else {
                // Last location is null, requesting a fresh update
                Log.d("WeatherApp", "Last location was null, requesting fresh update")
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { freshLocation ->
                        if (freshLocation != null) {
                            fetchWeatherByCoords(
                                freshLocation.latitude,
                                freshLocation.longitude,
                                "Current Location",
                                findViewById(R.id.tvCityName),
                                findViewById(R.id.tvTemperature),
                                findViewById(R.id.tvCondition),
                                findViewById(R.id.ivWeatherIcon),
                                findViewById(R.id.progressBar)
                            )
                        } else {
                            findViewById<TextView>(R.id.tvCityName).text = "Location unavailable"
                            Toast.makeText(this, "Could not determine location.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("WeatherApp", "Error fetching location", e)
            findViewById<TextView>(R.id.tvCityName).text = "Location Error"
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
                            Log.e("WeatherApp", "Direct search failed: ${response.code()} ${response.message()}")
                            // If direct search fails, it might be a specific city that needs geocoding resolution
                            handleErrorWithGeocoding(query, tvCity, tvTemp, tvCond, ivIcon, progress)
                        }
                    }

                    override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                        Log.e("WeatherApp", "Direct search failure", t)
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
                        Log.d("WeatherApp", "Geocoding success: ${location.name}, ${location.lat}, ${location.lon}")
                        fetchWeatherByCoords(location.lat, location.lon, location.name, tvCity, tvTemp, tvCond, ivIcon, progress)
                    } else {
                        Log.e("WeatherApp", "Geocoding failed or empty: ${response.code()} ${response.message()}")
                        progress.visibility = View.GONE
                        tvCity.text = getString(R.string.error_text)
                        Toast.makeText(this@MainActivity, getString(R.string.city_not_found), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<GeocodingResponse>>, t: Throwable) {
                    Log.e("WeatherApp", "Geocoding failure", t)
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
        progress.visibility = View.VISIBLE
        RetrofitClient.instance.getWeatherDataByCoords(lat, lon, apiKey, "metric")
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    progress.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        val weatherData = response.body()!!
                        
                        // Binding data to UI fields
                        tvCity.text = weatherData.cityName.ifEmpty { displayName }
                        tvTemp.text = getString(R.string.temp_format, weatherData.mainData.temperature.toInt())
                        
                        val rawCondition = weatherData.weatherDescriptionList.firstOrNull()?.description ?: "clear"
                        tvCond.text = rawCondition.replaceFirstChar { it.uppercase() }
                        updateWeatherIcon(rawCondition, ivIcon)
                    } else {
                        Log.e("WeatherApp", "Fetch by coords failed: ${response.code()}")
                        tvCity.text = getString(R.string.error_text)
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("WeatherApp", "Fetch by coords failure", t)
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
