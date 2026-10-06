package com.example.appjardin.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appjardin.R
import com.example.appjardin.data.local.DiagnosisEntity
import com.example.appjardin.data.local.PlantEntity
import com.example.appjardin.data.local.SessionEntity
import com.example.appjardin.model.MoistureState
import com.example.appjardin.model.Pet
import com.example.appjardin.model.PetMood
import com.example.appjardin.model.toPetMood
import com.example.appjardin.ui.theme.*
import com.example.appjardin.viewmodel.ConnectionMode
import com.example.appjardin.viewmodel.GardenViewModel
import java.text.SimpleDateFormat
import java.util.*

private val HISTORY_CARD_HEIGHT = 260.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: GardenViewModel, onNavigateToDiagnosisDetail: (Int) -> Unit) {
    val context = LocalContext.current
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val diagnostics by viewModel.allDiagnostics.collectAsStateWithLifecycle()
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    val selectedPet by viewModel.selectedPet.collectAsStateWithLifecycle()
    val petNames by viewModel.petNames.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle(initialValue = null)
    val plant by viewModel.selectedPlant.collectAsStateWithLifecycle()

    val selectedSessionIds by viewModel.selectedSessionIds.collectAsStateWithLifecycle()
    val isSessionSelectionMode by viewModel.isSessionSelectionMode.collectAsStateWithLifecycle()

    val selectedDiagnosisIds by viewModel.selectedDiagnosisIds.collectAsStateWithLifecycle()
    val isDiagnosisSelectionMode by viewModel.isDiagnosisSelectionMode.collectAsStateWithLifecycle()

    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onHistoryEntered()
    }

    val humidity = telemetry?.humedad ?: 0f
    val state = viewModel.getMoistureState(humidity, plant)

    val activeColor = if (plant == null) {
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

    val connectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()
    val isOffline = connectionMode == ConnectionMode.OFFLINE

    var selectedTab by rememberSaveable { mutableStateOf(if (isOffline) 1 else 0) } // 0 = Humedad, 1 = Diagnóstico
    var sessionIdDetail by rememberSaveable { mutableStateOf<Int?>(null) }
    var showMultiDeleteDialog by remember { mutableStateOf(false) }

    val currentSelectionMode = if (selectedTab == 0) isSessionSelectionMode else isDiagnosisSelectionMode
    val currentSelectedCount = if (selectedTab == 0) selectedSessionIds.size else selectedDiagnosisIds.size

    // Handle system back button during selection mode
    if (currentSelectionMode) {
        BackHandler {
            if (selectedTab == 0) viewModel.clearSessionSelection()
            else viewModel.clearDiagnosisSelection()
        }
    }

    Scaffold(
        topBar = {
            if (currentSelectionMode) {
                val selectionCountText = if (currentSelectedCount == 1) {
                    stringResource(R.string.selection_count_singular)
                } else {
                    stringResource(R.string.selection_count_plural, currentSelectedCount)
                }
                TopAppBar(
                    title = {
                        Text(
                            text = selectionCountText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (selectedTab == 0) viewModel.clearSessionSelection()
                                else viewModel.clearDiagnosisSelection()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (selectedTab == 0) viewModel.selectAllSessions(sessions.map { it.id })
                                else viewModel.selectAllDiagnostics(diagnostics.map { it.id })
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = stringResource(R.string.cd_select_all),
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { showMultiDeleteDialog = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete),
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.error)
                )
            } else {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Historial",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = activeColor)
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Segmented Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(stringResource(R.string.tab_humidity), stringResource(R.string.tab_diagnosis))
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val isTabDisabled = isOffline && index == 0
                    Button(
                        onClick = {
                            if (isTabDisabled) {
                                Toast.makeText(context, context.getString(R.string.offline_humidity_tab_disabled), Toast.LENGTH_SHORT).show()
                            } else {
                                selectedTab = index
                                viewModel.clearSessionSelection()
                                viewModel.clearDiagnosisSelection()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) (if (isTabDisabled) Color.Gray else activeColor) else Color.White,
                            contentColor = if (isSelected) Color.White else (if (isTabDisabled) Color.LightGray else DarkText)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                // Humedad Tab
                if (isOffline) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BluetoothDisabled,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray
                            )
                            Text(
                                text = stringResource(R.string.offline_humidity_tab_disabled),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay sesiones de riego registradas aún.",
                            fontSize = 16.sp,
                            color = DarkText
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = sessions,
                            key = { session -> session.id }
                        ) { session ->
                            val isSelected = selectedSessionIds.contains(session.id)
                            SessionHistoryItem(
                                session = session,
                                plants = plants,
                                selectedPet = selectedPet,
                                petNames = petNames,
                                viewModel = viewModel,
                                activeColor = activeColor,
                                isSelectionMode = isSessionSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSessionSelectionMode) {
                                        viewModel.toggleSessionSelection(session.id)
                                    } else {
                                        sessionIdDetail = session.id
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSessionSelection(session.id)
                                }
                            )
                        }
                    }
                }
            } else {
                // Diagnóstico Tab
                if (diagnostics.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray
                            )
                            Text(
                                text = stringResource(R.string.empty_diagnosis),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = diagnostics,
                            key = { it.id }
                        ) { diag ->
                            val isSelected = selectedDiagnosisIds.contains(diag.id)
                            DiagnosisHistoryCard(
                                diagnosis = diag,
                                isSelectionMode = isDiagnosisSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isDiagnosisSelectionMode) {
                                        viewModel.toggleDiagnosisSelection(diag.id)
                                    } else {
                                        onNavigateToDiagnosisDetail(diag.id)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleDiagnosisSelection(diag.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Session Detail Bottom Sheet
        if (sessionIdDetail != null) {
            val session = sessions.find { it.id == sessionIdDetail }
            if (session != null) {
                SessionDetailBottomSheet(
                    session = session,
                    plants = plants,
                    selectedPet = selectedPet,
                    petNames = petNames,
                    viewModel = viewModel,
                    activeColor = activeColor,
                    activeSessionId = activeSessionId,
                    onDismiss = { sessionIdDetail = null },
                    onDeleted = {
                        sessionIdDetail = null
                        Toast.makeText(context, "Sesión eliminada", Toast.LENGTH_SHORT).show()
                    },
                    onActiveExcluded = {
                        Toast.makeText(context, context.getString(R.string.active_session_warning), Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                sessionIdDetail = null
            }
        }

        // Multi-delete Confirmation Dialog
        if (showMultiDeleteDialog) {
            val deleteTitle = if (selectedTab == 0) {
                stringResource(R.string.delete_sessions_title, selectedSessionIds.size)
            } else {
                when {
                    selectedDiagnosisIds.size == diagnostics.size -> stringResource(R.string.delete_all_diagnostics_title)
                    selectedDiagnosisIds.size == 1 -> stringResource(R.string.delete_single_diagnostic_title)
                    else -> stringResource(R.string.delete_diagnostics_title, selectedDiagnosisIds.size)
                }
            }

            val deleteText = if (selectedTab == 0) {
                "Se eliminarán los registros seleccionados permanentemente."
            } else {
                stringResource(R.string.delete_diagnostics_text)
            }

            AlertDialog(
                onDismissRequest = { showMultiDeleteDialog = false },
                title = { Text(deleteTitle, fontWeight = FontWeight.Bold) },
                text = { Text(deleteText) },
                confirmButton = {
                    Button(
                        onClick = {
                            showMultiDeleteDialog = false
                            if (selectedTab == 0) {
                                viewModel.deleteSelectedSessions(
                                    activeSessionId = activeSessionId,
                                    onActiveExcluded = {
                                        Toast.makeText(context, context.getString(R.string.active_session_warning), Toast.LENGTH_LONG).show()
                                    }
                                )
                                Toast.makeText(context, "Registros eliminados", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.deleteSelectedDiagnostics()
                                Toast.makeText(context, "Diagnósticos eliminados", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorDelete)
                    ) {
                        Text("Eliminar", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMultiDeleteDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionHistoryItem(
    session: SessionEntity,
    plants: List<PlantEntity>,
    selectedPet: Pet,
    petNames: Map<String, String>,
    viewModel: GardenViewModel,
    activeColor: Color,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val finalHumidity = remember(session.humidities) {
        val hums = session.humidities.split(",")
        hums.lastOrNull()?.toFloatOrNull() ?: 0f
    }

    val plant = remember(session.plantId, plants) {
        plants.find { it.id == session.plantId }
    }

    val moistureState = remember(finalHumidity, plant) {
        if (plant != null) {
            viewModel.getMoistureState(finalHumidity, plant)
        } else {
            MoistureState.NO_PLANT
        }
    }

    val petMood = remember(moistureState) {
        moistureState.toPetMood()
    }

    val effectivePetName = petNames[selectedPet.id] ?: selectedPet.defaultName
    val moodStr = when (petMood) {
        PetMood.TRISTE -> stringResource(R.string.pet_mood_triste)
        PetMood.NEUTRAL -> stringResource(R.string.pet_mood_neutral)
        PetMood.FELIZ -> stringResource(R.string.pet_mood_feliz)
        PetMood.ENOJADO -> stringResource(R.string.pet_mood_enojado)
        else -> stringResource(R.string.pet_mood_neutral)
    }
    val contentDesc = stringResource(R.string.pet_mood_desc, effectivePetName, moodStr)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(HISTORY_CARD_HEIGHT)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) activeColor.copy(alpha = 0.08f) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(2.dp, activeColor) else null
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left text info (weight 1f) with proper spacing and no forced vertical clipping
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSelectionMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onClick() },
                                colors = CheckboxDefaults.colors(checkedColor = activeColor)
                            )
                        }
                        Text(
                            text = session.plantName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    }
                    Surface(
                        color = activeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Sesión #${session.id}",
                            color = activeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Inicio: ${formatDate(session.startTimeMs)}", fontSize = 13.sp, color = Color.Gray)
                    Text("Fin: ${formatDate(session.endTimeMs)}", fontSize = 13.sp, color = Color.Gray)

                    val hums = session.humidities.split(",")
                    val inicial = hums.firstOrNull()?.toFloatOrNull() ?: 0f
                    val final = hums.lastOrNull()?.toFloatOrNull() ?: 0f
                    val avg = if (hums.isNotEmpty()) hums.mapNotNull { it.toFloatOrNull() }.average() else 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ini: ${String.format(Locale.getDefault(), "%.1f", inicial)}% | Fin: ${String.format(Locale.getDefault(), "%.1f", final)}%", fontSize = 12.sp, color = DarkText)
                        Text("Prom: ${String.format(Locale.getDefault(), "%.1f", avg)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = activeColor)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mín: ${String.format(Locale.getDefault(), "%.1f", session.humedadMasBaja)}%", fontSize = 11.sp, color = Color.Gray)
                        Text("Máx: ${String.format(Locale.getDefault(), "%.1f", session.humedadMasAlta)}%", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            // Right image (max 38% of card width, ContentScale.Fit)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (isSelectionMode) 0.32f else 0.38f)
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = selectedPet.getDrawable(petMood)),
                    contentDescription = contentDesc,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailBottomSheet(
    session: SessionEntity,
    plants: List<PlantEntity>,
    selectedPet: Pet,
    petNames: Map<String, String>,
    viewModel: GardenViewModel,
    activeColor: Color,
    activeSessionId: Int?,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
    onActiveExcluded: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val finalHumidity = remember(session.humidities) {
        val hums = session.humidities.split(",")
        hums.lastOrNull()?.toFloatOrNull() ?: 0f
    }

    val plant = remember(session.plantId, plants) {
        plants.find { it.id == session.plantId }
    }

    val moistureState = remember(finalHumidity, plant) {
        if (plant != null) {
            viewModel.getMoistureState(finalHumidity, plant)
        } else {
            MoistureState.NO_PLANT
        }
    }

    val petMood = remember(moistureState) {
        moistureState.toPetMood()
    }

    val consequenceText = when (moistureState) {
        MoistureState.LOW_MOISTURE -> stringResource(R.string.consequence_low)
        MoistureState.GOOD_MOISTURE -> stringResource(R.string.consequence_good)
        MoistureState.EXCESS_MOISTURE -> stringResource(R.string.consequence_excess)
        MoistureState.MEDIUM_MOISTURE -> stringResource(R.string.consequence_neutral)
        else -> stringResource(R.string.consequence_noplant)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = selectedPet.getDrawable(petMood)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxHeight(),
                    contentScale = ContentScale.Fit
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sesión #${session.id} - ${session.plantName}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
            }
            val effectivePetName = petNames[selectedPet.id] ?: selectedPet.defaultName
            Text(
                text = "Mascota compañera: $effectivePetName",
                fontSize = 13.sp,
                color = Color.Gray
            )

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Inicio: ${formatDate(session.startTimeMs)}", fontSize = 14.sp, color = DarkText)
                Text("Fin: ${formatDate(session.endTimeMs)}", fontSize = 14.sp, color = DarkText)
                Text("Humedad Mínima: ${String.format(Locale.getDefault(), "%.1f", session.humedadMasBaja)}%", fontSize = 14.sp, color = DarkText)
                Text("Humedad Máxima: ${String.format(Locale.getDefault(), "%.1f", session.humedadMasAlta)}%", fontSize = 14.sp, color = DarkText)
                Text("Humedad Final: ${String.format(Locale.getDefault(), "%.1f", finalHumidity)}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = activeColor)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = activeColor.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Consecuencia de la humedad final:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
                    Text(text = consequenceText, fontSize = 14.sp, color = DarkText)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorDelete,
                        contentColor = Color.White
                    )
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                ) {
                    Text("Cerrar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_single_session_title), fontWeight = FontWeight.Bold) },
            text = { Text("Se eliminará permanentemente esta sesión del historial.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteSingleSession(
                            sessionId = session.id,
                            activeSessionId = activeSessionId,
                            onActiveExcluded = {
                                onActiveExcluded()
                            },
                            onSuccess = {
                                onDeleted()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorDelete)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = Color.Gray.copy(alpha = 0.8f))
                }
            }
        )
    }
}

private fun formatDate(ms: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(ms))
}
