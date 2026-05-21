plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.weathertrackerapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.weathertrackerapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}