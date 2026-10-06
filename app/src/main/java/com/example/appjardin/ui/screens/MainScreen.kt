package com.example.appjardin.ui.screens

import android.app.Activity
import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.appjardin.R
import com.example.appjardin.data.local.PlantEntity
import com.example.appjardin.model.MoistureState
import com.example.appjardin.model.Pet
import com.example.appjardin.model.toPetMood
import com.example.appjardin.ui.components.CircularGauge
import com.example.appjardin.ui.theme.*
import com.example.appjardin.util.PetFactUtils
import com.example.appjardin.util.PlantImageStorage
import com.example.appjardin.util.parseFinalHumidity
import com.example.appjardin.viewmodel.ConnectionMode
import com.example.appjardin.viewmodel.GardenViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val FixedHeaderAlpha = 0.52f
private val StatusNeutralGreen = ColorVerdeAlegre

@Composable
private fun MainScreenBackground() {
    val context = LocalContext.current
    val view = LocalView.current

    if (!view.isInEditMode) {
        DisposableEffect(view) {
            val activity = view.context as? Activity
            val window = activity?.window
            val insetsController = if (window != null) WindowCompat.getInsetsController(window, view) else null

            val previousLightStatus = insetsController?.isAppearanceLightStatusBars ?: true

            insetsController?.isAppearanceLightStatusBars = false

            onDispose {
                insetsController?.isAppearanceLightStatusBars = previousLightStatus
            }
        }
    }

    val imageRequest = remember(context) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/img/fe2.png")
            .crossfade(false)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF233B2B), Color(0xFFF6F3DC))
                )
            )
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )

        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarPadding + 28.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1B3123).copy(alpha = 0.40f),
                            Color(0xFF1B3123).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

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
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDiagnosisScanner: () -> Unit = {},
    onNavigateToScan: () -> Unit = {}
) {
    val context = LocalContext.current
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle(initialValue = null)
    val plant by viewModel.selectedPlant.collectAsStateWithLifecycle()
    val currentPlant = plant
    val selectedPet by viewModel.selectedPet.collectAsStateWithLifecycle()

    val connectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()
    val lastSession by viewModel.lastSessionForSelectedPlant.collectAsStateWithLifecycle()
    val isOffline = connectionMode == ConnectionMode.OFFLINE

    val offlineFinalHumidity = remember(lastSession) { parseFinalHumidity(lastSession?.humidities) }
    val realHumidity = telemetry?.humedad ?: 0f

    val effectiveHumidity = if (isOffline) (offlineFinalHumidity ?: 0f) else realHumidity
    val state = if (isOffline && offlineFinalHumidity == null) {
        MoistureState.NO_PLANT
    } else {
        viewModel.getMoistureState(effectiveHumidity, currentPlant)
    }

    val lastReadingDateStr = remember(lastSession) {
        if (lastSession != null) {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(lastSession!!.endTimeMs))
        } else ""
    }

    val subLabelText = if (isOffline) {
        if (offlineFinalHumidity != null && lastReadingDateStr.isNotEmpty()) {
            "${stringResource(R.string.offline_last_reading)} ($lastReadingDateStr)"
        } else {
            stringResource(R.string.offline_no_readings)
        }
    } else {
        "Humedad del suelo"
    }

    val targetColor = if (currentPlant == null || (isOffline && offlineFinalHumidity == null)) {
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
    } else if (isOffline && offlineFinalHumidity == null) {
        "Sin lecturas"
    } else {
        when (state) {
            MoistureState.NO_PLANT -> "Sin planta"
            MoistureState.LOW_MOISTURE -> "Poca humedad"
            MoistureState.MEDIUM_MOISTURE -> "Humedad media"
            MoistureState.GOOD_MOISTURE -> "Humedad adecuada"
            MoistureState.EXCESS_MOISTURE -> "Exceso de humedad"
        }
    }

    val isExcess = !isOffline && (telemetry?.exceso == true || (currentPlant != null && realHumidity > currentPlant.humedadExceso))
    val pumpOn by viewModel.pumpOn.collectAsStateWithLifecycle()

    val defaultRes = PlantImageStorage.getDefaultDrawableRes(currentPlant?.defaultKey)
    val plantImageModel = when {
        currentPlant != null && !currentPlant.imagePath.isNullOrBlank() -> File(currentPlant.imagePath)
        defaultRes != null -> defaultRes
        else -> R.drawable.planta
    }

    // Plant facts state surviving rotation via rememberSaveable
    val factArrayRes = remember(selectedPet.id) {
        PetFactUtils.getFactArrayResForPet(selectedPet.id)
    }
    val factsArray = remember(factArrayRes) { context.resources.getStringArray(factArrayRes) }
    var shuffledFacts by remember(selectedPet.id) { mutableStateOf(factsArray.toList().shuffled()) }
    var factIndex by remember(selectedPet.id) { mutableIntStateOf(0) }
    var showFact by remember(selectedPet.id) { mutableStateOf(false) }
    var factTapCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedPet.id) {
        showFact = false
    }

    LaunchedEffect(factTapCount) {
        if (factTapCount > 0) {
            delay(5000)
            showFact = false
        }
    }

    val handleFactTap: () -> Unit = {
        showFact = true
        factTapCount++
        if (factIndex >= shuffledFacts.size - 1) {
            val lastFact = shuffledFacts.lastOrNull()
            var newShuffled = factsArray.toList().shuffled()
            if (newShuffled.size > 1 && newShuffled.first() == lastFact) {
                newShuffled = newShuffled.shuffled()
            }
            shuffledFacts = newShuffled
            factIndex = 0
        } else {
            factIndex++
        }
    }

    MainScreenContent(
        activeColor = activeColor,
        stateText = stateText,
        subLabelText = subLabelText,
        effectiveHumidity = effectiveHumidity,
        isOffline = isOffline,
        offlineFinalHumidity = offlineFinalHumidity,
        currentPlant = currentPlant,
        plantImageModel = plantImageModel,
        selectedPet = selectedPet,
        showFact = showFact,
        shuffledFacts = shuffledFacts,
        factIndex = factIndex,
        isExcess = isExcess,
        pumpOn = pumpOn,
        handleFactTap = handleFactTap,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToDiagnosisScanner = onNavigateToDiagnosisScanner,
        onNavigateToScan = onNavigateToScan,
        onTogglePump = { turnOn -> viewModel.togglePump(turnOn) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    activeColor: Color,
    stateText: String,
    subLabelText: String,
    effectiveHumidity: Float,
    isOffline: Boolean,
    offlineFinalHumidity: Float?,
    currentPlant: PlantEntity?,
    plantImageModel: Any,
    selectedPet: Pet,
    showFact: Boolean,
    shuffledFacts: List<String>,
    factIndex: Int,
    isExcess: Boolean,
    pumpOn: Boolean,
    handleFactTap: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnosisScanner: () -> Unit,
    onNavigateToScan: () -> Unit,
    onTogglePump: (Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Stable background component with fe2.png artwork ONLY for MainScreen
        MainScreenBackground()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Jardín Inteligente",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.35f),
                                        offset = Offset(1f, 1f),
                                        blurRadius = 3f
                                    )
                                )
                            )
                        }
                    },
                    actions = {
                        if (isOffline) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable { onNavigateToScan() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BluetoothDisabled,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.offline_mode_chip),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = StatusNeutralGreen.copy(alpha = FixedHeaderAlpha)
                    )
                )
            },
            containerColor = Color.Transparent
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
                                    percentage = if (currentPlant == null || (isOffline && offlineFinalHumidity == null)) 0f else effectiveHumidity,
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
                                    text = subLabelText,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
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
                                    contentDescription = currentPlant?.name ?: "Planta",
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
                        val petMood = when {
                            currentPlant == null -> MoistureState.NO_PLANT.toPetMood()
                            isOffline && offlineFinalHumidity == null -> MoistureState.NO_PLANT.toPetMood()
                            else -> {
                                val currentMoistureState = if (isOffline) {
                                    MoistureState.GOOD_MOISTURE
                                } else {
                                    MoistureState.GOOD_MOISTURE
                                }
                                currentMoistureState.toPetMood()
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Image(
                                painter = painterResource(id = selectedPet.getDrawable(selectedPet.getDrawable(petMood).let { petMood })),
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
                                // Reserved Slot for Fact Card
                                Column(
                                    modifier = Modifier
                                        .wrapContentHeight()
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
                                            border = BorderStroke(1.5.dp, StatusNeutralGreen),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        ) {
                                            Text(
                                                text = shuffledFacts.getOrElse(factIndex) { "Las plantas aman el agua." },
                                                fontSize = 11.sp,
                                                color = DarkText,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(10.dp)
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
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusNeutralGreen),
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

                    // 3 & 4. BOTTOM BUTTONS (Watering + Diagnose)
                    val (buttonText, isEnabled, buttonAction) = when {
                        isOffline -> {
                            Triple(stringResource(R.string.offline_watering_disabled), false) {}
                        }
                        currentPlant == null -> {
                            Triple("ELEGIR PLANTA", true) { onNavigateToSettings() }
                        }
                        isExcess -> {
                            Triple("EXCESO DE HUMEDAD", false) {}
                        }
                        pumpOn -> {
                            Triple("DETENER RIEGO", true) { onTogglePump(false) }
                        }
                        else -> {
                            Triple("REGAR", true) { onTogglePump(true) }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentPlant != null && isExcess && !pumpOn && !isOffline) {
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
                                containerColor = StatusNeutralGreen,
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
                            onClick = {
                                if (currentPlant == null) {
                                    onNavigateToSettings()
                                } else {
                                    onNavigateToDiagnosisScanner()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .neumorphic(28.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CreamBackground,
                                contentColor = StatusNeutralGreen
                            ),
                            border = BorderStroke(1.dp, StatusNeutralGreen)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = StatusNeutralGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.diagnose_plant),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusNeutralGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891, name = "Good Moisture")
@Composable
fun MainScreenPreviewGood() {
    MaterialTheme {
        MainScreenContent(
            activeColor = ColorGoodMoisture,
            stateText = "Humedad adecuada",
            subLabelText = "Humedad del suelo",
            effectiveHumidity = 65f,
            isOffline = false,
            offlineFinalHumidity = null,
            currentPlant = PlantEntity(1, "Tomate", 30, 60, 80),
            plantImageModel = R.drawable.planta,
            selectedPet = Pet.GUSANO,
            showFact = false,
            shuffledFacts = emptyList(),
            factIndex = 0,
            isExcess = false,
            pumpOn = false,
            handleFactTap = {},
            onNavigateToSettings = {},
            onNavigateToDiagnosisScanner = {},
            onNavigateToScan = {},
            onTogglePump = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891, name = "Low Moisture")
@Composable
fun MainScreenPreviewLow() {
    MaterialTheme {
        MainScreenContent(
            activeColor = ColorLowMoisture,
            stateText = "Poca humedad",
            subLabelText = "Humedad del suelo",
            effectiveHumidity = 18f,
            isOffline = false,
            offlineFinalHumidity = null,
            currentPlant = PlantEntity(1, "Tomate", 30, 60, 80),
            plantImageModel = R.drawable.planta,
            selectedPet = Pet.GUSANO,
            showFact = false,
            shuffledFacts = emptyList(),
            factIndex = 0,
            isExcess = false,
            pumpOn = false,
            handleFactTap = {},
            onNavigateToSettings = {},
            onNavigateToDiagnosisScanner = {},
            onNavigateToScan = {},
            onTogglePump = {}
        )
    }
}
