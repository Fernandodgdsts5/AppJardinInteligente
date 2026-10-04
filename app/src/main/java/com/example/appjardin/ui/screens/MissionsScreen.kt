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
import com.example.appjardin.model.MoistureState
import com.example.appjardin.ui.theme.*
import com.example.appjardin.viewmodel.GardenViewModel
import java.text.SimpleDateFormat
import java.util.*

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

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Mission definitions
    val missions = remember {
        listOf(
            MissionDef(
                id = "m1_daily_gift",
                title = "Regalo diario",
                description = "Reclama tu cofre diario de bienvenida.",
                isDaily = true,
                target = 1,
                rewardText = "Cofre C1",
                rewardType = RewardType.CHEST,
                rewardRes = R.drawable.chest_c1
            ),
            MissionDef(
                id = "m2_water",
                title = "Riego atento",
                description = "Realiza 1 riego manual en el jardín.",
                isDaily = true,
                target = 1,
                rewardText = "50 Monedas",
                rewardType = RewardType.COINS,
                rewardAmount = 50,
                rewardRes = R.drawable.coin_stack
            ),
            MissionDef(
                id = "m3_happy_plant",
                title = "Planta feliz",
                description = "Mantén la humedad adecuada durante una sesión.",
                isDaily = true,
                target = 1,
                rewardText = "80 Monedas + 60 Exp",
                rewardType = RewardType.BOTH,
                rewardAmount = 80,
                rewardExp = 60,
                rewardRes = R.drawable.exp_icon
            ),
            MissionDef(
                id = "m4_doctor",
                title = "Doctor de plantas",
                description = "Realiza 1 diagnóstico de planta.",
                isDaily = true,
                target = 1,
                rewardText = "100 Monedas + 80 Exp",
                rewardType = RewardType.BOTH,
                rewardAmount = 100,
                rewardExp = 80,
                rewardRes = R.drawable.exp_icon
            ),
            MissionDef(
                id = "m5_perfect_day",
                title = "Día perfecto",
                description = "Completa las misiones 2, 3 y 4 del día.",
                isDaily = true,
                target = 3,
                rewardText = "Cofre C2",
                rewardType = RewardType.CHEST,
                rewardRes = R.drawable.chest_c2
            ),
            MissionDef(
                id = "m6_streak",
                title = "Racha de 5 días",
                description = "Alcanza un nivel múltiplo de 5 (días de uso).",
                isDaily = false,
                target = 5,
                rewardText = "Cofre C3",
                rewardType = RewardType.CHEST,
                rewardRes = R.drawable.chest_c3
            ),
            MissionDef(
                id = "m7_gardener",
                title = "Jardinero constante",
                description = "Consigue 50 registros adecuados y 10 diagnósticos.",
                isDaily = false,
                target = 60,
                rewardText = "Mascota Luna",
                rewardType = RewardType.PET,
                rewardRes = R.drawable.pet_hormiga_feliz
            ),
            MissionDef(
                id = "m8_photo",
                title = "Imagen personal",
                description = "Añade una foto personalizada a una de tus plantas.",
                isDaily = false,
                target = 1,
                rewardText = "150 Monedas + 100 Exp",
                rewardType = RewardType.BOTH,
                rewardAmount = 150,
                rewardExp = 100,
                rewardRes = R.drawable.exp_icon
            ),
            MissionDef(
                id = "m9_pet_name",
                title = "Mi mascota",
                description = "Personaliza el nombre de cualquier mascota en Ajustes.",
                isDaily = false,
                target = 1,
                rewardText = "100 Monedas",
                rewardType = RewardType.COINS,
                rewardAmount = 100,
                rewardRes = R.drawable.coin_stack
            ),
            MissionDef(
                id = "m10_legend",
                title = "Leyenda del jardín",
                description = "Alcanza el nivel 30 (repetible cada 30 niveles).",
                isDaily = false,
                target = 30,
                rewardText = "Cofre C5",
                rewardType = RewardType.CHEST,
                rewardRes = R.drawable.chest_c5
            )
        )
    }

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
                items(missions) { mission ->
                    MissionCard(
                        mission = mission,
                        level = level,
                        activeColor = activeColor,
                        onClick = { selectedMissionDetail = mission },
                        onClaim = {
                            when (mission.rewardType) {
                                RewardType.COINS -> {
                                    viewModel.addRewards(mission.rewardAmount, 0)
                                    viewModel.setMissionClaimed(mission.id, true)
                                    Toast.makeText(context, "¡Recompensa reclamada!", Toast.LENGTH_SHORT).show()
                                }
                                RewardType.BOTH -> {
                                    viewModel.addRewards(mission.rewardAmount, mission.rewardExp)
                                    viewModel.setMissionClaimed(mission.id, true)
                                    Toast.makeText(context, "¡Recompensa reclamada!", Toast.LENGTH_SHORT).show()
                                }
                                RewardType.CHEST -> {
                                    val rewardStr = when (mission.rewardRes) {
                                        R.drawable.chest_c1 -> {
                                            val c = (50..200).random()
                                            viewModel.addRewards(c, 0)
                                            "¡Has obtenido $c monedas!"
                                        }
                                        R.drawable.chest_c2 -> {
                                            val c = (200..500).random()
                                            val e = (400..600).random()
                                            viewModel.addRewards(c, e)
                                            "¡Has obtenido $c monedas y $e exp!"
                                        }
                                        R.drawable.chest_c3 -> {
                                            val c = (2000..5000).random()
                                            viewModel.addRewards(c, 0)
                                            "¡Has obtenido $c monedas!"
                                        }
                                        R.drawable.chest_c5 -> {
                                            val c = (10000..50000).random()
                                            val e = (10000..50000).random()
                                            viewModel.addRewards(c, e)
                                            "¡Has obtenido $c monedas y $e exp!"
                                        }
                                        else -> "¡Cofre abierto!"
                                    }
                                    viewModel.setMissionClaimed(mission.id, true)
                                    chestRewardDialogData = rewardStr
                                }
                                RewardType.PET -> {
                                    viewModel.unlockPet("hormiga")
                                    viewModel.setMissionClaimed(mission.id, true)
                                    chestRewardDialogData = "¡Has desbloqueado a la mascota Luna!"
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Chest reward dialog
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

    // Mission Detail Bottom Sheet
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
                    text = "Recompensa: ${mission.rewardText}",
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
    level: Int,
    activeColor: Color,
    onClick: () -> Unit,
    onClaim: () -> Unit
) {
    val progress = if (level >= mission.target) 1f else 0.4f
    val isCompleted = progress >= 1f
    var isClaimed by remember { mutableStateOf(false) }

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

            // Reward & Claim button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = mission.rewardRes),
                    contentDescription = mission.rewardText,
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
                        onClick = {
                            isClaimed = true
                            onClaim()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Reclamar", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = mission.rewardText,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

enum class RewardType {
    COINS, BOTH, CHEST, PET
}

data class MissionDef(
    val id: String,
    val title: String,
    val description: String,
    val isDaily: Boolean,
    val target: Int,
    val rewardText: String,
    val rewardType: RewardType,
    val rewardAmount: Int = 0,
    val rewardExp: Int = 0,
    val rewardRes: Int
)
