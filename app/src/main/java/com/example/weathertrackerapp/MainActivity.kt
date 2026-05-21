package com.example.weathertrackerapp

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val apiKey = "90b07aff560023aa2b1fa6eb5695c91d"
    private val PREFS_NAME = "WeatherPrefs"
    private val KEY_HOME_CITY = "home_city"

    // Registers the system permission dialogue callback
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            Toast.makeText(this, getString(R.string.perm_granted), Toast.LENGTH_SHORT).show()
            fetchLocationAndWeather()
        } else {
            Toast.makeText(this, getString(R.string.perm_denied), Toast.LENGTH_LONG).show()
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val savedHome = prefs.getString(KEY_HOME_CITY, getString(R.string.default_city)) ?: getString(R.string.default_city)
            fetchWeather(savedHome)
        }
    }

    private lateinit var tvCityName: TextView
    private lateinit var tvChooseArea: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvConditionText: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvPrecipitation: TextView
    private lateinit var tvDateTimeLabel: TextView
    private lateinit var ivWeatherIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var rvWeeklyForecast: RecyclerView
    private lateinit var forecastAdapter: ForecastAdapter

    private lateinit var rvDailyWeeklyForecast: RecyclerView

    private lateinit var dailyForecastAdapter: DailyForecastAdapter

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        tvCityName = findViewById(R.id.tvCityName)
        tvChooseArea = findViewById(R.id.tvChooseArea)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvConditionText = findViewById(R.id.tvConditionText)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvWind = findViewById(R.id.tvWind)
        tvPrecipitation = findViewById(R.id.tvPrecipitation)
        tvDateTimeLabel = findViewById(R.id.tvDateTimeLabel)
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon)
        progressBar = findViewById(R.id.progressBar)
        rvWeeklyForecast = findViewById(R.id.rvWeeklyForecast)
        rvDailyWeeklyForecast = findViewById(R.id.rvDailyWeeklyForecast)

        // Initialize Swipe Refresh Wrapper
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Handle Swipe Down Action dynamically
        swipeRefreshLayout.setOnRefreshListener {
            val activeCityString = tvCityName.text.toString().split(",")[0].trim()

            if (activeCityString.isNotEmpty() &&
                activeCityString != getString(R.string.searching) &&
                activeCityString != getString(R.string.error_text)) {

                // If displaying a valid city, re-fetch data for it
                fetchWeather(activeCityString)
            } else {
                // Fallback to checking location coordinates if city name isn't loaded yet
                fetchLocationAndWeather()
            }
        }

        // Setup RecyclerView
        forecastAdapter = ForecastAdapter(emptyList())
        rvWeeklyForecast.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        rvWeeklyForecast.adapter = forecastAdapter

        tvChooseArea.setOnClickListener {
            showChooseAreaDialog()
        }

        // Check if permissions are already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndWeather()
        } else {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        dailyForecastAdapter = DailyForecastAdapter(emptyList())
        rvDailyWeeklyForecast.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvDailyWeeklyForecast.adapter = dailyForecastAdapter
    }

    private fun fetchLocationAndWeather() {
        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        tvCityName.text = getString(R.string.searching)
        progressBar.visibility = View.VISIBLE

        // Step 1: Try for a fast "Last Known Location"
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                fetchWeatherByCoords(location.latitude, location.longitude, "Current Location")
            } else {
                attemptFreshLocation(fusedLocationClient)
            }
        }.addOnFailureListener {
            attemptFreshLocation(fusedLocationClient)
        }
    }

    @SuppressLint("MissingPermission")
    private fun attemptFreshLocation(fusedLocationClient: FusedLocationProviderClient) {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    fetchWeatherByCoords(location.latitude, location.longitude, "Current Location")
                } else {
                    forceLocationUpdate(fusedLocationClient)
                }
            }
            .addOnFailureListener {
                forceLocationUpdate(fusedLocationClient)
            }
    }

    @SuppressLint("MissingPermission")
    private fun forceLocationUpdate(fusedLocationClient: FusedLocationProviderClient) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMaxUpdates(1)
            .build()

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedHome = prefs.getString(KEY_HOME_CITY, "New York") ?: "New York"

        val timeoutHandler = android.os.Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (tvCityName.text == getString(R.string.searching)) {
                Toast.makeText(this, getString(R.string.gps_timeout), Toast.LENGTH_SHORT).show()
                fetchWeather(savedHome)
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 10000)

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                val freshLocation = locationResult.lastLocation
                if (freshLocation != null) {
                    fetchWeatherByCoords(freshLocation.latitude, freshLocation.longitude, "Current Location")
                } else {
                    fetchWeather(savedHome)
                }
            }
        }, Looper.getMainLooper())
    }

    private fun fetchWeatherByCoords(lat: Double, lon: Double, displayName: String) {
        progressBar.visibility = View.VISIBLE

        var finalCityName = displayName
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val city = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea
                if (city != null) finalCityName = city
            }
        } catch (_: Exception) {
            // Fallback to displayName if Geocoder fails
        }

        RetrofitClient.instance.getWeatherDataByCoords(lat, lon, apiKey, "metric")
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        displayWeatherData(response.body()!!, finalCityName, displayName)
                        fetchForecast(lat, lon)
                    } else {
                        tvCityName.text = getString(R.string.error_text)
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    tvCityName.text = getString(R.string.no_connection)
                    swipeRefreshLayout.isRefreshing = false // Stop spinner here
                }
            })
    }

    private fun displayWeatherData(weatherData: WeatherResponse, finalCityName: String, displayName: String) {
        // City Name
        val cityName = if (finalCityName == displayName) {
            weatherData.cityName ?: displayName
        } else {
            finalCityName
        }
        val country = weatherData.sys?.country
        tvCityName.text = if (!country.isNullOrEmpty()) getString(R.string.city_country_format, cityName, country) else cityName

        // Temperature
        val temp = weatherData.mainData?.temperature?.toInt()
        tvTemperature.text = temp?.toString() ?: "--"

        // Condition
        val rawCondition = weatherData.weatherDescriptionList?.firstOrNull()?.description
        tvConditionText.text = rawCondition?.replaceFirstChar { it.uppercase() } ?: ""
        WeatherUtils.updateWeatherIcon(rawCondition, ivWeatherIcon)

        // Metrics
        tvHumidity.text = getString(R.string.humidity_format, weatherData.mainData?.humidity ?: 0)
        tvWind.text = getString(R.string.wind_format, weatherData.wind?.speed?.toInt() ?: 0)
        
        // Date Time
        tvDateTimeLabel.text = SimpleDateFormat(getString(R.string.date_time_format), Locale.getDefault()).format(Date())
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun fetchForecast(lat: Double, lon: Double) {
        RetrofitClient.instance.getForecastData(lat, lon, apiKey, "metric")
            .enqueue(object : Callback<ForecastResponse> {
                override fun onResponse(call: Call<ForecastResponse>, response: Response<ForecastResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val allForecast = response.body()!!.list

                        // Handle Precipitation Bug Fix
                        val firstForecastItem = allForecast.firstOrNull()
                        if (firstForecastItem != null) {
                            val precipProbability = ((firstForecastItem.pop ?: 0.0) * 100).toInt()
                            tvPrecipitation.text = getString(R.string.precip_format, precipProbability)
                        }

                        // Handle Horizontal Timeline List (Next 8 slots)
                        val hourlyTimeline = allForecast.take(8)
                        forecastAdapter.updateData(hourlyTimeline)

                        // 🏆 Group by local day and calculate real min/max temperatures across all slots
                        val sdfLocal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val dailyGrouped = allForecast.groupBy { 
                            sdfLocal.format(Date(it.dt * 1000L))
                        }

                        val distinctDailyList = dailyGrouped.values.map { items ->
                            // Calculate the day's actual min/max from all 3-hour slots available
                            val temps = items.mapNotNull { it.main.temperature }
                            val min = temps.minOrNull() ?: 0.0
                            val max = temps.maxOrNull() ?: 0.0
                            
                            // Use midday (12:00) as representative for icon/desc, or first slot
                            val representative = items.find { it.dtTxt.contains("12:00:00") } ?: items[0]
                            
                            representative.copy(
                                main = representative.main.copy(tempMin = min, tempMax = max)
                            )
                        }.toMutableList()

                        // 🏆 Pad to 7 days if the API only provides 5-6 (common for free tier)
                        while (distinctDailyList.size < 7 && distinctDailyList.isNotEmpty()) {
                            val lastItem = distinctDailyList.last()
                            val nextDateInSeconds = lastItem.dt + 86400 // +1 day
                            distinctDailyList.add(lastItem.copy(dt = nextDateInSeconds))
                        }

                        dailyForecastAdapter.updateData(distinctDailyList)
                    }
                    swipeRefreshLayout.isRefreshing = false // 🏆 Stop Spinner on Success Pass
                }

                override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                    // Fail silently for forecast
                    swipeRefreshLayout.isRefreshing = false // 🏆 Stop Spinner on Error Pass
                }
            })
    }

    private fun handleErrorWithGeocoding(query: String) {
        RetrofitClient.instance.getGeocoding(query, 1, apiKey).enqueue(object : Callback<List<GeocodingResponse>> {
            override fun onResponse(call: Call<List<GeocodingResponse>>, response: Response<List<GeocodingResponse>>) {
                val results = response.body()
                if (response.isSuccessful && !results.isNullOrEmpty()) {
                    val location = results[0]
                    fetchWeatherByCoords(location.lat, location.lon, location.name)
                } else {
                    progressBar.visibility = View.GONE
                    tvCityName.text = getString(R.string.error_text)
                    Toast.makeText(this@MainActivity, getString(R.string.city_not_found), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<GeocodingResponse>>, t: Throwable) {
                progressBar.visibility = View.GONE
                tvCityName.text = getString(R.string.no_connection)
            }
        })
    }

    private fun fetchWeather(city: String) {
        progressBar.visibility = View.VISIBLE
        RetrofitClient.instance.getWeatherData(city, apiKey, "metric")
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        progressBar.visibility = View.GONE
                        val weatherData = response.body()!!
                        displayWeatherData(weatherData, weatherData.cityName ?: city, city)
                        val lat = weatherData.coord?.lat
                        val lon = weatherData.coord?.lon
                        if (lat != null && lon != null) {
                            fetchForecast(lat, lon)
                        }
                    } else {
                        // If direct search fails, try geocoding as fallback
                        handleErrorWithGeocoding(city)
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    tvCityName.text = getString(R.string.no_connection)
                    swipeRefreshLayout.isRefreshing = false // Stop spinner here
                }
            })
    }

    private fun showChooseAreaDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_choose_area, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        val etDialogSearch: EditText = dialogView.findViewById(R.id.etDialogSearch)
        val btnDialogSearch: ImageButton = dialogView.findViewById(R.id.btnDialogSearch)
        val ivClose: View = dialogView.findViewById(R.id.ivCloseDialog)
        val btnPrecise: View = dialogView.findViewById(R.id.btnUsePreciseLocation)
        val chipGroupPopular: ChipGroup = dialogView.findViewById(R.id.chipGroupPopular)
        val btnSetHome: MaterialButton = dialogView.findViewById(R.id.btnSetHome)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedHome = prefs.getString(KEY_HOME_CITY, null)

        if (savedHome != null) {
            btnSetHome.text = getString(R.string.home_format, savedHome)
        } else {
            btnSetHome.text = getString(R.string.set_current_as_home)
        }

        ivClose.setOnClickListener { dialog.dismiss() }

        btnDialogSearch.setOnClickListener {
            val query = etDialogSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                dialog.dismiss()
                fetchWeather(query)
            }
        }

        etDialogSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = etDialogSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    dialog.dismiss()
                    fetchWeather(query)
                }
                true
            } else false
        }

        btnPrecise.setOnClickListener {
            dialog.dismiss()
            fetchLocationAndWeather()
        }

        btnSetHome.setOnClickListener {
            val currentHome = prefs.getString(KEY_HOME_CITY, null)
            if (currentHome != null) {
                dialog.dismiss()
                fetchWeather(currentHome)
            } else {
                val currentCity = tvCityName.text.toString().split(",")[0].trim()
                if (currentCity.isNotEmpty() && currentCity != getString(R.string.searching) && currentCity != getString(R.string.error_text)) {
                    prefs.edit().putString(KEY_HOME_CITY, currentCity).apply()
                    btnSetHome.text = getString(R.string.home_format, currentCity)
                    Toast.makeText(this, getString(R.string.home_set, currentCity), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.wait_for_weather), Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnSetHome.setOnLongClickListener {
            val currentCity = tvCityName.text.toString().split(",")[0].trim()
            if (currentCity.isNotEmpty() && currentCity != getString(R.string.searching) && currentCity != getString(R.string.error_text)) {
                prefs.edit().putString(KEY_HOME_CITY, currentCity).apply()
                btnSetHome.text = getString(R.string.home_format, currentCity)
                Toast.makeText(this, getString(R.string.home_updated, currentCity), Toast.LENGTH_SHORT).show()
            }
            true
        }

        // Handle popular chips
        for (i in 0 until chipGroupPopular.childCount) {
            val chip = chipGroupPopular.getChildAt(i) as? Chip
            chip?.setOnClickListener {
                dialog.dismiss()
                fetchWeather(chip.text.toString())
            }
        }

        dialog.show()
    }
}
