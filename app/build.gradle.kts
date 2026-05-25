import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.weathertrackerapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.weathertrackerapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties()
        val propertiesFile = rootProject.file("local.properties")
        if (propertiesFile.exists()) {
            properties.load(propertiesFile.inputStream())
        }
        val apiKey = properties.getProperty("OPENWEATHER_API_KEY") ?: ""

        // Inject it as a dynamic string value into your generated BuildConfig file
        buildConfigField("String", "WEATHER_API_KEY", "\"$apiKey\"")
    }

    // Ensure BuildConfig generation is enabled
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Retrofit core library for making HTTP internet requests
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson converter to automatically parse incoming JSON data streams
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Google Play Services Location for GPS tracking
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // WorkManager for background tasks
    val workVersion = "2.10.0"
    implementation("androidx.work:work-runtime-ktx:$workVersion")
}