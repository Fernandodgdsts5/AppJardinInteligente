package com.example.appjardin.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.appjardin.R
import com.example.appjardin.model.MoistureState
import com.example.appjardin.model.toPetMood
import com.example.appjardin.ui.components.CircularGauge
import com.example.appjardin.ui.theme.*
import com.example.appjardin.util.PlantImageStorage
import com.example.appjardin.viewmodel.GardenViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: GardenViewModel,
    onNavigateToSettings: () -> Unit = {}
) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle(initialValue = null)
    val plant by viewModel.selectedPlant.collectAsStateWithLifecycle()
    val selectedPet by viewModel.selectedPet.collectAsStateWithLifecycle()
    
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

    val isExcess = telemetry?.exceso == true || (plant != null && humidity > plant!!.humedadExceso)
    val pumpOn by viewModel.pumpOn.collectAsStateWithLifecycle()

    val defaultRes = PlantImageStorage.getDefaultDrawableRes(plant?.defaultKey)
    val plantImageModel = when {
        plant != null && !plant!!.imagePath.isNullOrBlank() -> File(plant!!.imagePath!!)
        defaultRes != null -> defaultRes
        else -> R.drawable.planta
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. MAIN CARD (Two columns: Gauge on left, Plant photo on right)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Column (~50%): Moisture Gauge & Details
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = activeColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.size(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularGauge(
                                percentage = if (plant == null) 0f else humidity,
                                stateText = stateText,
                                stateColor = activeColor,
                                modifier = Modifier.size(120.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Humedad del suelo",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Right Column (~50%): Plant Photo
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 6.dp)
                    ) {
                        AsyncImage(
                            model = plantImageModel,
                            contentDescription = plant?.name ?: "Planta",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // 2. PET (Centered, height ~130dp, emotion based on humidity, no speech bubble)
            val petMood = state.toPetMood()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = selectedPet.getDrawable(petMood)),
                    contentDescription = selectedPet.speciesName,
                    modifier = Modifier
                        .height(130.dp)
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
            }

            // 3. WATERING BUTTON (Intact logic & style)
            val (buttonText, isEnabled, buttonAction) = when {
                plant == null -> {
                    Triple("ELEGIR PLANTA", true) { onNavigateToSettings() }
                }
                isExcess -> {
                    Triple("EXCESO DE HUMEDAD", false) {}
                }
                pumpOn -> {
                    Triple("DETENER RIEGO", true) { viewModel.togglePump(false) }
                }
                else -> {
                    Triple("REGAR", true) { viewModel.togglePump(true) }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (plant != null && isExcess && !pumpOn) {
                    Text(
                        text = "El riego manual está bloqueado por exceso de humedad (> ${plant!!.humedadExceso}%)",
                        color = ColorExcessMoisture,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Button(
                    onClick = buttonAction,
                    enabled = isEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeColor,
                        disabledContainerColor = Color.LightGray,
                        disabledContentColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) Color.White else Color.DarkGray
                    )
                }
            }

            // 4. DIAGNOSE PLANT BUTTON ("Diagnosticar mi planta")
            val darkGreen = Color(0xFF2E5E3E)
            val lightCream = Color(0xFFFFF9EE)

            OutlinedButton(
                onClick = { /* TODO: Diagnóstico */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = lightCream,
                    contentColor = darkGreen
                ),
                border = BorderStroke(1.5.dp, darkGreen)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = darkGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.diagnose_plant),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = darkGreen
                    )
                }
            }
        }
    }
}
