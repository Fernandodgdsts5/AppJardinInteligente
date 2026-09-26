package com.example.appjardin.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appjardin.R
import com.example.appjardin.model.MoistureState
import com.example.appjardin.ui.components.CircularGauge
import com.example.appjardin.ui.theme.*
import com.example.appjardin.viewmodel.GardenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GardenViewModel) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val plant by viewModel.selectedPlant.collectAsStateWithLifecycle()
    
    val humidity = telemetry?.humedad ?: 0f
    val state = viewModel.getMoistureState(humidity, plant)
    
    // Always use ColorVerdeAlegre when no plant is configured
    val targetColor = if (plant == null) {
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
    
    val activeColor by animateColorAsState(targetValue = targetColor, label = "ColorAnimation")
    
    val stateText = if (plant == null) {
        "Sin planta"
    } else {
        when (state) {
            MoistureState.NO_PLANT -> "Sin planta"
            MoistureState.LOW_MOISTURE -> "Poca humedad"
            MoistureState.MEDIUM_MOISTURE -> "Humedad media"
            MoistureState.GOOD_MOISTURE -> "Humedad adecuada"
            MoistureState.EXCESS_MOISTURE -> "Exceso de humedad"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Jardín Inteligente",
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header text below bar
            Text(
                text = "Humedad del suelo",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Circular Gauge Arc
            CircularGauge(
                percentage = if (plant == null) 0f else humidity,
                stateText = stateText,
                stateColor = activeColor
            )

            // Plant Image Resource
            Image(
                painter = painterResource(id = R.drawable.planta),
                contentDescription = "Planta en maceta",
                modifier = Modifier.size(130.dp)
            )

            // Guide text message
            val desc = if (plant == null) {
                "Ve a Ajustes y selecciona una planta para empezar a monitorear"
            } else {
                "Planta actual: ${plant!!.name}. Recomendado entre ${plant!!.inicioRiego}% y ${plant!!.finRiego}%."
            }
            
            Text(
                text = desc,
                fontSize = 15.sp,
                color = DarkText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Bottom Action Button
            val pumpOn = telemetry?.bomba == true
            val buttonText = if (plant == null) {
                "ELEGIR PLANTA"
            } else if (pumpOn) {
                "BOMBA ACTIVA"
            } else if (state == MoistureState.LOW_MOISTURE) {
                "REGAR"
            } else {
                "NO REGAR"
            }

            Button(
                onClick = {
                    if (plant != null) {
                        viewModel.togglePump(!pumpOn)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
