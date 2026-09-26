package com.example.appjardin

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.example.appjardin.model.MoistureState
import com.example.appjardin.ui.screens.*
import com.example.appjardin.ui.theme.*
import com.example.appjardin.viewmodel.GardenViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GardenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppJardinTheme {
                var permissionsGranted by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    permissionsGranted = results.all { it.value }
                }

                val enableBtLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { _ ->
                    viewModel.startScan()
                }

                fun checkPermissionsAndStartBt(onPermissionOk: () -> Unit) {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    } else {
                        arrayOf(
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    }
                    permissionLauncher.launch(permissions)
                    onPermissionOk()
                }

                val rootNavController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CreamBackground
                ) {
                    NavHost(
                        navController = rootNavController,
                        startDestination = "welcome"
                    ) {
                        composable("welcome") {
                            WelcomeScreen(
                                viewModel = viewModel,
                                onConnectClick = {
                                    checkPermissionsAndStartBt {
                                        if (!viewModel.isBluetoothEnabled()) {
                                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                            enableBtLauncher.launch(enableBtIntent)
                                        }
                                        rootNavController.navigate("scan")
                                    }
                                }
                            )
                        }

                        composable("scan") {
                            ScanScreen(
                                viewModel = viewModel,
                                onConnected = {
                                    rootNavController.navigate("main_app") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                },
                                onRequestEnableBluetooth = {
                                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                    enableBtLauncher.launch(enableBtIntent)
                                }
                            )
                        }

                        composable("main_app") {
                            MainAppContent(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnectBle()
    }
}

@Composable
fun MainAppContent(viewModel: GardenViewModel) {
    val bottomNavController = rememberNavController()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val plant by viewModel.selectedPlant.collectAsStateWithLifecycle()

    val humidity = telemetry?.humedad ?: 0f
    val state = viewModel.getMoistureState(humidity, plant)

    val activeColor = if (plant == null) {
        ColorVerdeAlegre
    } else {
        when (state) {
            MoistureState.NO_PLANT -> ColorVerdeAlegre
            MoistureState.LOW_MOISTURE -> ColorLowMoisture
            MoistureState.MEDIUM_MOISTURE -> ColorMediumMoisture
            MoistureState.GOOD_MOISTURE -> ColorGoodMoisture
            MoistureState.EXCESS_MOISTURE -> ColorExcessMoisture
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = activeColor
            ) {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Estado"
                        )
                    },
                    label = { Text("Estado", fontWeight = FontWeight.Bold) },
                    selected = currentRoute == "main",
                    onClick = {
                        bottomNavController.navigate("main") {
                            popUpTo("main") { inclusive = true }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = activeColor,
                        indicatorColor = activeColor,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Historial"
                        )
                    },
                    label = { Text("Historial", fontWeight = FontWeight.Bold) },
                    selected = currentRoute == "history",
                    onClick = {
                        bottomNavController.navigate("history") {
                            popUpTo("main")
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = activeColor,
                        indicatorColor = activeColor,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes"
                        )
                    },
                    label = { Text("Ajustes", fontWeight = FontWeight.Bold) },
                    selected = currentRoute == "settings",
                    onClick = {
                        bottomNavController.navigate("settings") {
                            popUpTo("main")
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = activeColor,
                        indicatorColor = activeColor,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CreamBackground)
        ) {
            NavHost(
                navController = bottomNavController,
                startDestination = "main"
            ) {
                composable("main") { MainScreen(viewModel) }
                composable("history") { HistoryScreen(viewModel) }
                composable("settings") { SettingsScreen(viewModel) }
            }
        }
    }
}
