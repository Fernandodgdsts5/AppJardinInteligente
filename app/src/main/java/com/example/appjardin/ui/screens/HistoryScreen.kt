package com.example.appjardin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appjardin.model.MoistureState
import com.example.appjardin.ui.theme.*
import com.example.appjardin.viewmodel.GardenViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: GardenViewModel) {
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
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
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Historial de Riego",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = activeColor)
            )
        },
        containerColor = CreamBackground
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay sesiones de riego registradas aún.",
                    fontSize = 16.sp,
                    color = DarkText
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = sessions,
                    key = { session -> session.id }
                ) { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = session.plantName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                                Surface(
                                    color = activeColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Sesión #${session.id}",
                                        color = activeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Inicio: ${formatDate(session.startTimeMs)}", fontSize = 14.sp, color = DarkText)
                            Text("Fin: ${formatDate(session.endTimeMs)}", fontSize = 14.sp, color = DarkText)

                            val hums = session.humidities.split(",")
                            val avg = if (hums.isNotEmpty()) hums.mapNotNull { it.toFloatOrNull() }.average() else 0.0
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Humedad Promedio: ${String.format(Locale.getDefault(), "%.1f", avg)}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeColor
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(ms: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(ms))
}
