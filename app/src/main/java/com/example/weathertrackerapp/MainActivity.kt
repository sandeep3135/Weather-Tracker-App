package com.example.weathertrackerapp

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
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
    private val prefsName = "WeatherPrefs"
    private val keyHomeCity = "home_city"

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
            val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
            val savedHome = prefs.getString(keyHomeCity, getString(R.string.default_city)) ?: getString(R.string.default_city)
            fetchWeather(savedHome)
        }
    }

    private lateinit var tvCityName: TextView
    private lateinit var btnChooseAreaIcon: ImageView
    private lateinit var tvTemperature: TextView
    private lateinit var tvConditionText: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvPrecipitation: TextView
    private lateinit var tvVisibility: TextView
    private lateinit var tvPressure: TextView
    private lateinit var tvFeelsLike: TextView
    private lateinit var tvDateTimeLabel: TextView
    private lateinit var ivWeatherIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var rvWeeklyForecast: RecyclerView
    private lateinit var forecastAdapter: ForecastAdapter

    private lateinit var llDailyForecastContainer: LinearLayout

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        tvCityName = findViewById(R.id.tvCityName)
        btnChooseAreaIcon = findViewById(R.id.btnChooseArea)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvConditionText = findViewById(R.id.tvConditionText)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvWind = findViewById(R.id.tvWind)
        tvPrecipitation = findViewById(R.id.tvPrecipitation)
        tvVisibility = findViewById(R.id.tvVisibility)
        tvPressure = findViewById(R.id.tvPressure)
        tvFeelsLike = findViewById(R.id.tvFeelsLike)
        tvDateTimeLabel = findViewById(R.id.tvDateTimeLabel)
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon)
        progressBar = findViewById(R.id.progressBar)
        rvWeeklyForecast = findViewById(R.id.rvWeeklyForecast)
        llDailyForecastContainer = findViewById(R.id.llDailyForecastContainer)

        // Initialize Swipe Refresh Wrapper
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // 🏆 ADD THIS LINE RIGHT HERE: Set explicit default drawable on boot
        swipeRefreshLayout.background = ContextCompat.getDrawable(this, R.drawable.bg_weather_default)

        // Handle Swipe Down Action dynamically
        swipeRefreshLayout.setOnRefreshListener {
            // Always try to refresh the location first to see if the user has moved
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndWeather()
            } else {
                val activeCityString = tvCityName.text.toString().split(",")[0].trim()
                if (activeCityString.isNotEmpty() &&
                    activeCityString != getString(R.string.searching) &&
                    activeCityString != getString(R.string.error_text)) {
                    fetchWeather(activeCityString)
                } else {
                    val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
                    val savedHome = prefs.getString(keyHomeCity, getString(R.string.default_city)) ?: getString(R.string.default_city)
                    fetchWeather(savedHome)
                }
            }
        }

        // Setup RecyclerView
        forecastAdapter = ForecastAdapter(emptyList())
        rvWeeklyForecast.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        rvWeeklyForecast.adapter = forecastAdapter

        tvCityName.setOnClickListener {
            showChooseAreaDialog()
        }

        btnChooseAreaIcon.setOnClickListener {
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

        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val savedHome = prefs.getString(keyHomeCity, "New York") ?: "New York"

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

        // 🏆 NEW: Consolidated Live UI Engine
        updateRootBackground(weatherData)

        // Metrics
        tvHumidity.text = getString(R.string.humidity_format, weatherData.mainData?.humidity ?: 0)
        tvWind.text = getString(R.string.wind_format, weatherData.wind?.speed?.toInt() ?: 0)
        tvVisibility.text = getString(R.string.visibility_format, (weatherData.visibility ?: 0) / 1000)
        tvPressure.text = getString(R.string.pressure_format, weatherData.mainData?.pressure ?: 0)
        tvFeelsLike.text = getString(R.string.feels_like_format, weatherData.mainData?.feelsLike?.toInt() ?: 0)
        
        // Date Time
        tvDateTimeLabel.text = SimpleDateFormat(getString(R.string.date_time_format), Locale.getDefault()).format(Date())
    }

    private fun updateRootBackground(weatherData: WeatherResponse) {
        val weather = weatherData.weatherDescriptionList?.firstOrNull()
        val mainCondition = weather?.main?.lowercase(Locale.getDefault()) ?: ""
        val description = weather?.description?.lowercase(Locale.getDefault()) ?: ""
        
        val dt = weatherData.dt ?: (System.currentTimeMillis() / 1000)
        val sunrise = weatherData.sys?.sunrise ?: 0L
        val sunset = weatherData.sys?.sunset ?: 0L
        
        // Determine if it's currently night at the location
        val isNight = if (sunrise != 0L && sunset != 0L) {
            dt !in sunrise..sunset
        } else {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            hour !in 6..18
        }

        val backgroundDrawableId = when {
            mainCondition.contains("thunderstorm") || mainCondition.contains("rain") || 
            mainCondition.contains("drizzle") || description.contains("rain") -> {
                R.drawable.bg_weather_rainy
            }
            mainCondition.contains("clear") || description.contains("clear") -> {
                if (isNight) R.drawable.bg_weather_default else R.drawable.bg_weather_sunny
            }
            mainCondition.contains("cloud") || mainCondition.contains("haze") || 
            mainCondition.contains("mist") || mainCondition.contains("fog") || 
            description.contains("cloud") || description.contains("haze") -> {
                R.drawable.bg_weather_cloudy
            }
            else -> {
                if (isNight) R.drawable.bg_weather_default else R.drawable.bg_weather_sunny
            }
        }
        swipeRefreshLayout.setBackgroundResource(backgroundDrawableId)
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

                        // 1. Extract distinct days and find real daily min/max
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val dailyItems = mutableListOf<ForecastItem>()
                        val dayGroups = allForecast.groupBy { sdf.format(Date(it.dt * 1000L)) }

                        // Sort keys to ensure chronological order
                        val sortedDays = dayGroups.keys.sorted()
                        
                        for (dayKey in sortedDays) {
                            val itemsForDay = dayGroups[dayKey] ?: continue
                            val min = itemsForDay.minOf { it.main.tempMin ?: it.main.temperature ?: 0.0 }
                            val max = itemsForDay.maxOf { it.main.tempMax ?: it.main.temperature ?: 0.0 }
                            
                            // Choose midday (12:00) as the representative icon/description
                            val representative = itemsForDay.find { it.dtTxt.contains("12:00:00") } ?: itemsForDay[itemsForDay.size / 2]
                            
                            dailyItems.add(
                                representative.copy(
                                    main = representative.main.copy(tempMin = min, tempMax = max)
                                )
                            )
                        }

                        // 2. FORCE exactly 7 items by padding if the API only returns 5-6 days
                        while (dailyItems.size < 7) {
                            val last = dailyItems.lastOrNull() ?: firstForecastItem!!
                            dailyItems.add(last.copy(dt = last.dt + 86400, dtTxt = "padding"))
                        }

                        // 3. Update UI
                        updateDailyForecastUI(dailyItems.take(7))
                    }
                    swipeRefreshLayout.isRefreshing = false // 🏆 Stop Spinner on Success Pass
                }

                override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                    // Fail silently for forecast
                    swipeRefreshLayout.isRefreshing = false // 🏆 Stop Spinner on Error Pass
                }
            })
    }

    private fun updateDailyForecastUI(items: List<ForecastItem>) {
        llDailyForecastContainer.removeAllViews()
        val inflater = android.view.LayoutInflater.from(this)

        items.forEachIndexed { index, item ->
            val view = inflater.inflate(R.layout.item_daily_forecast, llDailyForecastContainer, false)
            
            val tvDate: TextView = view.findViewById(R.id.tvDailyDate)
            val ivIcon: ImageView = view.findViewById(R.id.ivDailyIcon)
            val tvTempSpread: TextView = view.findViewById(R.id.tvDailyTempSpread)

            val date = Date(item.dt * 1000L)
            val dateString = SimpleDateFormat(getString(R.string.day_month_format), Locale.getDefault()).format(date)

            tvDate.text = when (index) {
                0 -> getString(R.string.daily_date_format, dateString, getString(R.string.today))
                1 -> getString(R.string.daily_date_format, dateString, getString(R.string.tomorrow))
                else -> {
                    val dayName = SimpleDateFormat(getString(R.string.day_name_format), Locale.getDefault()).format(date)
                    getString(R.string.daily_date_format, dateString, dayName)
                }
            }

            val maxTemp = item.main.tempMax?.toInt() ?: item.main.temperature?.toInt() ?: 0
            val minTemp = item.main.tempMin?.toInt() ?: (maxTemp - 4)
            tvTempSpread.text = getString(R.string.temp_spread_format, minTemp, maxTemp)

            val condition = item.weather.firstOrNull()?.description
            WeatherUtils.updateWeatherIcon(condition, ivIcon)

            llDailyForecastContainer.addView(view)
            
            // Add a thin divider line between items, except the last one
            if (index < items.size - 1) {
                val divider = View(this)
                divider.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                ).apply {
                    setMargins(0, 0, 0, 0)
                }
                divider.setBackgroundColor(android.graphics.Color.parseColor("#15FFFFFF"))
                llDailyForecastContainer.addView(divider)
            }
        }
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

        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val savedHome = prefs.getString(keyHomeCity, null)

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
            val currentHome = prefs.getString(keyHomeCity, null)
            if (currentHome != null) {
                dialog.dismiss()
                fetchWeather(currentHome)
            } else {
                val currentCity = tvCityName.text.toString().split(",")[0].trim()
                if (currentCity.isNotEmpty() && currentCity != getString(R.string.searching) && currentCity != getString(R.string.error_text)) {
                    prefs.edit().putString(keyHomeCity, currentCity).apply()
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
                prefs.edit().putString(keyHomeCity, currentCity).apply()
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
