package com.example.appjardin.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.appjardin.R
import com.example.appjardin.viewmodel.DiagnosisUiState
import com.example.appjardin.viewmodel.DiagnosisViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosisScreen(
    viewModel: DiagnosisViewModel,
    activeColor: Color,
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isTorchEnabled by rememberSaveable { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Se requiere permiso de cámara para diagnosticar", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                isTorchEnabled = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is DiagnosisUiState.Saved) {
            val savedId = state.id
            isTorchEnabled = false
            viewModel.resetToCamera()
            onNavigateToDetail(savedId)
        } else if (state is DiagnosisUiState.Error) {
            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            viewModel.retryCamera()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnosticar Planta", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        isTorchEnabled = false
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = activeColor)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (!hasCameraPermission) {
                Text("Esperando permiso de cámara...", color = Color.White)
                return@Scaffold
            }

            when (val state = uiState) {
                is DiagnosisUiState.ModelUnavailable -> {
                    Text("El modelo de IA aún no está disponible", color = Color.White)
                }
                is DiagnosisUiState.Camera -> {
                    CameraPreviewView(
                        activeColor = activeColor,
                        isTorchEnabled = isTorchEnabled,
                        onToggleTorch = { enabled -> isTorchEnabled = enabled },
                        onPhotoTaken = { file -> viewModel.onPhotoCaptured(file) }
                    )
                }
                is DiagnosisUiState.Captured -> {
                    AsyncImage(
                        model = state.bitmap,
                        contentDescription = "Foto capturada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { viewModel.retryCamera() },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Reintentar", color = Color.White)
                        }
                        Button(
                            onClick = { viewModel.analyzePhoto(state.bitmap) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                        ) {
                            Text("Diagnosticar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is DiagnosisUiState.Analyzing -> {
                    // Show scanning animation
                    val infiniteTransition = rememberInfiniteTransition(label = "ScanAnim")
                    val scanOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "ScanOffset"
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        // We assume the last captured bitmap is still somewhat available, but if not we just show scanning
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = 500.dp * scanOffset) // Approximation
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, activeColor, Color.Transparent)
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = activeColor)
                        }
                    }
                }
                is DiagnosisUiState.LowConfidence -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            "No se pudo determinar con suficiente confianza.",
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.retryCamera() },
                            colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                        ) {
                            Text("Volver a intentarlo")
                        }
                    }
                }
                else -> {
                    // Saved or Error, handled in LaunchedEffect
                }
            }
        }
    }
}

@Composable
fun CameraPreviewView(
    activeColor: Color,
    isTorchEnabled: Boolean,
    onToggleTorch: (Boolean) -> Unit,
    onPhotoTaken: (File) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    val cameraInfo = cameraInstance?.cameraInfo
    val torchLiveData = remember(cameraInfo) { cameraInfo?.torchState }
    var isTorchOnByLiveData by remember { mutableStateOf(false) }

    DisposableEffect(torchLiveData) {
        val observer = Observer<Int> { state ->
            isTorchOnByLiveData = (state == TorchState.ON)
        }
        torchLiveData?.observeForever(observer)
        onDispose {
            torchLiveData?.removeObserver(observer)
        }
    }

    val hasFlashUnit = remember(cameraInfo) { cameraInfo?.hasFlashUnit() == true }

    LaunchedEffect(cameraInstance, isTorchEnabled, hasFlashUnit) {
        val cam = cameraInstance
        if (cam != null && hasFlashUnit) {
            val future = cam.cameraControl.enableTorch(isTorchEnabled)
            future.addListener({
                try {
                    future.get()
                } catch (e: Exception) {
                    onToggleTorch(false)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    DisposableEffect(cameraInstance) {
        onDispose {
            cameraInstance?.cameraControl?.enableTorch(false)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "FrameAnim")
    val frameAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FrameAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    imageCapture = ImageCapture.Builder().build()
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val boundCamera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                        cameraInstance = boundCamera
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Error de cámara", Toast.LENGTH_SHORT).show()
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Focus Frame
        Box(
            modifier = Modifier
                .fillMaxSize(0.8f)
                .align(Alignment.Center)
                .border(2.dp, activeColor.copy(alpha = frameAlpha), RoundedCornerShape(24.dp))
        )

        // Torch Toggle Button overlay (Top-End)
        if (hasFlashUnit) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
            ) {
                IconButton(
                    onClick = {
                        val nextState = !isTorchOnByLiveData
                        onToggleTorch(nextState)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isTorchOnByLiveData) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = stringResource(
                            if (isTorchOnByLiveData) R.string.cd_torch_turn_off else R.string.cd_torch_turn_on
                        ),
                        tint = if (isTorchOnByLiveData) activeColor else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Button(
            onClick = {
                val cacheDir = File(context.cacheDir, "diagnosis")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val photoFile = File(cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture?.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            onPhotoTaken(photoFile)
                        }
                        override fun onError(exc: ImageCaptureException) {
                            Toast.makeText(context, "Error al capturar", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .height(60.dp)
                .fillMaxWidth(0.6f),
            colors = ButtonDefaults.buttonColors(containerColor = activeColor),
            shape = RoundedCornerShape(30.dp)
        ) {
            Text("Diagnosticar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
