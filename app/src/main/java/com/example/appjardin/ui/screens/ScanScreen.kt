package com.example.appjardin.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appjardin.R
import com.example.appjardin.ui.components.FullScreenArtBackground
import com.example.appjardin.ui.theme.ColorVerdeAlegre
import com.example.appjardin.ui.theme.DarkText
import com.example.appjardin.viewmodel.GardenViewModel

@SuppressLint("MissingPermission")
@Composable
fun ScanScreen(
    viewModel: GardenViewModel,
    onConnected: () -> Unit,
    onSkipOffline: () -> Unit,
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

    ScanScreenContent(
        devices = devices,
        isScanning = isScanning,
        connectionState = connectionState,
        isBluetoothEnabled = viewModel.isBluetoothEnabled(),
        onStartScan = { viewModel.startScan() },
        onConnectDevice = { device -> viewModel.connectToDevice(device) },
        onSkipOffline = onSkipOffline,
        onRequestEnableBluetooth = onRequestEnableBluetooth
    )
}

@SuppressLint("MissingPermission")
@Composable
fun ScanScreenContent(
    devices: List<BluetoothDevice>,
    isScanning: Boolean,
    connectionState: Int,
    isBluetoothEnabled: Boolean,
    onStartScan: () -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onSkipOffline: () -> Unit,
    onRequestEnableBluetooth: () -> Unit
) {
    FullScreenArtBackground(
        assetPath = "img/fc.png",
        contentDescription = null,
        aspectRatio = 1376f / 3060f,
        topBoundaryPct = 0.27f,
        imageAlignment = Alignment.TopCenter,
        isLightStatusBar = false,
        isLightNavBar = false,
        topScrimAlpha = 0.40f,
        bottomScrimAlpha = 0.40f
    ) { topSpacerHeight ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(topSpacerHeight))
                }

                // 1. Header Card with Wifi icon, Title & Subtitle
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = ColorVerdeAlegre,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Conectar Jardín Inteligente",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = DarkText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Busca y conecta tus dispositivos cercanos para empezar a cuidar tus plantas.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // 2. Status / Searching Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
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
                                    fontSize = 15.sp,
                                    color = DarkText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
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
                                    if (!isBluetoothEnabled) {
                                        onRequestEnableBluetooth()
                                    } else {
                                        onStartScan()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Buscar de nuevo",
                                        tint = ColorVerdeAlegre
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Warning Card if Disconnected, Not scanning & No devices found
                if (connectionState == BluetoothProfile.STATE_DISCONNECTED && !isScanning && devices.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE).copy(alpha = 0.95f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "No se encontraron dispositivos cercanos.",
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Asegúrate de que tu ESP32 esté encendido y con Bluetooth activado.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFC62828)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = onStartScan,
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorVerdeAlegre),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("Reintentar búsqueda", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 4. Section Title
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_background),
                            contentDescription = null,
                            tint = ColorVerdeAlegre,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Dispositivos cercanos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DarkText
                        )
                    }
                }

                // 5. Device Cards
                items(
                    items = devices,
                    key = { device -> device.address }
                ) { device ->
                    val deviceName = try { device.name ?: "Dispositivo sin nombre" } catch (e: SecurityException) { "Dispositivo BLE" }
                    val isTarget = deviceName.contains("Jardin", ignoreCase = true) || deviceName.contains("ESP32", ignoreCase = true)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConnectDevice(device) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTarget) Color(0xFFE8F5E9).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.94f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (isTarget) ColorVerdeAlegre else Color.Gray,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(end = 10.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deviceName,
                                    fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = DarkText
                                )
                                Text(
                                    text = device.address,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Button(
                                onClick = { onConnectDevice(device) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTarget) ColorVerdeAlegre else Color.Gray
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Conectar >", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Fixed "Saltar" Button at Bottom Center
            Button(
                onClick = onSkipOffline,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .width(180.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorVerdeAlegre,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.btn_skip),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891, name = "ScanScreen Tall 411x891")
@Composable
fun ScanScreenPreviewTall() {
    MaterialTheme {
        ScanScreenContent(
            devices = emptyList(),
            isScanning = true,
            connectionState = BluetoothProfile.STATE_DISCONNECTED,
            isBluetoothEnabled = true,
            onStartScan = {},
            onConnectDevice = {},
            onSkipOffline = {},
            onRequestEnableBluetooth = {}
        )
    }
}
