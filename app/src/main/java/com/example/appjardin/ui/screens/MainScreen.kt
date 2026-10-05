package com.example.appjardin.ui.screens

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import kotlinx.coroutines.delay
import java.io.File

fun Modifier.neumorphic(cornerRadius: Dp = 28.dp) = this.drawBehind {
    val paint = Paint().apply {
        isAntiAlias = true
    }
    val radius = cornerRadius.toPx()
    
    // Top-Light Highlight
    paint.color = android.graphics.Color.WHITE
    paint.setShadowLayer(
        10.dp.toPx(),
        -4.dp.toPx(),
        -4.dp.toPx(),
        android.graphics.Color.argb(160, 255, 255, 255)
    )
    drawContext.canvas.nativeCanvas.drawRoundRect(
        0f, 0f, size.width, size.height,
        radius, radius,
        paint
    )
    
    // Bottom-Dark Shadow
    paint.color = android.graphics.Color.TRANSPARENT
    paint.setShadowLayer(
        10.dp.toPx(),
        4.dp.toPx(),
        4.dp.toPx(),
        android.graphics.Color.argb(35, 0, 0, 0)
    )
    drawContext.canvas.nativeCanvas.drawRoundRect(
        0f, 0f, size.width, size.height,
        radius, radius,
        paint
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: GardenViewModel,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle(initialValue = null)
    val plant by viewModel.selectedPlant.collectAsStateWithLifecycle()
    val currentPlant = plant
    val selectedPet by viewModel.selectedPet.collectAsStateWithLifecycle()
    
    val humidity = telemetry?.humedad ?: 0f
    val state = viewModel.getMoistureState(humidity, currentPlant)
    
    // Always use ColorVerdeAlegre when no plant is configured
    val targetColor = if (currentPlant == null) {
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
    
    val stateText = if (currentPlant == null) {
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

    val isExcess = telemetry?.exceso == true || (currentPlant != null && humidity > currentPlant.humedadExceso)
    val pumpOn by viewModel.pumpOn.collectAsStateWithLifecycle()

    val defaultRes = PlantImageStorage.getDefaultDrawableRes(currentPlant?.defaultKey)
    val plantImageModel = when {
        currentPlant != null && !currentPlant.imagePath.isNullOrBlank() -> File(currentPlant.imagePath)
        defaultRes != null -> defaultRes
        else -> R.drawable.planta
    }

    // Plant facts state surviving rotation via rememberSaveable
    val factsArray = remember { context.resources.getStringArray(R.array.plant_facts) }
    var shuffledFacts by remember { mutableStateOf(factsArray.toList().shuffled()) }
    var factIndex by rememberSaveable { mutableStateOf(0) }
    var showFact by rememberSaveable { mutableStateOf(false) }
    var factTapCount by remember { mutableStateOf(0) }

    LaunchedEffect(factTapCount) {
        if (factTapCount > 0) {
            delay(5000)
            showFact = false
        }
    }

    val handleFactTap = {
        showFact = true
        factTapCount++
        if (factIndex >= shuffledFacts.size) {
            shuffledFacts = factsArray.toList().shuffled()
            factIndex = 0
        }
        factIndex = (factIndex + 1) % shuffledFacts.size
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
        @Suppress("UnusedBoxWithConstraintsScope") // maxHeight needed for heightIn min constraint inside verticalScroll
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val availableHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = availableHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. MAIN CARD (Height 265dp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(265.dp),
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
                        // Left Column (~50%): Moisture Gauge & Details with equal itemSpacing
                        val itemSpacing = 6.dp
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
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(itemSpacing))
                            CircularGauge(
                                percentage = if (plant == null) 0f else humidity,
                                stateColor = activeColor,
                                modifier = Modifier.size(110.dp)
                            )
                            Spacer(modifier = Modifier.height(itemSpacing))
                            Text(
                                text = stateText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(itemSpacing))
                            Text(
                                text = "Humedad del suelo",
                                fontSize = 11.sp,
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

                // 2. PET + "DATO CURIOSO" BUTTON ROW (Vertically centered in available space)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val petMood = state.toPetMood()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Image(
                            painter = painterResource(id = selectedPet.getDrawable(petMood)),
                            contentDescription = selectedPet.speciesName,
                            modifier = Modifier
                                .height(240.dp)
                                .fillMaxWidth(0.55f)
                                .aspectRatio(1f),
                            contentScale = ContentScale.Fit
                        )

                        // Right side Column with reserved slot for fact card + button
                        Column(
                            modifier = Modifier
                                .width(130.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // Reserved Slot for Fact Card (90.dp height)
                            Column(
                                modifier = Modifier
                                    .height(90.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedVisibility(
                                    visible = showFact,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.5.dp, activeColor),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = shuffledFacts.getOrElse(factIndex) { "Las plantas aman el agua." },
                                            fontSize = 11.sp,
                                            color = DarkText,
                                            textAlign = TextAlign.Center,
                                            maxLines = 4,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // "Dato curioso" Button
                            Button(
                                onClick = handleFactTap,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                                shape = RoundedCornerShape(19.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.advice_button),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 3 & 4. BOTTOM BUTTONS (Watering + Diagnose) positioned at 8dp from bottom edge
                val (buttonText, isEnabled, buttonAction) = when {
                    currentPlant == null -> {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentPlant != null && isExcess && !pumpOn) {
                        Text(
                            text = "El riego manual está bloqueado por exceso de humedad (> ${currentPlant.humedadExceso}%)",
                            color = ColorExcessMoisture,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 2.dp)
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

                    // DIAGNOSE PLANT BUTTON (Soft Neumorphic Style)
                    OutlinedButton(
                        onClick = { /* TODO: Diagnóstico */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .neumorphic(28.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CreamBackground,
                            contentColor = activeColor
                        ),
                        border = BorderStroke(1.dp, activeColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = activeColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.diagnose_plant),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = activeColor
                            )
                        }
                    }
                }
            }
        }
    }
}
