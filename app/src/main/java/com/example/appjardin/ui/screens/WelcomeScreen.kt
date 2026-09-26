package com.example.appjardin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appjardin.R
import com.example.appjardin.ui.theme.ColorVerdeAlegre
import com.example.appjardin.ui.theme.CreamBackground
import com.example.appjardin.ui.theme.DarkText
import com.example.appjardin.viewmodel.GardenViewModel

@Composable
fun WelcomeScreen(
    viewModel: GardenViewModel,
    onConnectClick: () -> Unit
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .systemBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.planta),
                contentDescription = "Planta Jardín Inteligente",
                modifier = Modifier
                    .size(160.dp)
                    .padding(bottom = 24.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "Bienvenido",
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = DarkText,
                textAlign = TextAlign.Center
            )

            Text(
                text = userName.ifBlank { "Guardián de las Plantas" },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ColorVerdeAlegre,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Text(
                text = "Supervisa y cuida tus plantas en tiempo real con la tecnología de riego inteligente.",
                fontSize = 16.sp,
                color = DarkText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onConnectClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorVerdeAlegre)
            ) {
                Text(
                    text = "CONECTAR JARDÍN INTELIGENTE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
