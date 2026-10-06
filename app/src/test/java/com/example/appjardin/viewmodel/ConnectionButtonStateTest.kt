package com.example.appjardin.viewmodel

import android.bluetooth.BluetoothProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

data class ConnectionButtonConfig(
    val text: String,
    val hasIcon: Boolean,
    val isConnected: Boolean
)

object ConnectionButtonLogic {
    fun getButtonConfig(connectionState: Int, isOffline: Boolean): ConnectionButtonConfig {
        val isConnected = !isOffline && connectionState == BluetoothProfile.STATE_CONNECTED
        return if (isConnected) {
            ConnectionButtonConfig(
                text = "Desconectar Jardín Inteligente",
                hasIcon = false,
                isConnected = true
            )
        } else {
            ConnectionButtonConfig(
                text = "Conectar Jardín Inteligente",
                hasIcon = true,
                isConnected = false
            )
        }
    }
}

class ConnectionButtonStateTest {

    @Test
    fun testConnectedState() {
        val config = ConnectionButtonLogic.getButtonConfig(BluetoothProfile.STATE_CONNECTED, isOffline = false)
        assertEquals("Desconectar Jardín Inteligente", config.text)
        assertFalse(config.hasIcon)
        assertTrue(config.isConnected)
    }

    @Test
    fun testDisconnectedState() {
        val config = ConnectionButtonLogic.getButtonConfig(BluetoothProfile.STATE_DISCONNECTED, isOffline = false)
        assertEquals("Conectar Jardín Inteligente", config.text)
        assertTrue(config.hasIcon)
        assertFalse(config.isConnected)
    }

    @Test
    fun testOfflineModeState() {
        val config = ConnectionButtonLogic.getButtonConfig(BluetoothProfile.STATE_CONNECTED, isOffline = true)
        assertEquals("Conectar Jardín Inteligente", config.text)
        assertTrue(config.hasIcon)
        assertFalse(config.isConnected)
    }
}
