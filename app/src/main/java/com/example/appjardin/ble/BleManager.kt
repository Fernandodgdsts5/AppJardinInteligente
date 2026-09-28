package com.example.appjardin.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.appjardin.BuildConfig
import com.example.appjardin.model.ActionCommand
import com.example.appjardin.model.Config
import com.example.appjardin.model.Telemetry
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BleManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
    val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private val bleScanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner
    
    private var bluetoothGatt: BluetoothGatt? = null
    private val bleScope = CoroutineScope(Dispatchers.IO)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices

    private val _telemetry = MutableSharedFlow<Telemetry?>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val telemetry: SharedFlow<Telemetry?> = _telemetry.asSharedFlow()

    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val CHAR_TELEMETRY_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val CHAR_CONFIG_UUID: UUID = UUID.fromString("0a3f7d22-8f4a-4a9b-9a8b-1a2b3c4d5e6f")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun hasBlePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!hasBlePermissions()) return
            val device = result.device ?: return
            try {
                val current = _discoveredDevices.value
                if (current.none { it.address == device.address }) {
                    _discoveredDevices.value = current + device
                }
            } catch (e: SecurityException) {
                Log.e("BleManager", "SecurityException during scan result processing", e)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleManager", "Scan failed with error code: $errorCode")
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            bleScope.launch {
                _connectionState.value = newState
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    try {
                        if (hasBlePermissions()) {
                            // Request a larger MTU (256 bytes) to prevent JSON truncation (Bug 1 fix)
                            val mtuRequested = gatt.requestMtu(256)
                            if (!mtuRequested) {
                                // Fallback if device doesn't support manual MTU request
                                gatt.discoverServices()
                            }
                        }
                    } catch (e: SecurityException) {
                        Log.e("BleManager", "SecurityException discovering services", e)
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    closeGatt()
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d("BleManager", "MTU changed to $mtu (status: $status)")
            try {
                if (hasBlePermissions()) {
                    gatt.discoverServices()
                }
            } catch (e: SecurityException) {
                Log.e("BleManager", "SecurityException discovering services after MTU change", e)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bleScope.launch {
                    try {
                        val service = gatt.getService(SERVICE_UUID) ?: return@launch
                        val telemetryChar = service.getCharacteristic(CHAR_TELEMETRY_UUID) ?: return@launch
                        
                        if (hasBlePermissions()) {
                            gatt.setCharacteristicNotification(telemetryChar, true)
                            val descriptor = telemetryChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                            if (descriptor != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                                } else {
                                    @Suppress("DEPRECATION")
                                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                    @Suppress("DEPRECATION")
                                    gatt.writeDescriptor(descriptor)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("BleManager", "Error enabling notifications", e)
                    }
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && descriptor?.uuid == CLIENT_CHARACTERISTIC_CONFIG_UUID) {
                Log.d("BleManager", "CCCD Subscription successful! Receiving telemetry now.")
            } else {
                Log.e("BleManager", "CCCD Subscription failed with status $status")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Log.d("BleManager", "onCharacteristicWrite completed with status: $status for ${characteristic?.uuid}")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == CHAR_TELEMETRY_UUID) {
                val json = String(value, Charsets.UTF_8)
                parseAndEmitTelemetry(json)
            }
        }

        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == CHAR_TELEMETRY_UUID) {
                val bytes = characteristic.value ?: ByteArray(0)
                val json = String(bytes, Charsets.UTF_8)
                parseAndEmitTelemetry(json)
            }
        }
    }

    private fun parseAndEmitTelemetry(rawJson: String?) {
        if (rawJson.isNullOrBlank()) return
        
        // Remove trailing null-terminators (\u0000) or weird characters sometimes appended by C++ over BLE
        val json = rawJson.trim().trimEnd('\u0000')
        val timestamp = System.currentTimeMillis()
        Log.d("BleManager", "[$timestamp] Raw telemetry received: '$json'")
        
        try {
            val data = Gson().fromJson(json, Telemetry::class.java)
            if (data != null) {
                _telemetry.tryEmit(data)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("BleManager", "Error parsing telemetry JSON safely discarded: $json", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        try {
            if (!hasBlePermissions() || !isBluetoothEnabled()) {
                Log.w("BleManager", "Cannot scan: permissions missing or Bluetooth disabled")
                return
            }
            if (!_isScanning.value) {
                _discoveredDevices.value = emptyList()
                _isScanning.value = true

                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                val filter = ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(SERVICE_UUID))
                    .build()

                try {
                    bleScanner?.startScan(listOf(filter), settings, scanCallback)
                } catch (e: Exception) {
                    Log.w("BleManager", "Filtered scan exception, falling back to unfiltered scan", e)
                    bleScanner?.startScan(null, settings, scanCallback)
                }
            }
        } catch (e: Exception) {
            Log.e("BleManager", "Failed to start scan", e)
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            if (_isScanning.value && hasBlePermissions()) {
                bleScanner?.stopScan(scanCallback)
            }
        } catch (e: Exception) {
            Log.e("BleManager", "Failed to stop scan", e)
        } finally {
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        try {
            stopScan()
            _connectionState.value = BluetoothProfile.STATE_CONNECTING
            if (hasBlePermissions()) {
                bluetoothGatt = device.connectGatt(context, false, gattCallback)
            } else {
                Log.w("BleManager", "Cannot connect: BLE permissions missing")
                _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
            }
        } catch (e: Exception) {
            Log.e("BleManager", "Failed to connect to device", e)
            _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
        }
    }

    private fun determineWriteType(char: BluetoothGattCharacteristic): Int {
        return if ((char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }
    }

    @SuppressLint("MissingPermission")
    fun writeConfig(config: Config) {
        bleScope.launch {
            try {
                val gatt = bluetoothGatt ?: return@launch
                val service = gatt.getService(SERVICE_UUID) ?: return@launch
                val charConfig = service.getCharacteristic(CHAR_CONFIG_UUID) ?: return@launch
                
                if (hasBlePermissions()) {
                    val json = Gson().toJson(config)
                    val bytes = json.toByteArray(Charsets.UTF_8)
                    val writeType = determineWriteType(charConfig)
                    Log.d("BleManager", "Writing config JSON to BLE (writeType=$writeType): $json")
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeCharacteristic(charConfig, bytes, writeType)
                    } else {
                        @Suppress("DEPRECATION")
                        charConfig.value = bytes
                        @Suppress("DEPRECATION")
                        charConfig.writeType = writeType
                        @Suppress("DEPRECATION")
                        gatt.writeCharacteristic(charConfig)
                    }
                }
            } catch (e: Exception) {
                Log.e("BleManager", "Failed to write config", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun sendWateringAction(action: String) {
        bleScope.launch {
            try {
                val gatt = bluetoothGatt ?: return@launch
                val service = gatt.getService(SERVICE_UUID) ?: return@launch
                val charConfig = service.getCharacteristic(CHAR_CONFIG_UUID) ?: return@launch
                
                if (hasBlePermissions()) {
                    val command = ActionCommand(accion = action)
                    val json = Gson().toJson(command)
                    val bytes = json.toByteArray(Charsets.UTF_8)
                    val writeType = determineWriteType(charConfig)
                    Log.d("BleManager", "Sending action JSON to BLE (writeType=$writeType): $json")
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeCharacteristic(charConfig, bytes, writeType)
                    } else {
                        @Suppress("DEPRECATION")
                        charConfig.value = bytes
                        @Suppress("DEPRECATION")
                        charConfig.writeType = writeType
                        @Suppress("DEPRECATION")
                        gatt.writeCharacteristic(charConfig)
                    }
                }
            } catch (e: Exception) {
                Log.e("BleManager", "Failed to send watering action", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            if (hasBlePermissions()) {
                bluetoothGatt?.disconnect()
            }
        } catch (e: Exception) {
            Log.e("BleManager", "Failed to disconnect", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun closeGatt() {
        try {
            if (hasBlePermissions()) {
                bluetoothGatt?.close()
            }
        } catch (e: Exception) {
            Log.e("BleManager", "Failed to close GATT", e)
        } finally {
            bluetoothGatt = null
            _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
        }
    }
}
