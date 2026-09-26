package com.example.appjardin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appjardin.model.MoistureState
import com.example.appjardin.ui.components.CircularGauge
import com.example.appjardin.ui.theme.*
import com.example.appjardin.viewmodel.GardenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GardenViewModel) {
    val telemetry by viewModel.telemetry.collectAsState()
    val plant by viewModel.selectedPlant.collectAsState()
    
    val humidity = telemetry?.humedad ?: 0f
    val state = viewModel.getMoistureState(humidity, plant)
    
    val stateColor = when (state) {
        MoistureState.NO_PLANT -> ColorNoPlant
        MoistureState.LOW_MOISTURE -> ColorLowMoisture
        MoistureState.MEDIUM_MOISTURE -> ColorMediumMoisture
        MoistureState.GOOD_MOISTURE -> ColorGoodMoisture
        MoistureState.EXCESS_MOISTURE -> ColorExcessMoisture
    }
    
    val stateText = when (state) {
        MoistureState.NO_PLANT -> "Sin planta configurada"
        MoistureState.LOW_MOISTURE -> "Poca humedad"
        MoistureState.MEDIUM_MOISTURE -> "Humedad media"
        MoistureState.GOOD_MOISTURE -> "Humedad adecuada"
        MoistureState.EXCESS_MOISTURE -> "Exceso de humedad"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jardín Inteligente", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = stateColor)
            )
        },
        containerColor = CreamBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularGauge(
                percentage = humidity,
                stateText = stateText,
                stateColor = stateColor
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            val desc = if (plant == null) {
                "Ve a Ajustes y selecciona una planta para empezar a monitorear."
            } else {
                "Planta actual: ${plant!!.name}. Recomendado entre ${plant!!.inicioRiego}% y ${plant!!.finRiego}%."
            }
            
            Text(
                text = desc,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            val pumpOn = telemetry?.bomba == true
            Button(
                onClick = { /* Implement pump toggle if needed */ },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = stateColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (pumpOn) "BOMBA ACTIVA" else if (state == MoistureState.LOW_MOISTURE) "REGAR" else "NO REGAR",
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        }
    }
}
