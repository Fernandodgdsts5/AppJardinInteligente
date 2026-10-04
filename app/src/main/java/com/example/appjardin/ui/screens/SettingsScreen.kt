package com.example.appjardin.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.res.painterResource
import com.example.appjardin.model.Pet
import com.example.appjardin.model.PetMood
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import com.example.appjardin.R
import com.example.appjardin.data.local.PlantEntity
import com.example.appjardin.model.MoistureState
import com.example.appjardin.model.Telemetry
import com.example.appjardin.ui.theme.*
import com.example.appjardin.util.PlantImageStorage
import com.example.appjardin.viewmodel.GardenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

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
    var plantToEdit by remember { mutableStateOf<PlantEntity?>(null) }
    
    // Plant detail bottom sheet state surviving rotation via ID
    var plantDetailId by rememberSaveable { mutableStateOf<Int?>(null) }

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
                                ),
                                trailingIcon = {
                                    if (tempNameInput.isNotBlank()) {
                                        IconButton(onClick = { tempNameInput = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                        }
                                    }
                                }
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

                val listState = rememberLazyListState()
                val previousSize = remember { mutableStateOf(plants.size) }

                LaunchedEffect(plants.size) {
                    if (plants.size > previousSize.value) {
                        listState.animateScrollToItem(plants.size - 1)
                    }
                    previousSize.value = plants.size
                }

                val cardHeight = 90.dp
                val spacing = 8.dp
                val visibleCount = minOf(plants.size, 3)
                val calculatedHeight = if (plants.isEmpty()) 0.dp else (cardHeight * visibleCount) + (spacing * (visibleCount - 1))

                Box(modifier = Modifier.fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(calculatedHeight),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        items(
                            items = plants,
                            key = { it.id }
                        ) { plant ->
                            val isSelected = selectedPlant?.id == plant.id
                            PlantSelectionCard(
                                plant = plant,
                                isSelected = isSelected,
                                activeColor = activeColor,
                                onCardClick = {
                                    viewModel.selectPlant(plant.id)
                                    plantDetailId = plant.id
                                },
                                onEditClick = { plantToEdit = plant }
                            )
                        }
                    }

                    if (plants.size > 3 && listState.canScrollForward) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, CreamBackground.copy(alpha = 0.9f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.scroll_more),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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

            // PET SELECTOR SECTION
            val petNames by viewModel.petNames.collectAsStateWithLifecycle()
            val selectedPetName by viewModel.selectedPetName.collectAsStateWithLifecycle()
            val selectedPet by viewModel.selectedPet.collectAsStateWithLifecycle()
            var isPetExpanded by rememberSaveable { mutableStateOf(false) }
            var isEditingPetName by remember { mutableStateOf(false) }

            val currentEffectiveName = petNames[selectedPet.id] ?: selectedPet.defaultName
            var tempPetNameInput by remember(currentEffectiveName) { mutableStateOf(currentEffectiveName) }

            LaunchedEffect(selectedPet.id) {
                isEditingPetName = false
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clickable { isPetExpanded = !isPetExpanded },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.pet_section_label),
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = selectedPetName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.9f)
                            .aspectRatio(1f)
                    ) {
                        Image(
                            painter = painterResource(id = selectedPet.getDrawable(PetMood.FELIZ)),
                            contentDescription = selectedPet.speciesName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isPetExpanded) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pet Name row / Edit
                        if (!isEditingPetName) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Nombre de la mascota: $currentEffectiveName",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkText
                                    )
                                    TextButton(onClick = {
                                        tempPetNameInput = currentEffectiveName
                                        isEditingPetName = true
                                    }) {
                                        Text(stringResource(R.string.pet_name_edit), color = activeColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (currentEffectiveName != selectedPet.defaultName) {
                                    TextButton(
                                        onClick = { viewModel.resetPetName(selectedPet.id) },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(stringResource(R.string.reset_name), fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        } else {
                            Column {
                                OutlinedTextField(
                                    value = tempPetNameInput,
                                    onValueChange = { if (it.length <= 20) tempPetNameInput = it },
                                    label = { Text(stringResource(R.string.pet_name_title)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = activeColor,
                                        focusedLabelColor = activeColor
                                    ),
                                    trailingIcon = {
                                        if (tempPetNameInput.isNotBlank()) {
                                            IconButton(onClick = { tempPetNameInput = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                            }
                                        }
                                    }
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { isEditingPetName = false }) {
                                        Text("Cancelar", color = Color.Gray)
                                    }
                                    Button(
                                        onClick = {
                                            val trimmed = tempPetNameInput.trim()
                                            if (trimmed.isNotBlank()) {
                                                if (trimmed == selectedPet.defaultName) {
                                                    viewModel.resetPetName(selectedPet.id)
                                                } else {
                                                    viewModel.savePetName(selectedPet.id, trimmed)
                                                }
                                            }
                                            isEditingPetName = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                                    ) {
                                        Text("Guardar")
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        // Pet Grid (2 columns)
                        val pets = Pet.entries
                        pets.chunked(2).forEach { rowPets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowPets.forEach { pet ->
                                    val isPetSelected = selectedPet == pet
                                    val petEffectiveName = petNames[pet.id] ?: pet.defaultName
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                BorderStroke(
                                                    width = if (isPetSelected) 2.dp else 1.dp,
                                                    color = if (isPetSelected) activeColor else Color.LightGray.copy(alpha = 0.5f)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .background(if (isPetSelected) activeColor.copy(alpha = 0.05f) else Color.White)
                                            .clickable {
                                                viewModel.selectPet(pet.id)
                                                isEditingPetName = false
                                            }
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(70.dp)
                                            ) {
                                                Image(
                                                    painter = painterResource(id = pet.getDrawable(PetMood.FELIZ)),
                                                    contentDescription = pet.speciesName,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                                if (isPetSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .align(Alignment.TopEnd)
                                                            .background(activeColor, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Seleccionada",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = petEffectiveName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DarkText
                                            )
                                        }
                                    }
                                }
                                if (rowPets.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Plant Detail Bottom Sheet
        if (plantDetailId != null) {
            val currentPlant = plants.find { it.id == plantDetailId }
            if (currentPlant != null) {
                PlantDetailBottomSheet(
                    plant = currentPlant,
                    selectedPlantId = selectedPlant?.id,
                    telemetry = telemetry,
                    viewModel = viewModel,
                    onDismiss = { plantDetailId = null },
                    onEdit = {
                        plantDetailId = null
                        plantToEdit = currentPlant
                    },
                    activeColor = activeColor
                )
            } else {
                plantDetailId = null
            }
        }

        // Add Plant Dialog
        if (showAddDialog) {
            PlantFormDialog(
                plantToEdit = null,
                onDismiss = { showAddDialog = false },
                onSave = { newPlant, imagePath ->
                    val plantToSave = newPlant.copy(imagePath = imagePath)
                    viewModel.addPlant(plantToSave)
                    showAddDialog = false
                },
                activeColor = activeColor,
                context = context,
                scope = scope
            )
        }

        // Edit Plant Dialog
        if (plantToEdit != null) {
            PlantFormDialog(
                plantToEdit = plantToEdit,
                onDismiss = { plantToEdit = null },
                onSave = { updatedPlant, newImagePath ->
                    viewModel.updatePlant(updatedPlant, newImagePath)
                    plantToEdit = null
                },
                activeColor = activeColor,
                context = context,
                scope = scope
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
    onEditClick: () -> Unit
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
                        .clickable { onEditClick() },
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
                            contentDescription = "Editar planta",
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
fun PlantDetailBottomSheet(
    plant: PlantEntity,
    selectedPlantId: Int?,
    telemetry: Telemetry?,
    viewModel: GardenViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    activeColor: Color
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val defaultRes = PlantImageStorage.getDefaultDrawableRes(plant.defaultKey)
    val imageModel = when {
        !plant.imagePath.isNullOrBlank() -> File(plant.imagePath)
        defaultRes != null -> defaultRes
        else -> R.drawable.planta
    }

    val isSelected = selectedPlantId == plant.id
    val humidity = telemetry?.humedad ?: 0f
    val moistureState = if (isSelected && telemetry != null) {
        viewModel.getMoistureState(humidity, plant)
    } else null

    val stateText = moistureState?.let {
        when (it) {
            MoistureState.LOW_MOISTURE -> "Poca humedad"
            MoistureState.MEDIUM_MOISTURE -> "Humedad media"
            MoistureState.GOOD_MOISTURE -> "Humedad adecuada"
            MoistureState.EXCESS_MOISTURE -> "Exceso de humedad"
            else -> null
        }
    }

    val stateColor = moistureState?.let {
        when (it) {
            MoistureState.LOW_MOISTURE -> ColorLowMoisture
            MoistureState.MEDIUM_MOISTURE -> ColorMediumMoisture
            MoistureState.GOOD_MOISTURE -> ColorGoodMoisture
            MoistureState.EXCESS_MOISTURE -> ColorExcessMoisture
            else -> Color.Gray
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Large Image (4:3 aspect ratio)
            AsyncImage(
                model = imageModel,
                contentDescription = plant.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            // Name & Selected badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plant.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                if (isSelected) {
                    Surface(
                        color = activeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Planta seleccionada",
                            color = activeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Type
            val plantType = if (plant.defaultKey != null) "Predeterminada" else "Personalizada"
            Text(
                text = "Tipo: $plantType",
                fontSize = 14.sp,
                color = Color.Gray
            )

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

            // Humidity Thresholds
            Text(
                text = "Umbrales de Humedad",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThresholdRow("Humedad Mínima", "${plant.humedadMinima}%", ColorLowMoisture)
                ThresholdRow("Humedad Adecuada (Buena)", "${plant.humedadBuena}%", ColorGoodMoisture)
                ThresholdRow("Humedad Exceso", "> ${plant.humedadExceso}%", ColorExcessMoisture)
            }

            // Current Humidity (Only if selected and telemetry available)
            if (isSelected && telemetry != null && stateText != null && stateColor != null) {
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                Text(
                    text = "Estado Actual en Vivo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = stateColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Humedad actual: ${String.format(Locale.getDefault(), "%.1f", humidity)}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        Surface(
                            color = stateColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stateText,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    border = BorderStroke(1.5.dp, activeColor)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = activeColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", color = activeColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                ) {
                    Text("Cerrar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Delete button separated to prevent accidental taps
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar planta", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar ${plant.name}?", fontWeight = FontWeight.Bold) },
            text = { Text("Se eliminará la planta y su imagen. El historial de humedad conservará sus registros.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deletePlant(
                            plant = plant,
                            onSuccess = {
                                Toast.makeText(context, "Planta eliminada", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            onError = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ThresholdRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Text(text = label, fontSize = 14.sp, color = DarkText)
        }
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantFormDialog(
    plantToEdit: PlantEntity? = null,
    onDismiss: () -> Unit,
    onSave: (PlantEntity, String?) -> Unit,
    activeColor: Color,
    context: Context,
    scope: CoroutineScope
) {
    val isEditing = plantToEdit != null
    var name by remember { mutableStateOf(plantToEdit?.name ?: "") }
    var minStr by remember { mutableStateOf(plantToEdit?.humedadMinima?.toString() ?: "") }
    var buenaStr by remember { mutableStateOf(plantToEdit?.humedadBuena?.toString() ?: "") }
    var excesoStr by remember { mutableStateOf(plantToEdit?.humedadExceso?.toString() ?: "") }
    var errorMsg by remember { mutableStateOf("") }
    
    var currentImagePath by remember { mutableStateOf(plantToEdit?.imagePath) }
    
    // Using rememberSaveable since Uri is Parcelable and survives activity recreation
    var pendingUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showPreview by rememberSaveable { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var tempCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val ts = System.currentTimeMillis()
        Log.d("PlantCamera", "[$ts] cameraLauncher result: success=$success, uri=$tempCameraUri")
        if (success && tempCameraUri != null) {
            pendingUri = tempCameraUri
            showPreview = true
        } else {
            cleanupTempUri(tempCameraUri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val ts = System.currentTimeMillis()
        Log.d("PlantCamera", "[$ts] galleryLauncher result: uri=$uri")
        if (uri != null) {
            pendingUri = uri
            showPreview = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val ts = System.currentTimeMillis()
        Log.d("PlantCamera", "[$ts] cameraPermissionLauncher result: granted=$granted")
        if (granted) {
            val u = createImageUri(context)
            if (u != null) {
                tempCameraUri = u
                Log.d("PlantCamera", "[$ts] Launching camera with uri: $u")
                cameraLauncher.launch(u)
            }
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = {
            cleanupTempUri(tempCameraUri)
            cleanupTempUri(pendingUri)
            onDismiss()
        },
        title = { Text(if (isEditing) "Editar Planta" else "Nueva Planta Personalizada", fontWeight = FontWeight.Bold) },
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
                    val defaultRes = plantToEdit?.let { PlantImageStorage.getDefaultDrawableRes(it.defaultKey) }
                    val displayModel = when {
                        !currentImagePath.isNullOrBlank() -> File(currentImagePath!!)
                        defaultRes != null -> defaultRes
                        else -> R.drawable.planta
                    }

                    AsyncImage(
                        model = displayModel,
                        contentDescription = "Imagen de planta",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Edit overlay icon
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Cambiar", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la Planta") },
                    singleLine = true,
                    trailingIcon = {
                        if (name.isNotBlank()) {
                            IconButton(onClick = { name = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    }
                )
                OutlinedTextField(
                    value = minStr,
                    onValueChange = { minStr = it },
                    label = { Text("Humedad Mínima (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    trailingIcon = {
                        if (minStr.isNotBlank()) {
                            IconButton(onClick = { minStr = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    }
                )
                OutlinedTextField(
                    value = buenaStr,
                    onValueChange = { buenaStr = it },
                    label = { Text("Humedad Buena (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    trailingIcon = {
                        if (buenaStr.isNotBlank()) {
                            IconButton(onClick = { buenaStr = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    }
                )
                OutlinedTextField(
                    value = excesoStr,
                    onValueChange = { excesoStr = it },
                    label = { Text("Humedad Exceso (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    trailingIcon = {
                        if (excesoStr.isNotBlank()) {
                            IconButton(onClick = { excesoStr = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    }
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
                        val plantResult = if (isEditing) {
                            plantToEdit.copy(
                                name = name.trim(),
                                humedadMinima = min,
                                humedadBuena = buena,
                                humedadExceso = exceso
                            )
                        } else {
                            PlantEntity(
                                name = name.trim(),
                                humedadMinima = min,
                                humedadBuena = buena,
                                humedadExceso = exceso,
                                defaultKey = null
                            )
                        }
                        cleanupTempUri(tempCameraUri)
                        cleanupTempUri(pendingUri)
                        onSave(plantResult, currentImagePath)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = activeColor)
            ) {
                Text("Guardar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                cleanupTempUri(tempCameraUri)
                cleanupTempUri(pendingUri)
                onDismiss()
            }) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )

    if (showSheet) {
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
                            val ts = System.currentTimeMillis()
                            Log.d("PlantCamera", "[$ts] Tapped 'Tomar foto'")
                            val perm = Manifest.permission.CAMERA
                            if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
                                val u = createImageUri(context)
                                if (u != null) {
                                    tempCameraUri = u
                                    Log.d("PlantCamera", "[$ts] Launching camera with uri: $u")
                                    cameraLauncher.launch(u)
                                }
                            } else {
                                cameraPermissionLauncher.launch(perm)
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

                if (!currentImagePath.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSheet = false
                                currentImagePath = null
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                        Text("Quitar foto personalizada", fontSize = 16.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showPreview && pendingUri != null) {
        AlertDialog(
            onDismissRequest = {
                showPreview = false
                cleanupTempUri(pendingUri)
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
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(pendingUri)
                            .size(1024, 1024)
                            .build(),
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
                            val ts = System.currentTimeMillis()
                            Log.d("PlantCamera", "[$ts] Start processing image from uri: $u")
                            scope.launch(Dispatchers.IO) {
                                val path = PlantImageStorage.saveImageToInternalStorage(context, u)
                                cleanupTempUri(u)
                                withContext(Dispatchers.Main) {
                                    val endTs = System.currentTimeMillis()
                                    Log.d("PlantCamera", "[$endTs] Finished processing image. Result path: $path")
                                    if (path != null) {
                                        currentImagePath = path
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
                    cleanupTempUri(pendingUri)
                    pendingUri = null
                    showSheet = true
                }) {
                    Text("Volver a intentar", color = Color.Gray)
                }
            }
        )
    }
}

private fun createImageUri(context: Context): Uri? {
    return try {
        val cameraDir = File(context.cacheDir, "camera")
        if (!cameraDir.exists()) {
            cameraDir.mkdirs()
        }
        cameraDir.listFiles()?.forEach { file ->
            if (System.currentTimeMillis() - file.lastModified() > 3600000L) {
                file.delete()
            }
        }
        val imageFile = File(cameraDir, "temp_camera_${System.currentTimeMillis()}.jpg")
        imageFile.createNewFile()
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    } catch (e: Exception) {
        android.util.Log.e("PlantCamera", "Error creating camera image URI", e)
        Toast.makeText(context, "Error al preparar la cámara", Toast.LENGTH_SHORT).show()
        null
    }
}

private fun cleanupTempUri(uri: Uri?) {
    if (uri == null) return
    try {
        val path = uri.path ?: return
        val file = File(path)
        if (file.exists() && file.absolutePath.contains("camera")) {
            file.delete()
        } else if (uri.scheme == "file") {
            file.delete()
        }
    } catch (e: Exception) {
        android.util.Log.e("PlantCamera", "Error cleaning up temp uri: $uri", e)
    }
}
