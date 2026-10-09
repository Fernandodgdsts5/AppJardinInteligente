package com.example.appjardin.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.appjardin.R
import com.example.appjardin.ui.minijuego.MinijuegoActivity
import com.example.appjardin.ui.theme.ColorVerdeAlegre

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun PlayScreen() {
    val context = LocalContext.current
    var isStartingGame by remember { mutableStateOf(false) }

    LaunchedEffect(isStartingGame) {
        if (isStartingGame) {
            try {
                val intent = Intent(context, MinijuegoActivity::class.java)
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback catch if Activity launch fails
            }
            isStartingGame = false
        }
    }

    val imageRequest = remember(context) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/img/fondoJuego.png")
            .crossfade(false)
            .build()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF233B2B), Color(0xFFF6F3DC))
                )
            )
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        // 1. Cover Background Image (fondoJuego.png)
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )

        // 2. "Iniciar" Button positioned in bottom free zone using fractions
        val buttonWidth = minOf(containerWidth * 0.60f, 340.dp)
        val buttonHeight = maxOf(containerHeight * 0.07f, 56.dp)
        val bottomMargin = containerHeight * 0.055f

        Button(
            onClick = {
                if (!isStartingGame) {
                    isStartingGame = true
                }
            },
            enabled = !isStartingGame,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomMargin)
                .width(buttonWidth)
                .height(buttonHeight),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorVerdeAlegre,
                contentColor = Color.White
            ),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.minijuego_btn_start),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
