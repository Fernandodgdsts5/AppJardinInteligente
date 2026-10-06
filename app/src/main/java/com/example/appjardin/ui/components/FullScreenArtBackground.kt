package com.example.appjardin.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest

private val StatusScrimColor = Color(0xFF1B3123)

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun FullScreenArtBackground(
    assetPath: String,
    contentDescription: String?,
    aspectRatio: Float,
    topBoundaryPct: Float,
    isLightStatusBar: Boolean = false,
    isLightNavBar: Boolean = false,
    topScrimAlpha: Float = 0.40f,
    bottomScrimAlpha: Float = 0f,
    imageAlignment: Alignment = Alignment.TopCenter,
    fallbackTopColor: Color = Color(0xFF233B2B),
    fallbackBottomColor: Color = Color(0xFFF6F3DC),
    content: @Composable (topSpacerHeight: Dp) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Apply transparent system bar icon appearance for this screen and restore on exit
    if (!view.isInEditMode) {
        DisposableEffect(view) {
            val window = (view.context as? Activity)?.window
            val insetsController = if (window != null) WindowCompat.getInsetsController(window, view) else null

            val previousLightStatus = insetsController?.isAppearanceLightStatusBars ?: false
            val previousLightNav = insetsController?.isAppearanceLightNavigationBars ?: false

            insetsController?.isAppearanceLightStatusBars = isLightStatusBar
            insetsController?.isAppearanceLightNavigationBars = isLightNavBar

            onDispose {
                insetsController?.isAppearanceLightStatusBars = previousLightStatus
                insetsController?.isAppearanceLightNavigationBars = previousLightNav
            }
        }
    }

    val imageRequest = remember(context, assetPath) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/$assetPath")
            .crossfade(false)
            .build()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(fallbackTopColor, fallbackBottomColor)
                )
            )
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val screenRatio = if (screenHeight > 0.dp) screenWidth / screenHeight else aspectRatio

        val scaledImgHeight = if (screenRatio > aspectRatio) {
            screenWidth / aspectRatio
        } else {
            screenHeight
        }

        val topCropOffset = if (screenRatio > aspectRatio) {
            -(scaledImgHeight - screenHeight) / 2f
        } else {
            0.dp
        }

        val boundaryDp = topCropOffset + (scaledImgHeight * topBoundaryPct)
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val topSpacerHeight = maxOf(statusBarPadding + 16.dp, boundaryDp + 12.dp)

        // 1. Background Artwork Edge-to-Edge
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = imageAlignment
        )

        // 2. Top Scrim for Status Bar contrast if needed
        if (topScrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarPadding + 28.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                StatusScrimColor.copy(alpha = topScrimAlpha),
                                StatusScrimColor.copy(alpha = topScrimAlpha * 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // 3. Bottom Scrim for Navigation Bar contrast if needed
        if (bottomScrimAlpha > 0f) {
            val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navBarPadding + 28.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                StatusScrimColor.copy(alpha = bottomScrimAlpha * 0.5f),
                                StatusScrimColor.copy(alpha = bottomScrimAlpha)
                            )
                        )
                    )
            )
        }

        // 4. Content Layer
        content(topSpacerHeight)
    }
}
