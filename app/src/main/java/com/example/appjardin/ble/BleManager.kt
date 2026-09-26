package com.example.appjardin.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.appjardin.model.Config
import com.example.appjardin.model.Telemetry
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _telemetry = MutableStateFlow<Telemetry?>(null)
    val telemetry: StateFlow<Telemetry?> = _telemetry

    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val CHAR_TELEMETRY_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val CHAR_CONFIG_UUID: UUID = UUID.fromString("8a0b0d91-2dc0-44ec-b8fa-3f9dbb9ce6e9")
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
                            gatt.discoverServices()
                        }
                    } catch (e: SecurityException) {
                        Log.e("BleManager", "SecurityException discovering services", e)
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    closeGatt()
                }
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
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("BleManager", "Error enabling notifications", e)
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == CHAR_TELEMETRY_UUID) {
                val json = characteristic.getStringValue(0)
                bleScope.launch {
                    try {
                        val data = Gson().fromJson(json, Telemetry::class.java)
                        _telemetry.value = data
                    } catch (e: Exception) {
                        Log.e("BleManager", "Error parsing telemetry JSON: $json", e)
                    }
                }
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
                bleScanner?.startScan(scanCallback)
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
            }
        } catch (e: Exception) {
            Log.e("BleManager", "Failed to connect to device", e)
            _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
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
                    charConfig.value = json.toByteArray()
                    gatt.writeCharacteristic(charConfig)
                }
            } catch (e: Exception) {
                Log.e("BleManager", "Failed to write config", e)
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
