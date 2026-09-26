package com.example.appjardin.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appjardin.ui.theme.ColorVerdeAlegre
import com.example.appjardin.ui.theme.CreamBackground
import com.example.appjardin.ui.theme.DarkText
import com.example.appjardin.viewmodel.GardenViewModel

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: GardenViewModel,
    onConnected: () -> Unit,
    onRequestEnableBluetooth: () -> Unit
) {
    val devices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    // Automatically navigate when connected
    LaunchedEffect(connectionState) {
        if (connectionState == BluetoothProfile.STATE_CONNECTED) {
            onConnected()
        }
    }

    // Start scan on screen entry
    LaunchedEffect(Unit) {
        if (!viewModel.isBluetoothEnabled()) {
            onRequestEnableBluetooth()
        } else {
            viewModel.startScan()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conectar Jardín Inteligente", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorVerdeAlegre)
            )
        },
        containerColor = CreamBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Status Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (connectionState) {
                                BluetoothProfile.STATE_CONNECTING -> "Conectando al ESP32..."
                                BluetoothProfile.STATE_CONNECTED -> "¡Jardín Conectado!"
                                else -> if (isScanning) "Buscando dispositivos BLE..." else "Selecciona tu dispositivo"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DarkText
                        )
                        Text(
                            text = "Dispositivo esperado: JardinInteligente-ESP32",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    if (isScanning || connectionState == BluetoothProfile.STATE_CONNECTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = ColorVerdeAlegre,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = {
                            if (!viewModel.isBluetoothEnabled()) {
                                onRequestEnableBluetooth()
                            } else {
                                viewModel.startScan()
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Buscar de nuevo", tint = ColorVerdeAlegre)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = connectionState == BluetoothProfile.STATE_DISCONNECTED && !isScanning && devices.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("No se encontraron dispositivos cercanos.", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        Text("Asegúrate de que tu ESP32 esté encendido y con Bluetooth activado.", fontSize = 12.sp, color = Color(0xFFC62828))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.startScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorVerdeAlegre)
                        ) {
                            Text("Reintentar búsqueda")
                        }
                    }
                }
            }

            Text(
                text = "Dispositivos cercanos:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = DarkText,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = devices,
                    key = { device -> device.address }
                ) { device ->
                    val deviceName = try { device.name ?: "Dispositivo sin nombre" } catch (e: SecurityException) { "Dispositivo BLE" }
                    val isTarget = deviceName.contains("Jardin", ignoreCase = true) || deviceName.contains("ESP32", ignoreCase = true)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.connectToDevice(device)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTarget) Color(0xFFE8F5E9) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (isTarget) ColorVerdeAlegre else Color.Gray,
                                modifier = Modifier.size(32.dp).padding(end = 12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deviceName,
                                    fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = DarkText
                                )
                                Text(
                                    text = device.address,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Button(
                                onClick = { viewModel.connectToDevice(device) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTarget) ColorVerdeAlegre else Color.Gray
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Conectar", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
