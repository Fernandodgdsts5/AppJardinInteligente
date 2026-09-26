package com.example.appjardin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.appjardin.viewmodel.GardenViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: GardenViewModel) {
    val sessions by viewModel.allSessions.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Riego") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay sesiones de riego registradas.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions) { session ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Planta: ${session.plantName}", style = MaterialTheme.typography.titleMedium)
                            Text("Inicio: ${formatDate(session.startTimeMs)}")
                            Text("Fin: ${formatDate(session.endTimeMs)}")
                            
                            val hums = session.humidities.split(",")
                            val avg = if (hums.isNotEmpty()) hums.mapNotNull { it.toFloatOrNull() }.average() else 0.0
                            Text("Humedad Promedio: ${String.format("%.1f", avg)}%")
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(ms: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(ms))
}
