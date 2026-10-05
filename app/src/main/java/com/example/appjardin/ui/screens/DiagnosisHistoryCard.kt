package com.example.appjardin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.appjardin.R
import com.example.appjardin.data.local.DiagnosisEntity
import com.example.appjardin.ui.theme.ColorExcessMoisture
import com.example.appjardin.ui.theme.ColorGoodMoisture
import com.example.appjardin.ui.theme.DarkText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiagnosisHistoryCard(
    diagnosis: DiagnosisEntity,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(diagnosis.createdAt))
    val isHealthy = diagnosis.result == "HEALTHY"
    val resultColor = if (isHealthy) ColorGoodMoisture else ColorExcessMoisture
    val resultTitle = if (isHealthy) stringResource(R.string.diagnosis_healthy_title_short) else stringResource(R.string.diagnosis_unhealthy_title_short)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.LightGray.copy(alpha = 0.3f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.error) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Diagnóstico de ${diagnosis.plantName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = resultTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = resultColor
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.45f)
            ) {
                AsyncImage(
                    model = File(diagnosis.imagePath),
                    contentDescription = "Foto de diagnóstico",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
