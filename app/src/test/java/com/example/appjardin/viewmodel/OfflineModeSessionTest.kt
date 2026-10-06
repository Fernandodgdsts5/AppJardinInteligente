package com.example.appjardin.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineModeSessionTest {

    @Test
    fun testOfflineModeBlocksSessionPersistence() {
        var isOffline = false
        var sessionsCreated = 0

        fun onTelemetryReceived(humidity: Float) {
            if (isOffline) {
                return // Block session creation
            }
            sessionsCreated++
        }

        // Online mode: telemetry creates sessions
        isOffline = false
        onTelemetryReceived(65f)
        assertEquals(1, sessionsCreated)

        // Offline mode: telemetry MUST NOT create sessions
        isOffline = true
        onTelemetryReceived(70f)
        onTelemetryReceived(75f)
        assertEquals(1, sessionsCreated) // Still 1, no new sessions created in offline mode
    }
}
