package com.example.appjardin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appjardin.R
import com.example.appjardin.ui.components.FullScreenArtBackground
import com.example.appjardin.ui.theme.ColorVerdeAlegre
import com.example.appjardin.viewmodel.GardenViewModel

// Fixed colors independent of light/dark system theme
private val DarkGreenWelcomeText = Color(0xFF1E3A2B)
private val BrandGreenNameText = Color(0xFF09862F)

@Composable
fun WelcomeScreen(
    viewModel: GardenViewModel,
    onConnectClick: () -> Unit
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    WelcomeScreenContent(
        userName = userName,
        onConnectClick = onConnectClick
    )
}

@Composable
fun WelcomeScreenContent(
    userName: String,
    onConnectClick: () -> Unit
) {
    val rawName = userName.trim()
    val defaultNameStr = stringResource(R.string.default_user_name)
    val welcomePrefixStr = stringResource(R.string.welcome_prefix)
    val displayName = if (rawName.isBlank()) defaultNameStr else rawName

    val greetingAnnotatedString = remember(displayName, welcomePrefixStr) {
        buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = DarkGreenWelcomeText,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp
                )
            ) {
                append(welcomePrefixStr)
            }
            withStyle(
                SpanStyle(
                    color = BrandGreenNameText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            ) {
                append(displayName)
            }
        }
    }

    FullScreenArtBackground(
        assetPath = "img/fb2.png",
        contentDescription = stringResource(R.string.welcome_bg_cd),
        aspectRatio = 841f / 1870f,
        topBoundaryPct = 0.772f,
        isLightStatusBar = false,
        isLightNavBar = true,
        topScrimAlpha = 0.40f
    ) { topSpacerHeight ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(topSpacerHeight))

            // Greeting + Button Block
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .widthIn(max = 360.dp)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = greetingAnnotatedString,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onConnectClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorVerdeAlegre,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.connect_button_text),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891, name = "Tall 411x891 (19.5:9)")
@Composable
fun WelcomeScreenPreviewTall() {
    MaterialTheme {
        WelcomeScreenContent(userName = "Fernando", onConnectClick = {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Standard 360x800 (20:9)")
@Composable
fun WelcomeScreenPreviewStandard() {
    MaterialTheme {
        WelcomeScreenContent(userName = "Carlos", onConnectClick = {})
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 873, name = "Pixel 393x873")
@Composable
fun WelcomeScreenPreviewPixel() {
    MaterialTheme {
        WelcomeScreenContent(userName = "", onConnectClick = {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640, name = "Short 360x640 (16:9)")
@Composable
fun WelcomeScreenPreviewShort() {
    MaterialTheme {
        WelcomeScreenContent(
            userName = "Guardián de las Plantas del Jardín del Rey",
            onConnectClick = {}
        )
    }
}
