package com.example.appjardin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appjardin.R
import com.example.appjardin.model.GameConfig
import com.example.appjardin.model.MissionDef
import com.example.appjardin.model.MoistureState
import com.example.appjardin.model.Pet
import com.example.appjardin.model.PetMood
import com.example.appjardin.model.RewardType
import com.example.appjardin.ui.theme.*
import com.example.appjardin.viewmodel.GardenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsScreen(
    viewModel: GardenViewModel,
    onNavigateToSettingsPets: () -> Unit = {}
) {
    val context = LocalContext.current
    val coins by viewModel.coins.collectAsStateWithLifecycle()
    val exp by viewModel.exp.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()
    val unlockedPets by viewModel.unlockedPets.collectAsStateWithLifecycle()
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val petNames by viewModel.petNames.collectAsStateWithLifecycle()

    val diagnosticsCount by viewModel.diagnosticsCount.collectAsStateWithLifecycle()
    val manualWateringsCount by viewModel.manualWateringsCount.collectAsStateWithLifecycle()
    val thresholdEditsCount by viewModel.thresholdEditsCount.collectAsStateWithLifecycle()
    val petSelectionChangesCount by viewModel.petSelectionChangesCount.collectAsStateWithLifecycle()
    val coinsEarnedTotal by viewModel.coinsEarnedTotal.collectAsStateWithLifecycle()
    val coinsSpentPetTotal by viewModel.coinsSpentPetTotal.collectAsStateWithLifecycle()
    val chestsOpenedCount by viewModel.chestsOpenedCount.collectAsStateWithLifecycle()
    val bleConnectedOnce by viewModel.bleConnectedOnce.collectAsStateWithLifecycle()

    val manualWateredToday by viewModel.getDailyActionFlow("manual_watering").collectAsStateWithLifecycle(initialValue = false)
    val happyPlantToday by viewModel.getDailyActionFlow("happy_plant").collectAsStateWithLifecycle(initialValue = false)
    val diagnosticsToday by viewModel.getDailyActionFlow("diagnosis").collectAsStateWithLifecycle(initialValue = false)
    val adviceShownToday by viewModel.getDailyActionFlow("advice_shown").collectAsStateWithLifecycle(initialValue = false)
    val historyOpenedToday by viewModel.getDailyActionFlow("history_opened").collectAsStateWithLifecycle(initialValue = false)

    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle(initialValue = null)
    val plant by viewModel.selectedPlant.collectAsStateWithLifecycle()
    val state = viewModel.getMoistureState(telemetry?.humedad ?: 0f, plant)

    val activeColor = when (state) {
        MoistureState.LOW_MOISTURE -> ColorLowMoisture
        MoistureState.MEDIUM_MOISTURE -> ColorMediumMoisture
        MoistureState.GOOD_MOISTURE -> ColorGoodMoisture
        MoistureState.EXCESS_MOISTURE -> ColorExcessMoisture
        else -> ColorVerdeAlegre
    }

    var selectedMissionDetail by remember { mutableStateOf<MissionDef?>(null) }
    var chestRewardDialogData by remember { mutableStateOf<String?>(null) }

    val adequateSessionsCount = remember(sessions, plants) {
        sessions.count { s ->
            val p = plants.find { it.id == s.plantId }
            if (p != null) {
                val finalHum = s.humidities.split(",").lastOrNull()?.toFloatOrNull() ?: 0f
                finalHum >= p.humedadBuena && finalHum <= p.humedadExceso
            } else false
        }
    }

    val customImageCount = remember(plants) {
        plants.count { !it.imagePath.isNullOrBlank() }
    }

    val renamedPetsCount = remember(petNames) {
        petNames.count { (Pet.entries.find { p -> p.id == it.key }?.defaultName) != it.value }
    }

    val missions = remember {
        listOf(
            // Daily
            MissionDef("m1", "Regalo diario", "Reclama tu cofre diario de bienvenida.", true, 1, "Cofre Básico", RewardType.CHEST, rewardRes = R.drawable.chest_c1),
            MissionDef("m2", "Riego atento", "Realiza 1 riego manual en el jardín.", true, 1, "50 Monedas", RewardType.COINS, rewardAmount = 50, rewardRes = R.drawable.coin_stack),
            MissionDef("m3", "Planta feliz", "Mantén la humedad adecuada durante una sesión.", true, 1, "80 Monedas + 60 Exp", RewardType.BOTH, rewardAmount = 80, rewardExp = 60, rewardRes = R.drawable.exp_icon),
            MissionDef("m4", "Doctor de plantas", "Realiza 1 diagnóstico de planta.", true, 1, "100 Monedas + 80 Exp", RewardType.BOTH, rewardAmount = 100, rewardExp = 80, rewardRes = R.drawable.exp_icon),
            MissionDef("m5", "Día perfecto", "Completa las misiones 2, 3 y 4 del día.", true, 3, "Cofre del Aventurero", RewardType.CHEST, rewardRes = R.drawable.chest_c2),
            MissionDef("m6", "Revisar consejo", "Consulta el consejo de tu mascota hoy.", true, 1, "30 Monedas", RewardType.COINS, rewardAmount = 30, rewardRes = R.drawable.coin_stack),
            MissionDef("m7", "Abrir historial", "Revisa tus registros de riego en Historial.", true, 1, "40 Monedas", RewardType.COINS, rewardAmount = 40, rewardRes = R.drawable.coin_stack),
            
            // Humidity records milestones
            MissionDef("m10", "Humedad ideal I", "Alcanza 10 registros de humedad adecuada.", false, 10, "200 Monedas + 150 Exp", RewardType.BOTH, rewardAmount = 200, rewardExp = 150, rewardRes = R.drawable.exp_icon),
            MissionDef("m11", "Humedad ideal II", "Alcanza 25 registros de humedad adecuada.", false, 25, "500 Monedas + 300 Exp", RewardType.BOTH, rewardAmount = 500, rewardExp = 300, rewardRes = R.drawable.exp_icon),
            MissionDef("m12", "Humedad ideal III", "Alcanza 50 registros de humedad adecuada.", false, 50, "Cofre Dorado", RewardType.CHEST, rewardRes = R.drawable.chest_c3),
            MissionDef("m13", "Humedad ideal IV", "Alcanza 100 registros de humedad adecuada.", false, 100, "2,000 Monedas + 1,500 Exp", RewardType.BOTH, rewardAmount = 2000, rewardExp = 1500, rewardRes = R.drawable.exp_icon),
            MissionDef("m14", "Humedad ideal V", "Alcanza 200 registros de humedad adecuada.", false, 200, "Cofre Mítico", RewardType.CHEST, rewardRes = R.drawable.chest_c5),

            // Diagnostics milestones
            MissionDef("m20", "Diagnósticos I", "Completa 1 diagnóstico.", false, 1, "50 Monedas", RewardType.COINS, rewardAmount = 50, rewardRes = R.drawable.coin_stack),
            MissionDef("m21", "Diagnósticos II", "Completa 5 diagnósticos.", false, 5, "250 Monedas + 200 Exp", RewardType.BOTH, rewardAmount = 250, rewardExp = 200, rewardRes = R.drawable.exp_icon),
            MissionDef("m22", "Diagnósticos III", "Completa 10 diagnósticos.", false, 10, "Cofre Dorado", RewardType.CHEST, rewardRes = R.drawable.chest_c3),
            MissionDef("m23", "Diagnósticos IV", "Completa 25 diagnósticos.", false, 25, "1,500 Monedas + 1,000 Exp", RewardType.BOTH, rewardAmount = 1500, rewardExp = 1000, rewardRes = R.drawable.exp_icon),
            MissionDef("m24", "Diagnósticos V", "Completa 50 diagnósticos.", false, 50, "Cofre Mítico", RewardType.CHEST, rewardRes = R.drawable.chest_c5),

            // Manual watering milestones
            MissionDef("m30", "Riegos manuales I", "Ejecuta 10 riegos manuales.", false, 10, "150 Monedas", RewardType.COINS, rewardAmount = 150, rewardRes = R.drawable.coin_stack),
            MissionDef("m31", "Riegos manuales II", "Ejecuta 50 riegos manuales.", false, 50, "800 Monedas + 500 Exp", RewardType.BOTH, rewardAmount = 800, rewardExp = 500, rewardRes = R.drawable.exp_icon),
            MissionDef("m32", "Riegos manuales III", "Ejecuta 100 riegos manuales.", false, 100, "Cofre Dorado", RewardType.CHEST, rewardRes = R.drawable.chest_c3),

            // Streaks / Levels
            MissionDef("m40", "Nivel 3", "Alcanza el nivel 3.", false, 3, "100 Monedas + 100 Exp", RewardType.BOTH, rewardAmount = 100, rewardExp = 100, rewardRes = R.drawable.exp_icon),
            MissionDef("m41", "Nivel 7", "Alcanza el nivel 7.", false, 7, "300 Monedas + 250 Exp", RewardType.BOTH, rewardAmount = 300, rewardExp = 250, rewardRes = R.drawable.exp_icon),
            MissionDef("m42", "Nivel 10", "Alcanza el nivel 10.", false, 10, "Cofre Dorado", RewardType.CHEST, rewardRes = R.drawable.chest_c3),
            MissionDef("m43", "Nivel 15", "Alcanza el nivel 15.", false, 15, "1,000 Monedas + 800 Exp", RewardType.BOTH, rewardAmount = 1000, rewardExp = 800, rewardRes = R.drawable.exp_icon),
            MissionDef("m44", "Nivel 20", "Alcanza el nivel 20.", false, 20, "1,500 Monedas + 1,200 Exp", RewardType.BOTH, rewardAmount = 1500, rewardExp = 1200, rewardRes = R.drawable.exp_icon),
            MissionDef("m45", "Nivel 30", "Alcanza el nivel 30.", false, 30, "Cofre Mítico", RewardType.CHEST, rewardRes = R.drawable.chest_c5),
            MissionDef("m46", "Nivel 50", "Alcanza el nivel 50.", false, 50, "10,000 Monedas + 10,000 Exp", RewardType.BOTH, rewardAmount = 10000, rewardExp = 10000, rewardRes = R.drawable.exp_icon),
            MissionDef("m47", "Nivel 100", "Alcanza el nivel 100.", false, 100, "Cofre Mítico", RewardType.CHEST, rewardRes = R.drawable.chest_c5),

            // Plants
            MissionDef("m50", "Primeros brotes", "Añade una planta a tu jardín.", false, 1, "100 Monedas", RewardType.COINS, rewardAmount = 100, rewardRes = R.drawable.coin_stack),
            MissionDef("m51", "Imagen personal", "Añade una foto personalizada a una planta.", false, 1, "150 Monedas + 100 Exp", RewardType.BOTH, rewardAmount = 150, rewardExp = 100, rewardRes = R.drawable.exp_icon),
            MissionDef("m52", "Botanista", "Edita los umbrales de alguna planta.", false, 1, "80 Monedas", RewardType.COINS, rewardAmount = 80, rewardRes = R.drawable.coin_stack),
            MissionDef("m53", "Coleccionista", "Añade 4 plantas diferentes al jardín.", false, 4, "500 Monedas + 400 Exp", RewardType.BOTH, rewardAmount = 500, rewardExp = 400, rewardRes = R.drawable.exp_icon),

            // Pets
            MissionDef("m60", "Mi mascota", "Personaliza el nombre de cualquier mascota.", false, 1, "100 Monedas", RewardType.COINS, rewardAmount = 100, rewardRes = R.drawable.coin_stack),
            MissionDef("m61", "Cariñoso", "Cambia de mascota seleccionada 3 veces.", false, 3, "90 Monedas", RewardType.COINS, rewardAmount = 90, rewardRes = R.drawable.coin_stack),

            // Economy
            MissionDef("m70", "Ahorrador I", "Acumula 1,000 monedas.", false, 1000, "Cofre Dorado", RewardType.CHEST, rewardRes = R.drawable.chest_c3),
            MissionDef("m71", "Ahorrador II", "Acumula 5,000 monedas.", false, 5000, "2,000 Monedas + 1,500 Exp", RewardType.BOTH, rewardAmount = 2000, rewardExp = 1500, rewardRes = R.drawable.exp_icon),
            MissionDef("m72", "Ahorrador III", "Acumula 10,000 monedas.", false, 10000, "Cofre Mítico", RewardType.CHEST, rewardRes = R.drawable.chest_c5),
            MissionDef("m73", "Inversor", "Gasta monedas desbloqueando contenido.", false, 1, "100 Exp", RewardType.BOTH, rewardExp = 100, rewardRes = R.drawable.exp_icon),

            // Chests & BLE
            MissionDef("m80", "Abre cofres I", "Reclama 5 cofres en total.", false, 5, "500 Monedas + 500 Exp", RewardType.BOTH, rewardAmount = 500, rewardExp = 500, rewardRes = R.drawable.exp_icon),
            MissionDef("m81", "Abre cofres II", "Reclama 20 cofres en total.", false, 20, "Cofre Mítico", RewardType.CHEST, rewardRes = R.drawable.chest_c5),
            MissionDef("m90", "Conexión estable", "Conecta tu jardín inteligente por BLE.", false, 1, "100 Monedas + 100 Exp", RewardType.BOTH, rewardAmount = 100, rewardExp = 100, rewardRes = R.drawable.exp_icon)
        )
    }

    val reygeko = Pet.REYGEKO
    val isReygekoUnlocked = unlockedPets.contains(reygeko.id)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Misiones",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Totals Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.coin_single),
                            contentDescription = "Monedas",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "$coins",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.exp_icon),
                            contentDescription = "Experiencia",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "$exp",
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
                            text = "Nivel $level",
                            color = activeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Unlock Pet Button
            Button(
                onClick = onNavigateToSettingsPets,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = activeColor)
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Desbloquear mascota",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Reygeko Fixed Card
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = reygeko.getDrawable(PetMood.FELIZ)),
                                contentDescription = reygeko.speciesName,
                                modifier = Modifier.size(45.dp),
                                contentScale = ContentScale.Fit
                            )
                            Column {
                                Text(
                                    text = "Oscar (Reygeko)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                                Text(
                                    text = if (isReygekoUnlocked) "Desbloqueado" else "Requiere Nivel 15 + Monedas + Exp",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        if (!isReygekoUnlocked) {
                            val canUnlock = level >= GameConfig.OSCAR_DAYS && coins >= GameConfig.OSCAR_COINS && exp >= GameConfig.OSCAR_EXP
                            Button(
                                onClick = {
                                    if (canUnlock) {
                                        viewModel.buyPetAtomic(reygeko.id, GameConfig.OSCAR_COINS, GameConfig.OSCAR_EXP) { success ->
                                            if (success) {
                                                Toast.makeText(context, "¡Has desbloqueado a Oscar!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Recursos insuficientes", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Aún no cumples los requisitos", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = canUnlock,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Desbloquear", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    val daysProgress = (level.toFloat() / GameConfig.OSCAR_DAYS.toFloat()).coerceIn(0f, 1f)
                    val coinsProgress = (coins.toFloat() / GameConfig.OSCAR_COINS.toFloat()).coerceIn(0f, 1f)
                    val expProgress = (exp.toFloat() / GameConfig.OSCAR_EXP.toFloat()).coerceIn(0f, 1f)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Días de uso: $level / ${GameConfig.OSCAR_DAYS}", fontSize = 11.sp, color = Color.Gray)
                        }
                        LinearProgressIndicator(
                            progress = { daysProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = activeColor,
                            trackColor = Color.LightGray.copy(alpha = 0.5f)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Monedas: $coins / ${GameConfig.OSCAR_COINS}", fontSize = 11.sp, color = Color.Gray)
                        }
                        LinearProgressIndicator(
                            progress = { coinsProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = activeColor,
                            trackColor = Color.LightGray.copy(alpha = 0.5f)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Experiencia: $exp / ${GameConfig.OSCAR_EXP}", fontSize = 11.sp, color = Color.Gray)
                        }
                        LinearProgressIndicator(
                            progress = { expProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = activeColor,
                            trackColor = Color.LightGray.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Text(
                text = "Misiones Disponibles",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            // Missions List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = missions,
                    key = { mission -> mission.id }
                ) { mission ->
                    val isClaimed by viewModel.getMissionClaimedFlow(mission.id, mission.isDaily).collectAsStateWithLifecycle(initialValue = false)
                    
                    val m2Completed = if (manualWateredToday) 1 else 0
                    val m3Completed = if (happyPlantToday) 1 else 0
                    val m4Completed = if (diagnosticsToday) 1 else 0
                    val m5Completed = m2Completed + m3Completed + m4Completed

                    val progress = when (mission.id) {
                        "m1" -> 1f
                        "m2" -> if (manualWateredToday) 1f else 0f
                        "m3" -> if (happyPlantToday) 1f else 0f
                        "m4" -> if (diagnosticsToday) 1f else 0f
                        "m5" -> (m5Completed.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
                        "m6" -> if (adviceShownToday) 1f else 0f
                        "m7" -> if (historyOpenedToday) 1f else 0f
                        in listOf("m10", "m11", "m12", "m13", "m14") -> (adequateSessionsCount.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
                        in listOf("m20", "m21", "m22", "m23", "m24") -> (diagnosticsCount.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
                        in listOf("m30", "m31", "m32") -> (manualWateringsCount.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
                        in listOf("m40", "m41", "m42", "m43", "m44", "m45", "m46", "m47") -> (level.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
                        "m50" -> if (plants.size > GameConfig.DEFAULT_PLANTS_COUNT) 1f else 0f
                        "m53" -> ((plants.size - GameConfig.DEFAULT_PLANTS_COUNT).coerceIn(0, 4).toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
                        "m51" -> (customImageCount.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
                        "m52" -> (thresholdEditsCount.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
                        "m60" -> (renamedPetsCount.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
                        "m61" -> (petSelectionChangesCount.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
                        in listOf("m70", "m71", "m72") -> (coinsEarnedTotal.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
                        "m73" -> if (coinsSpentPetTotal > 0) 1f else 0f
                        in listOf("m80", "m81") -> (chestsOpenedCount.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
                        "m90" -> if (bleConnectedOnce) 1f else 0f
                        else -> 0f
                    }

                    MissionCard(
                        mission = mission,
                        progress = progress,
                        activeColor = activeColor,
                        isClaimed = isClaimed,
                        onClick = { selectedMissionDetail = mission },
                        onClaim = {
                            viewModel.claimMission(
                                missionId = mission.id,
                                rewardType = mission.rewardType,
                                rewardAmount = mission.rewardAmount,
                                rewardExp = mission.rewardExp,
                                isDaily = mission.isDaily
                            ) { success ->
                                if (success) {
                                    if (mission.rewardType == RewardType.CHEST) {
                                        chestRewardDialogData = "¡Cofre abierto con éxito!"
                                    } else {
                                        Toast.makeText(context, "¡Recompensa reclamada!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (chestRewardDialogData != null) {
        AlertDialog(
            onDismissRequest = { chestRewardDialogData = null },
            title = { Text("¡Cofre Abierto!", fontWeight = FontWeight.Bold) },
            text = { Text(chestRewardDialogData!!) },
            confirmButton = {
                Button(
                    onClick = { chestRewardDialogData = null },
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                ) {
                    Text("Genial", color = Color.White)
                }
            }
        )
    }

    if (selectedMissionDetail != null) {
        val mission = selectedMissionDetail!!
        ModalBottomSheet(
            onDismissRequest = { selectedMissionDetail = null },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = mission.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Text(
                    text = mission.description,
                    fontSize = 15.sp,
                    color = Color.Gray
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Text(
                    text = "Premio: ${mission.rewardTextRes}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeColor
                )
                Button(
                    onClick = { selectedMissionDetail = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                ) {
                    Text("Cerrar", color = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MissionCard(
    mission: MissionDef,
    progress: Float,
    activeColor: Color,
    isClaimed: Boolean,
    onClick: () -> Unit,
    onClaim: () -> Unit
) {
    val isCompleted = progress >= 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (isClaimed) Color.LightGray.copy(alpha = 0.3f) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = mission.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isClaimed) Color.Gray else DarkText
                )
                Text(
                    text = mission.description,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = activeColor,
                    trackColor = Color.LightGray.copy(alpha = 0.5f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = mission.rewardRes),
                    contentDescription = mission.rewardTextRes,
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isClaimed) {
                    Text(
                        text = "Reclamado",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isCompleted) {
                    Button(
                        onClick = onClaim,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Reclamar", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = mission.rewardTextRes,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
