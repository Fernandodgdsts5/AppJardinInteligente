package com.example.appjardin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appjardin.data.local.PlantEntity
import com.example.appjardin.model.MoistureState
import com.example.appjardin.ui.theme.*
import com.example.appjardin.viewmodel.GardenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: GardenViewModel) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    val selectedPlant by viewModel.selectedPlant.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()

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

    var dropdownExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

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

            // PLANT SELECTION DROPDOWN SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Planta",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPlant?.name ?: "Selecciona una planta",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeColor,
                                unfocusedBorderColor = Color.LightGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            plants.forEach { plantItem ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = plantItem.name,
                                                fontWeight = if (selectedPlant?.id == plantItem.id) FontWeight.Bold else FontWeight.Normal,
                                                color = DarkText
                                            )
                                            Text(
                                                text = "Min: ${plantItem.humedadMinima}% | Opt: ${plantItem.humedadBuena}% | Exceso: > ${plantItem.humedadExceso}%",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectPlant(plantItem.id)
                                        dropdownExpanded = false
                                    },
                                    contentPadding = PaddingValues(12.dp)
                                )
                            }
                        }
                    }

                    // Display details of current selected plant
                    selectedPlant?.let { p ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = activeColor.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                Text("Parámetros de ${p.name}:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)
                                Text("• Humedad Mínima (humedadMinima): ${p.humedadMinima}%", fontSize = 12.sp, color = DarkText)
                                Text("• Humedad Buena (humedadBuena): ${p.humedadBuena}%", fontSize = 12.sp, color = DarkText)
                                Text("• Humedad Exceso (humedadExceso): ${p.humedadExceso}%", fontSize = 12.sp, color = DarkText)
                            }
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
        }

        if (showAddDialog) {
            AddPlantDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { newPlant ->
                    viewModel.addPlant(newPlant)
                    showAddDialog = false
                },
                activeColor = activeColor
            )
        }
    }
}

@Composable
fun AddPlantDialog(
    onDismiss: () -> Unit,
    onAdd: (PlantEntity) -> Unit,
    activeColor: Color
) {
    var name by remember { mutableStateOf("") }
    var minStr by remember { mutableStateOf("") }
    var buenaStr by remember { mutableStateOf("") }
    var excesoStr by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Planta Personalizada", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la Planta") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = minStr,
                    onValueChange = { minStr = it },
                    label = { Text("Humedad Mínima (%) [humedadMinima]") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = buenaStr,
                    onValueChange = { buenaStr = it },
                    label = { Text("Humedad Buena (%) [humedadBuena]") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = excesoStr,
                    onValueChange = { excesoStr = it },
                    label = { Text("Humedad Exceso (%) [humedadExceso]") },
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
                        errorMsg = "Regla requerida: humedadMinima < humedadBuena < humedadExceso"
                    } else {
                        onAdd(
                            PlantEntity(
                                name = name,
                                humedadMinima = min,
                                humedadBuena = buena,
                                humedadExceso = exceso
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
}
