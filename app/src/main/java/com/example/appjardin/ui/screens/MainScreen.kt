package com.example.appjardin.ui.screens

import android.content.res.AssetFileDescriptor
import android.graphics.Paint
import android.media.MediaPlayer
import android.graphics.SurfaceTexture
import android.view.TextureView
import android.view.Surface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

@Composable
fun PlantVideoPlayer(
    modifier: Modifier = Modifier,
    activeColor: Color,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasError by remember { mutableStateOf(false) }

    val mediaPlayer = remember {
        try {
            val afd: AssetFileDescriptor = context.assets.openFd("animaciones/ab.mp4")
            MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true
                setVolume(0f, 0f)
                prepare()
                start()
            }
        } catch (e: Exception) {
            hasError = true
            null
        }
    }

    DisposableEffect(lifecycleOwner, mediaPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START -> {
                    try {
                        if (mediaPlayer != null && !mediaPlayer.isPlaying) {
                            mediaPlayer.start()
                        }
                    } catch (e: Exception) {}
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    try {
                        if (mediaPlayer != null && mediaPlayer.isPlaying) {
                            mediaPlayer.pause()
                        }
                    } catch (e: Exception) {}
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mediaPlayer?.release()
            } catch (e: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                onClick = onTap,
                role = Role.Button
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!hasError && mediaPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                                try {
                                    mediaPlayer.setSurface(Surface(surface))
                                } catch (e: Exception) {}
                            }
                            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                try {
                                    mediaPlayer.setSurface(null)
                                } catch (e: Exception) {}
                                return true
                            }
                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Surface(
                color = activeColor,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Dato curioso",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
        // Transparent touch overlay guaranteeing touch target >= 48dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClick = onTap,
                    role = Role.Button
                ),
            contentAlignment = Alignment.Center
        ) {
            if (hasError || mediaPlayer == null) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Dato curioso",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
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

    // Plant facts state
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

    val handleVideoTap = {
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
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

                // 2. PET + VIDEO ROW (Vertically centered in available space)
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

                        PlantVideoPlayer(
                            modifier = Modifier
                                .height(130.dp)
                                .width(110.dp),
                            activeColor = activeColor,
                            onTap = handleVideoTap
                        )
                    }

                    // Fact bubble overlay
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.End
                    ) {
                        AnimatedVisibility(
                            visible = showFact,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.padding(end = 120.dp, top = 10.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(2.dp, activeColor),
                                modifier = Modifier.widthIn(max = 220.dp)
                            ) {
                                Text(
                                    text = shuffledFacts.getOrElse(factIndex) { "Las plantas aman el agua y la luz solar." },
                                    fontSize = 13.sp,
                                    color = DarkText,
                                    maxLines = 4,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // 3 & 4. BOTTOM BUTTONS (Watering + Diagnose) positioned at 8dp from bottom edge
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (plant != null && isExcess && !pumpOn) {
                        Text(
                            text = "El riego manual está bloqueado por exceso de humedad (> ${plant!!.humedadExceso}%)",
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
