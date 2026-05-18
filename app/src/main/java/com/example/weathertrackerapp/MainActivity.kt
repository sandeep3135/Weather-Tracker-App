package com.example.weathertrackerapp

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This links your Kotlin code directly to your activity_main.xml layout asset
        setContentView(R.layout.activity_main)

        // Initialize your clean weather layout views
        val etCitySearch: EditText = findViewById(R.id.etCitySearch)
        val btnSearch: ImageButton = findViewById(R.id.btnSearch)
        val tvCityName: TextView = findViewById(R.id.tvCityName)
        val tvTemperature: TextView = findViewById(R.id.tvTemperature)
        val tvCondition: TextView = findViewById(R.id.tvCondition)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)

        // Set up a click listener for the search action button
        btnSearch.setOnClickListener {
            val city = etCitySearch.text.toString().trim()
            if (city.isNotEmpty()) {
                // We will trigger our live API network call right here in our next milestone!
                tvCityName.text = city
            }
        }
    }
}