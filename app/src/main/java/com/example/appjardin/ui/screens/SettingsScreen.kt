package com.example.appjardin.ui.screens

import android.Manifest
import android.bluetooth.BluetoothProfile
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.text.style.TextOverflow
import com.example.appjardin.viewmodel.ConnectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.res.painterResource
import com.example.appjardin.model.AppTheme
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
import com.example.appjardin.model.GameConfig
import com.example.appjardin.model.MoistureState
import com.example.appjardin.model.Telemetry
import com.example.appjardin.ui.theme.*
import com.example.appjardin.util.PlantImageStorage
import com.example.appjardin.util.parseFinalHumidity
import com.example.appjardin.viewmodel.GardenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val PET_CARD_HEIGHT = 180.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GardenViewModel,
    onNavigateToMissions: () -> Unit = {},
    onNavigateToScan: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    val selectedPlant by viewModel.selectedPlant.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle(initialValue = null)
    val unlockedPets by viewModel.unlockedPets.collectAsStateWithLifecycle()
    val petNames by viewModel.petNames.collectAsStateWithLifecycle()

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()
    val lastSession by viewModel.lastSessionForSelectedPlant.collectAsStateWithLifecycle()

    val isOffline = connectionMode == ConnectionMode.OFFLINE
    val isConnected = !isOffline && connectionState == BluetoothProfile.STATE_CONNECTED
    val isDisconnecting = connectionState == BluetoothProfile.STATE_DISCONNECTING

    val liveHumidity = telemetry?.humedad ?: 0f
    val offlineFinalHumidity = remember(lastSession) { parseFinalHumidity(lastSession?.humidities) }

    val effectiveHumidity = if (isConnected) liveHumidity else (offlineFinalHumidity ?: 0f)
    val buttonMoistureState = if (!isConnected && offlineFinalHumidity == null) {
        MoistureState.NO_PLANT
    } else {
        viewModel.getMoistureState(effectiveHumidity, selectedPlant)
    }

    val buttonTargetColor = if (selectedPlant == null || (!isConnected && offlineFinalHumidity == null)) {
        ColorVerdeAlegre
    } else {
        when (buttonMoistureState) {
            MoistureState.NO_PLANT -> ColorVerdeAlegre
            MoistureState.LOW_MOISTURE -> ColorLowMoisture
            MoistureState.MEDIUM_MOISTURE -> ColorMediumMoisture
            MoistureState.GOOD_MOISTURE -> ColorGoodMoisture
            MoistureState.EXCESS_MOISTURE -> ColorExcessMoisture
        }
    }

    val buttonColor by animateColorAsState(targetValue = buttonTargetColor, label = "ConnectionButtonColor")

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
    var lockedPetToUnlock by remember { mutableStateOf<Pet?>(null) }
    var unlockedPetForCongratulations by rememberSaveable { mutableStateOf<Pet?>(null) }
    
    // Plant detail bottom sheet state surviving rotation via ID
    var plantDetailId by rememberSaveable { mutableStateOf<Int?>(null) }

    val scrollState = rememberScrollState()

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
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
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

            // BLE CONNECTION CARD SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (isConnected) {
                                viewModel.disconnectBle()
                            } else {
                                onNavigateToScan()
                            }
                        },
                        enabled = !isDisconnecting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor,
                            contentColor = Color.White,
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.DarkGray
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isConnected) "Desconectar Jardín Inteligente" else "Conectar Jardín Inteligente",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!isConnected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
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
            var isThemeExpanded by rememberSaveable { mutableStateOf(false) }
            var isEditingPetName by remember { mutableStateOf(false) }

            val currentEffectiveName = petNames[selectedPet.id] ?: selectedPet.defaultName
            var tempPetNameInput by remember(currentEffectiveName) { mutableStateOf(currentEffectiveName) }

            LaunchedEffect(selectedPet.id) {
                isEditingPetName = false
            }

            LaunchedEffect(isPetExpanded) {
                if (isPetExpanded) {
                    isThemeExpanded = false
                    delay(200)
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }

            LaunchedEffect(isThemeExpanded) {
                if (isThemeExpanded) {
                    isPetExpanded = false
                    delay(200)
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PET_CARD_HEIGHT)
                    .clickable { isPetExpanded = !isPetExpanded },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.pet_section_label),
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedPetName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = selectedPet.getDrawable(PetMood.FELIZ)),
                            contentDescription = selectedPet.speciesName,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
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

                        // Pet Grid (Ordered: Larva, Gusano, Hormiga, Chanchito, Abeja + Reygeko double cell at end)
                        val petsTop = listOf(Pet.LARVA, Pet.GUSANO, Pet.HORMIGA, Pet.CHANCHITO, Pet.ABEJA)
                        petsTop.chunked(2).forEach { rowPets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowPets.forEach { pet ->
                                    val isPetLocked = pet.isLocked && !unlockedPets.contains(pet.id)
                                    PetGridCell(
                                        pet = pet,
                                        selectedPet = selectedPet,
                                        petNames = petNames,
                                        unlockedPets = unlockedPets,
                                        activeColor = activeColor,
                                        modifier = Modifier.weight(1f),
                                        onPetClick = {
                                            if (isPetLocked) {
                                                lockedPetToUnlock = pet
                                            } else {
                                                viewModel.selectPet(pet.id)
                                                isEditingPetName = false
                                            }
                                        }
                                    )
                                }
                                if (rowPets.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // Reygeko double cell at the end (double height)
                        val reygeko = Pet.REYGEKO
                        val isReygekoSelected = selectedPet == reygeko
                        val isReygekoLocked = reygeko.isLocked && !unlockedPets.contains(reygeko.id)
                        val reygekoEffectiveName = petNames[reygeko.id] ?: reygeko.defaultName
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .alpha(if (isReygekoLocked) 0.5f else 1f)
                                .border(
                                    BorderStroke(
                                        width = if (isReygekoSelected) 2.dp else 1.dp,
                                        color = if (isReygekoSelected) activeColor else Color.LightGray.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(if (isReygekoSelected) activeColor.copy(alpha = 0.05f) else Color.White)
                                .clickable {
                                    if (isReygekoLocked) {
                                        lockedPetToUnlock = reygeko
                                    } else {
                                        viewModel.selectPet(reygeko.id)
                                        isEditingPetName = false
                                    }
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier.size(110.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = reygeko.getDrawable(PetMood.FELIZ)),
                                        contentDescription = reygeko.speciesName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                    if (isReygekoSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .align(Alignment.TopEnd)
                                                .background(activeColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Seleccionada",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    if (isReygekoLocked) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .align(Alignment.TopStart)
                                                .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Bloqueada",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = reygekoEffectiveName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                            }
                        }
                    }
                }
            }

            // THEME SELECTOR SECTION
            val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clickable { isThemeExpanded = !isThemeExpanded },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.theme_section_label),
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(selectedTheme.titleRes),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .background(CreamBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedTheme.assetPath != null) {
                            val thumbRequest = remember(context, selectedTheme.assetPath) {
                                ImageRequest.Builder(context)
                                    .data("file:///android_asset/${selectedTheme.assetPath}")
                                    .size(200, 200)
                                    .crossfade(false)
                                    .build()
                            }
                            AsyncImage(
                                model = thumbRequest,
                                contentDescription = stringResource(selectedTheme.titleRes),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(16.dp).background(ColorLowMoisture, CircleShape))
                                Box(modifier = Modifier.size(16.dp).background(ColorGoodMoisture, CircleShape))
                                Box(modifier = Modifier.size(16.dp).background(ColorExcessMoisture, CircleShape))
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isThemeExpanded) {
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
                        val themes = AppTheme.entries
                        themes.chunked(2).forEach { rowThemes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowThemes.forEach { theme ->
                                    ThemeGridCell(
                                        theme = theme,
                                        selectedTheme = selectedTheme,
                                        activeColor = activeColor,
                                        modifier = Modifier.weight(1f),
                                        onThemeClick = {
                                            viewModel.selectTheme(theme.id)
                                        }
                                    )
                                }
                                if (rowThemes.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Locked Pet Unlock Dialog
        if (lockedPetToUnlock != null) {
            val pet = lockedPetToUnlock!!
            val coins by viewModel.coins.collectAsStateWithLifecycle()
            val exp by viewModel.exp.collectAsStateWithLifecycle()
            val level by viewModel.level.collectAsStateWithLifecycle()

            val (reqCoins, reqExp, reqDays) = when (pet) {
                Pet.HORMIGA -> Triple(GameConfig.LUNA_COINS, GameConfig.LUNA_EXP, 0)
                Pet.CHANCHITO -> Triple(GameConfig.TROLL_COINS, GameConfig.TROLL_EXP, 0)
                Pet.ABEJA -> Triple(GameConfig.MIEL_COINS, GameConfig.MIEL_EXP, 0)
                Pet.REYGEKO -> Triple(GameConfig.OSCAR_COINS, GameConfig.OSCAR_EXP, GameConfig.OSCAR_DAYS)
                else -> Triple(0, 0, 0)
            }

            val canAfford = when (pet) {
                Pet.HORMIGA, Pet.CHANCHITO -> coins >= reqCoins || exp >= reqExp
                Pet.ABEJA -> coins >= reqCoins && exp >= reqExp
                Pet.REYGEKO -> level >= reqDays && coins >= reqCoins && exp >= reqExp
                else -> true
            }

            AlertDialog(
                onDismissRequest = { lockedPetToUnlock = null },
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = pet.getDrawable(PetMood.FELIZ)),
                            contentDescription = pet.speciesName,
                            modifier = Modifier.size(90.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Desbloquear a ${pet.defaultName}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (pet == Pet.REYGEKO) {
                            val daysProg = (level.toFloat() / reqDays.toFloat()).coerceIn(0f, 1f)
                            Text("Días de uso: $level / $reqDays", fontSize = 12.sp, color = Color.Gray)
                            LinearProgressIndicator(
                                progress = { daysProg },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = activeColor,
                                trackColor = Color.LightGray.copy(alpha = 0.5f)
                            )
                        }

                        if (reqCoins > 0) {
                            val coinsProg = (coins.toFloat() / reqCoins.toFloat()).coerceIn(0f, 1f)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Image(painter = painterResource(id = R.drawable.coin_single), contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Monedas: $coins / $reqCoins", fontSize = 12.sp, color = Color.Gray)
                            }
                            LinearProgressIndicator(
                                progress = { coinsProg },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = activeColor,
                                trackColor = Color.LightGray.copy(alpha = 0.5f)
                            )
                        }

                        if (reqExp > 0) {
                            val expProg = (exp.toFloat() / reqExp.toFloat()).coerceIn(0f, 1f)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Image(painter = painterResource(id = R.drawable.exp_icon), contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Experiencia: $exp / $reqExp", fontSize = 12.sp, color = Color.Gray)
                            }
                            LinearProgressIndicator(
                                progress = { expProg },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = activeColor,
                                trackColor = Color.LightGray.copy(alpha = 0.5f)
                            )
                        }

                        val requirementText = when (pet) {
                            Pet.HORMIGA, Pet.CHANCHITO -> "Requiere $reqCoins monedas o $reqExp exp"
                            Pet.ABEJA -> "Requiere $reqCoins monedas y $reqExp exp"
                            Pet.REYGEKO -> "Requiere $reqDays días, $reqCoins monedas y $reqExp exp"
                            else -> ""
                        }
                        Text(text = requirementText, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DarkText)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (canAfford) {
                                val (coinsToDeduct, expToDeduct) = when (pet) {
                                    Pet.HORMIGA, Pet.CHANCHITO -> {
                                        if (coins >= reqCoins) Pair(reqCoins, 0) else Pair(0, reqExp)
                                    }
                                    Pet.ABEJA, Pet.REYGEKO -> {
                                        Pair(reqCoins, reqExp)
                                    }
                                    else -> Pair(0, 0)
                                }
                                viewModel.buyPetAtomic(pet.id, coinsToDeduct, expToDeduct) { success ->
                                    if (success) {
                                        lockedPetToUnlock = null
                                        Toast.makeText(context, "¡${pet.defaultName} desbloqueado!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Recursos insuficientes", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Recursos insuficientes", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                        enabled = canAfford
                    ) {
                        Text("Obtener", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                lockedPetToUnlock = null
                                onNavigateToMissions()
                            }
                        ) {
                            Text("Ir a Misiones", color = activeColor, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { lockedPetToUnlock = null }) {
                            Text("Cancelar", color = Color.Gray)
                        }
                    }
                }
            )
        }

        // Pet Congratulations Dialog
        if (unlockedPetForCongratulations != null) {
            val pet = unlockedPetForCongratulations!!
            val petName = petNames[pet.id] ?: pet.defaultName
            PetCongratulationsDialog(
                pet = pet,
                effectivePetName = petName,
                activeColor = activeColor,
                onEquip = {
                    viewModel.selectPet(pet.id)
                    unlockedPetForCongratulations = null
                },
                onDismiss = {
                    unlockedPetForCongratulations = null
                }
            )
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
                onSave = { _, _ -> showAddDialog = false },
                activeColor = activeColor,
                context = context,
                scope = scope,
                viewModel = viewModel
            )
        }

        // Edit Plant Dialog
        if (plantToEdit != null) {
            PlantFormDialog(
                plantToEdit = plantToEdit,
                onDismiss = { plantToEdit = null },
                onSave = { _, _ -> plantToEdit = null },
                activeColor = activeColor,
                context = context,
                scope = scope,
                viewModel = viewModel
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
            .height(135.dp)
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
            // Large Image (4:3 aspect ratio) with white background and ContentScale.Fit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = plant.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

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
    scope: CoroutineScope,
    viewModel: GardenViewModel
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
                        scope.launch(Dispatchers.IO) {
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
                                    imagePath = currentImagePath,
                                    defaultKey = null
                                )
                            }
                            val success = if (isEditing) {
                                viewModel.updatePlant(plantResult, currentImagePath)
                            } else {
                                viewModel.addPlant(plantResult)
                            }
                            withContext(Dispatchers.Main) {
                                if (success) {
                                    cleanupTempUri(tempCameraUri)
                                    cleanupTempUri(pendingUri)
                                    onSave(plantResult, currentImagePath)
                                    onDismiss()
                                } else {
                                    errorMsg = context.getString(R.string.error_duplicate_plant_name)
                                }
                            }
                        }
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

@Composable
fun ThemeGridCell(
    theme: AppTheme,
    selectedTheme: AppTheme,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onThemeClick: () -> Unit
) {
    val context = LocalContext.current
    val isSelected = selectedTheme == theme
    val themeTitle = stringResource(theme.titleRes)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) activeColor else Color.LightGray.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (isSelected) activeColor.copy(alpha = 0.05f) else Color.White)
            .clickable { onThemeClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CreamBackground),
                contentAlignment = Alignment.Center
            ) {
                if (theme.assetPath != null) {
                    val thumbRequest = remember(context, theme.assetPath) {
                        ImageRequest.Builder(context)
                            .data("file:///android_asset/${theme.assetPath}")
                            .size(180, 180)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = thumbRequest,
                        contentDescription = themeTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(18.dp).background(ColorLowMoisture, CircleShape))
                        Box(modifier = Modifier.size(18.dp).background(ColorGoodMoisture, CircleShape))
                        Box(modifier = Modifier.size(18.dp).background(ColorExcessMoisture, CircleShape))
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(activeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Seleccionado",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = themeTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
        }
    }
}

@Composable
fun PetGridCell(
    pet: Pet,
    selectedPet: Pet,
    petNames: Map<String, String>,
    unlockedPets: Set<String> = emptySet(),
    activeColor: Color,
    modifier: Modifier = Modifier,
    onPetClick: () -> Unit
) {
    val isPetSelected = selectedPet == pet
    val isPetLocked = pet.isLocked && !unlockedPets.contains(pet.id)
    val petEffectiveName = petNames[pet.id] ?: pet.defaultName
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .alpha(if (isPetLocked) 0.5f else 1f)
            .border(
                BorderStroke(
                    width = if (isPetSelected) 2.dp else 1.dp,
                    color = if (isPetSelected) activeColor else Color.LightGray.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (isPetSelected) activeColor.copy(alpha = 0.05f) else Color.White)
            .clickable { onPetClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(70.dp)
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
                if (isPetLocked) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopStart)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloqueada",
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

