package com.example.weathertrackerapp

import org.junit.Assert.assertEquals
import org.junit.Test

class VibeEngineTest {

    @Test
    fun testRainyVibePriority() {
        // Rain should take priority over time of day
        val vibe = VibeEngine.calculateVibe("Rain", "light rain", 12)
        assertEquals(VibeEngine.WeatherVibe.RAINY, vibe)
    }

    @Test
    fun testMorningVibe() {
        val vibe = VibeEngine.calculateVibe("Clear", "clear sky", 8)
        assertEquals(VibeEngine.WeatherVibe.MORNING, vibe)
    }

    @Test
    fun testAfternoonVibe() {
        val vibe = VibeEngine.calculateVibe("Clouds", "few clouds", 14)
        assertEquals(VibeEngine.WeatherVibe.AFTERNOON, vibe)
    }

    @Test
    fun testEveningVibe() {
        val vibe = VibeEngine.calculateVibe("Clear", "clear sky", 19)
        assertEquals(VibeEngine.WeatherVibe.EVENING, vibe)
    }

    @Test
    fun testNightVibe() {
        val vibe = VibeEngine.calculateVibe("Clear", "clear sky", 23)
        assertEquals(VibeEngine.WeatherVibe.NIGHT, vibe)
    }

    @Test
    fun testLocalHourCalculation() {
        // UTC 12:00:00 PM = 1715688000000L (example)
        val utcTime = 1715688000000L // Ensure this matches a specific UTC hour for the test
        // Let's use a simpler way to test the logic by mocking or choosing a fixed point.
        // Or just test the offset logic:
        
        val offsetIST = 19800 // +5:30
        val hourInIST = VibeEngine.getLocalHour(offsetIST, 1715688000000L) // 12:00 UTC -> 17:30 IST
        assertEquals(17, hourInIST)
    }
}
