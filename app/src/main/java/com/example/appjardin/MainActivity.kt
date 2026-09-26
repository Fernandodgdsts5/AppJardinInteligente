package com.example.appjardin

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.appjardin.ui.screens.HistoryScreen
import com.example.appjardin.ui.screens.MainScreen
import com.example.appjardin.ui.screens.SettingsScreen
import com.example.appjardin.ui.theme.AppJardinTheme
import com.example.appjardin.viewmodel.GardenViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GardenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestBlePermissions()

        setContent {
            AppJardinTheme {
                val navController = rememberNavController()
                
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = "Estado") },
                                label = { Text("Estado") },
                                selected = currentRoute == "main",
                                onClick = {
                                    navController.navigate("main") {
                                        popUpTo("main") { inclusive = true }
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.List, contentDescription = "Historial") },
                                label = { Text("Historial") },
                                selected = currentRoute == "history",
                                onClick = {
                                    navController.navigate("history")
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                                label = { Text("Ajustes") },
                                selected = currentRoute == "settings",
                                onClick = {
                                    navController.navigate("settings")
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("main") { MainScreen(viewModel) }
                        composable("history") { HistoryScreen(viewModel) }
                        composable("settings") { SettingsScreen(viewModel) }
                    }
                }
            }
        }
    }
    
    private fun requestBlePermissions() {
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
        
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            if (results.all { it.value }) {
                viewModel.startScan()
            }
        }
        
        requestPermissionLauncher.launch(permissions)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnectBle()
    }
}
