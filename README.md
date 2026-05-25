# 🌤️ Weather Tracker App

A production-grade, offline-resilient Android application built with modern Kotlin and decoupled architecture. The application leverages real-time location metrics, geometric background mapping, automated system-level background sync routines, and robust local persistence storage models to provide an uninterrupted weather monitoring experience.

---

## 🚀 Key Engineering & Architecture Features

This application transitions away from basic API rendering to showcase enterprise software engineering principles:

* **🛠️ Decoupled Vibe Engine Architecture:** Fully isolates time-of-day math and condition parsing into a standalone, micro-tested `VibeEngine` component, ensuring the `MainActivity` acts strictly as a lightweight UI Controller.
* **📦 GSON Serialization Offline Fallback:** Implements a localized persistence queue that backs up search responses on-disk. In zero-signal or network-failure events, the app seamlessly degrades gracefully to show the last cached metrics and search history.
* **⏰ Dynamic Timezone Localization:** Decouples timezone integer shifts directly from the API response payload, rendering the precise local hour profile of any global country searched, independent of the user's local device clock.
* **🔄 WorkManager Periodic Synchronization:** Schedules a system-managed background synchronization loop running on a strict 15-minute interval that keeps the local database cache refreshed without impacting the device battery footprint.
* **🧪 Unit-Tested Logical Cores:** Includes dedicated local JUnit validation suites (`VibeEngineTest.kt`) to guarantee business logic correctness over complex condition priorities and boundary configurations.

---

## 📊 Technical System Directory Structure

```text
com.example.weathertrackerapp
├── MainActivity.kt          # UI Controller, Permission Lifecycle & View Binding
├── VibeEngine.kt            # Pure Logic: Core Time-of-Day/Condition Vibe Math Matrix
├── WeatherUtils.kt          # Static Utilities: Timezone Formatting & Vector Resource Mapping
├── WeatherRefreshWorker.kt  # WorkManager Loop: Periodic Silent Background Cache Updates
├── SearchHistoryUtils.kt    # Persistence Layer: SharedPrefs Operations & GSON Serialization Fallbacks
├── NetworkUtils.kt          # Connectivity Agent: Safe Zero-Signal Connectivity Verification
├── Network Models / Adapters # Retrofit API Handlers and Horizontal RecyclerView Frameworks