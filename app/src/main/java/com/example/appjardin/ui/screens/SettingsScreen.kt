package com.example.appjardin.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.appjardin.R
import com.example.appjardin.data.local.PlantEntity
import com.example.appjardin.model.MoistureState
import com.example.appjardin.ui.theme.*
import com.example.appjardin.util.PlantImageStorage
import com.example.appjardin.viewmodel.GardenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: GardenViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    val selectedPlant by viewModel.selectedPlant.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle(initialValue = null)

    val humidity = telemetry?.humedad ?: 0f
    val state = viewModel.getMoistureState(humidity, selectedPlant)

    val activeColor = if (selectedPlant == null) {
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

    var isEditingName by remember { mutableStateOf(false) }
    var tempNameInput by remember(userName) { mutableStateOf(userName) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Image change flow states
    var plantToEditImageId by remember { mutableStateOf<Int?>(null) }
    var showImageSourceSheet by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            pendingImageUri = tempCameraUri
            showPreviewDialog = true
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingImageUri = uri
            showPreviewDialog = true
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createImageUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Ajustes",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // USERNAME SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Nombre de Usuario",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isEditingName) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = userName.ifBlank { "Guardián de las Plantas" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                            TextButton(onClick = {
                                tempNameInput = userName
                                isEditingName = true
                            }) {
                                Text("Editar nombre", color = activeColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column {
                            OutlinedTextField(
                                value = tempNameInput,
                                onValueChange = { tempNameInput = it },
                                label = { Text("Nuevo nombre") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activeColor,
                                    focusedLabelColor = activeColor
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { isEditingName = false }) {
                                    Text("Cancelar", color = Color.Gray)
                                }
                                Button(
                                    onClick = {
                                        if (tempNameInput.isNotBlank()) {
                                            viewModel.saveUserName(tempNameInput)
                                        }
                                        isEditingName = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                                ) {
                                    Text("Guardar")
                                }
                            }
                        }
                    }
                }
            }

            // PLANT SELECTION CARDS SECTION
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Seleccionar Planta",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                plants.forEach { plant ->
                    val isSelected = selectedPlant?.id == plant.id
                    PlantSelectionCard(
                        plant = plant,
                        isSelected = isSelected,
                        activeColor = activeColor,
                        onCardClick = { viewModel.selectPlant(plant.id) },
                        onEditImageClick = {
                            plantToEditImageId = plant.id
                            showImageSourceSheet = true
                        }
                    )
                }
            }

            // NEW PLANT BUTTON
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = activeColor)
            ) {
                Text(
                    text = "Nueva Planta",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Add Plant Dialog
        if (showAddDialog) {
            AddPlantDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { newPlant ->
                    viewModel.addPlant(newPlant)
                    showAddDialog = false
                },
                activeColor = activeColor,
                context = context,
                scope = scope
            )
        }

        // Image Source Bottom Sheet
        if (showImageSourceSheet) {
            ModalBottomSheet(
                onDismissRequest = { showImageSourceSheet = false },
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Actualizar imagen de planta",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceSheet = false
                                val permission = Manifest.permission.CAMERA
                                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                    val uri = createImageUri(context)
                                    tempCameraUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    cameraPermissionLauncher.launch(permission)
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = activeColor)
                        Text("Tomar foto", fontSize = 16.sp, color = DarkText, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceSheet = false
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = activeColor)
                        Text("Elegir de la galería", fontSize = 16.sp, color = DarkText, fontWeight = FontWeight.Medium)
                    }

                    val targetPlant = plants.find { it.id == plantToEditImageId }
                    if (targetPlant?.defaultKey != null && !targetPlant.imagePath.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showImageSourceSheet = false
                                    plantToEditImageId?.let { id ->
                                        scope.launch {
                                            // Delete old file
                                            PlantImageStorage.deleteImageFile(targetPlant.imagePath)
                                            viewModel.updatePlantImage(id, null)
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray)
                            Text("Restaurar imagen original", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Preview & Confirmation Dialog
        if (showPreviewDialog && pendingImageUri != null) {
            AlertDialog(
                onDismissRequest = {
                    showPreviewDialog = false
                    pendingImageUri = null
                },
                title = { Text("Vista previa", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = pendingImageUri,
                            contentDescription = "Vista previa",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text("¿Deseas usar esta foto?", fontSize = 14.sp, color = DarkText)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val uri = pendingImageUri
                            val plantId = plantToEditImageId
                            showPreviewDialog = false
                            pendingImageUri = null

                            if (uri != null && plantId != null) {
                                scope.launch(Dispatchers.IO) {
                                    val internalPath = PlantImageStorage.saveImageToInternalStorage(context, uri)
                                    withContext(Dispatchers.Main) {
                                        if (internalPath != null) {
                                            viewModel.updatePlantImage(plantId, internalPath)
                                        } else {
                                            Toast.makeText(context, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                    ) {
                        Text("Usar esta foto", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPreviewDialog = false
                        // Retry -> reopen source sheet
                        showImageSourceSheet = true
                    }) {
                        Text("Volver a intentar", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun PlantSelectionCard(
    plant: PlantEntity,
    isSelected: Boolean,
    activeColor: Color,
    onCardClick: () -> Unit,
    onEditImageClick: () -> Unit
) {
    val defaultRes = PlantImageStorage.getDefaultDrawableRes(plant.defaultKey)
    val imageModel = when {
        !plant.imagePath.isNullOrBlank() -> File(plant.imagePath)
        defaultRes != null -> defaultRes
        else -> R.drawable.planta
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(2.dp, activeColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: Name & Humidity data
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = plant.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Seleccionada",
                            tint = activeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mín: ${plant.humedadMinima}% | Opt: ${plant.humedadBuena}% | Exceso: > ${plant.humedadExceso}%",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Right side: Square image (~90% of card height) with edit pencil icon at top end
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.9f)
                    .aspectRatio(1f)
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = plant.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                // Edit pencil icon container (min 48dp touch target)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.TopEnd)
                        .clickable { onEditImageClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar imagen",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantDialog(
    onDismiss: () -> Unit,
    onAdd: (PlantEntity) -> Unit,
    activeColor: Color,
    context: Context,
    scope: CoroutineScope
) {
    var name by remember { mutableStateOf("") }
    var minStr by remember { mutableStateOf("") }
    var buenaStr by remember { mutableStateOf("") }
    var excesoStr by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var customImagePath by remember { mutableStateOf<String?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempUri != null) {
            pendingUri = tempUri
            showPreview = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pendingUri = uri
            showPreview = true
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val u = createImageUri(context)
            tempUri = u
            cameraLauncher.launch(u)
        } else {
            Toast.makeText(context, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Planta Personalizada", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Image picker field
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray.copy(alpha = 0.3f))
                        .clickable { showSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (customImagePath != null) {
                        AsyncImage(
                            model = File(customImagePath!!),
                            contentDescription = "Imagen seleccionada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Agregar imagen", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la Planta") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = minStr,
                    onValueChange = { minStr = it },
                    label = { Text("Humedad Mínima (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = buenaStr,
                    onValueChange = { buenaStr = it },
                    label = { Text("Humedad Buena (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = excesoStr,
                    onValueChange = { excesoStr = it },
                    label = { Text("Humedad Exceso (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val min = minStr.toIntOrNull()
                    val buena = buenaStr.toIntOrNull()
                    val exceso = excesoStr.toIntOrNull()

                    if (name.isBlank() || min == null || buena == null || exceso == null) {
                        errorMsg = "Completa todos los campos con números válidos."
                    } else if (min < 0 || buena < 0 || exceso > 100) {
                        errorMsg = "Los porcentajes deben estar entre 0% y 100%."
                    } else if (!(min < buena && buena < exceso)) {
                        errorMsg = "Regla requerida: Min < Buena < Exceso"
                    } else {
                        onAdd(
                            PlantEntity(
                                name = name,
                                humedadMinima = min,
                                humedadBuena = buena,
                                humedadExceso = exceso,
                                imagePath = customImagePath,
                                defaultKey = null
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = activeColor)
            ) {
                Text("Guardar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )

    if (showSheet) {
        ExperimentalMaterial3Api::class
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Imagen de planta", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSheet = false
                            val perm = Manifest.permission.CAMERA
                            if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
                                val u = createImageUri(context)
                                tempUri = u
                                cameraLauncher.launch(u)
                            } else {
                                permLauncher.launch(perm)
                            }
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = activeColor)
                    Text("Tomar foto", fontSize = 16.sp, color = DarkText)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSheet = false
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = activeColor)
                    Text("Elegir de la galería", fontSize = 16.sp, color = DarkText)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showPreview && pendingUri != null) {
        AlertDialog(
            onDismissRequest = {
                showPreview = false
                pendingUri = null
            },
            title = { Text("Vista previa", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = pendingUri,
                        contentDescription = "Vista previa",
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val u = pendingUri
                        showPreview = false
                        pendingUri = null
                        if (u != null) {
                            scope.launch(Dispatchers.IO) {
                                val path = PlantImageStorage.saveImageToInternalStorage(context, u)
                                withContext(Dispatchers.Main) {
                                    if (path != null) {
                                        customImagePath = path
                                    } else {
                                        Toast.makeText(context, "Error al guardar imagen", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                ) {
                    Text("Usar esta foto", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPreview = false
                    showSheet = true
                }) {
                    Text("Volver a intentar", color = Color.Gray)
                }
            }
        )
    }
}

private fun createImageUri(context: Context): Uri {
    val imageFile = File(context.cacheDir, "temp_camera_image_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}
