package com.example.appjardin.viewmodel

import android.bluetooth.BluetoothProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionLostAlertManager {
    var wasConnected = false
    var showAlert = false

    fun onConnectionStateChanged(newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            wasConnected = true
            showAlert = false // Reconnection cancels alert
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            if (wasConnected) {
                showAlert = true
                wasConnected = false
            }
        }
    }

    fun onUserDismissedAlert() {
        showAlert = false
        // wasConnected remains false, so subsequent DISCONNECTED events don't re-trigger alert
    }
}

class ConnectionLostAlertTest {

    @Test
    fun testNeverConnectedDoesNotTriggerAlert() {
        val manager = ConnectionLostAlertManager()
        manager.onConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTED)
        assertFalse(manager.showAlert)
    }

    @Test
    fun testDisconnectTriggersAlert() {
        val manager = ConnectionLostAlertManager()
        manager.onConnectionStateChanged(BluetoothProfile.STATE_CONNECTED)
        assertTrue(manager.wasConnected)
        assertFalse(manager.showAlert)

        manager.onConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTED)
        assertFalse(manager.wasConnected)
        assertTrue(manager.showAlert)
    }

    @Test
    fun testUserDismissedAlertDoesNotLoop() {
        val manager = ConnectionLostAlertManager()
        manager.onConnectionStateChanged(BluetoothProfile.STATE_CONNECTED)
        manager.onConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTED)
        assertTrue(manager.showAlert)

        manager.onUserDismissedAlert()
        assertFalse(manager.showAlert)

        // Another DISCONNECTED state emission without being CONNECTED first
        manager.onConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTED)
        assertFalse(manager.showAlert) // No loop!
    }

    @Test
    fun testReconnectionCancelsAlert() {
        val manager = ConnectionLostAlertManager()
        manager.onConnectionStateChanged(BluetoothProfile.STATE_CONNECTED)
        manager.onConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTED)
        assertTrue(manager.showAlert)

        // Reconnected before user dismissed
        manager.onConnectionStateChanged(BluetoothProfile.STATE_CONNECTED)
        assertFalse(manager.showAlert)
    }
}
