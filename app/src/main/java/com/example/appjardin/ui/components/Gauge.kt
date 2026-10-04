package com.example.appjardin.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appjardin.ui.theme.DarkText

@Composable
fun CircularGauge(
    percentage: Float,
    stateColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedPercentage by animateFloatAsState(targetValue = percentage, label = "ProgressAnimation")
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val strokeWidth = 14.dp.toPx()
            val startAngle = 135f
            val sweepAngle = 270f
            
            // Background arc in light gray
            drawArc(
                color = Color(0xFFE0E0E0),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
            
            // Foreground active arc
            val progressSweep = (animatedPercentage.coerceIn(0f, 100f) / 100f) * sweepAngle
            drawArc(
                color = stateColor,
                startAngle = startAngle,
                sweepAngle = progressSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
        }
        
        // Percentage centered inside semicircle
        Text(
            text = "${animatedPercentage.toInt()}%",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
