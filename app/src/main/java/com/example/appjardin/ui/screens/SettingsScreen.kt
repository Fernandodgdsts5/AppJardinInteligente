package com.example.appjardin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.appjardin.data.local.PlantEntity
import com.example.appjardin.viewmodel.GardenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: GardenViewModel) {
    val userName by viewModel.userName.collectAsState()
    val plants by viewModel.allPlants.collectAsState()
    val selectedPlant by viewModel.selectedPlant.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Planta")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            var nameInput by remember { mutableStateOf(userName) }
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Nombre de Usuario") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.saveUserName(nameInput) },
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
            ) {
                Text("Guardar Nombre")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Selecciona una Planta", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(plants) { plant ->
                    val isSelected = selectedPlant?.id == plant.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectPlant(plant.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(plant.name, style = MaterialTheme.typography.titleMedium)
                                Text("Riego: ${plant.inicioRiego}% - ${plant.finRiego}%", style = MaterialTheme.typography.bodySmall)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Seleccionada")
                            }
                        }
                    }
                }
            }
        }
        
        if (showAddDialog) {
            AddPlantDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { p -> 
                    viewModel.addPlant(p)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddPlantDialog(onDismiss: () -> Unit, onAdd: (PlantEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var minStr by remember { mutableStateOf("") }
    var maxStr by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Planta") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
                OutlinedTextField(value = minStr, onValueChange = { minStr = it }, label = { Text("Humedad Mínima (%)") })
                OutlinedTextField(value = maxStr, onValueChange = { maxStr = it }, label = { Text("Humedad Máxima (%)") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val min = minStr.toIntOrNull() ?: 50
                val max = maxStr.toIntOrNull() ?: 70
                onAdd(PlantEntity(name = name, inicioRiego = min, finRiego = max, recomendadaMax = max + 10, exceso = max + 20))
            }) { Text("Añadir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
