package com.example.appjardin.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.util.Log
import com.example.appjardin.model.Config
import com.example.appjardin.model.Telemetry
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false

    private val _telemetry = MutableStateFlow<Telemetry?>(null)
    val telemetry: StateFlow<Telemetry?> = _telemetry

    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState

    companion object {
        private const val DEVICE_NAME = "JardinInteligente-ESP32"
        val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val CHAR_TELEMETRY_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val CHAR_CONFIG_UUID: UUID = UUID.fromString("8a0b0d91-2dc0-44ec-b8fa-3f9dbb9ce6e9")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device.name == DEVICE_NAME || device.name?.contains("Jardin") == true) {
                stopScan()
                connectToDevice(device)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            _connectionState.value = newState
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    val telemetryChar = service.getCharacteristic(CHAR_TELEMETRY_UUID)
                    if (telemetryChar != null) {
                        gatt.setCharacteristicNotification(telemetryChar, true)
                        val descriptor = telemetryChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == CHAR_TELEMETRY_UUID) {
                val json = characteristic.getStringValue(0)
                try {
                    val data = Gson().fromJson(json, Telemetry::class.java)
                    _telemetry.value = data
                } catch (e: Exception) {
                    Log.e("BleManager", "Error parsing telemetry: $json", e)
                }
            }
        }
    }

    fun startScan() {
        if (!isScanning && bluetoothAdapter?.isEnabled == true) {
            isScanning = true
            bleScanner?.startScan(scanCallback)
        }
    }

    fun stopScan() {
        if (isScanning) {
            isScanning = false
            bleScanner?.stopScan(scanCallback)
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun writeConfig(config: Config) {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(SERVICE_UUID) ?: return
        val charConfig = service.getCharacteristic(CHAR_CONFIG_UUID) ?: return
        
        val json = Gson().toJson(config)
        charConfig.value = json.toByteArray()
        gatt.writeCharacteristic(charConfig)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
    }
}
