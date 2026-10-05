package com.example.appjardin.ui.components

import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.isActive

@Composable
fun GlowParticleButton(
    onClick: () -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val animatorScale = remember {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        } catch (e: Exception) {
            1f
        }
    }
    val animationsEnabled = animatorScale > 0f

    val infiniteTransition = rememberInfiniteTransition(label = "GlowAnim")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )

    val particleSystem = remember { ParticleSystem(16) }
    val tickState = remember { mutableLongStateOf(0L) }
    var isResumed by remember { mutableStateOf(true) }
    var buttonSize by remember { mutableStateOf(Size.Zero) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = when (event) {
                Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START -> true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> false
                else -> isResumed
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (animationsEnabled) {
        LaunchedEffect(isResumed) {
            if (isResumed) {
                var lastTime = System.nanoTime()
                var emissionTimer = 0f
                while (isActive && isResumed) {
                    val frameTime = withInfiniteAnimationFrameNanos { it }
                    val dt = ((frameTime - lastTime) / 1e9f).coerceAtMost(0.05f)
                    lastTime = frameTime

                    emissionTimer += dt
                    if (emissionTimer >= 0.25f) {
                        emissionTimer = 0f
                        particleSystem.emit(65f, 19f, 2)
                    }

                    particleSystem.update(dt, 130f, 38f)
                    tickState.longValue = frameTime
                }
            }
        }
    }

    val starPath = remember {
        Path().apply {
            moveTo(0f, -10f)
            quadraticTo(2f, -2f, 10f, 0f)
            quadraticTo(2f, 2f, 0f, 10f)
            quadraticTo(-2f, 2f, -10f, 0f)
            quadraticTo(-2f, -2f, 0f, -10f)
            close()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Glow behind button
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = glowScale
                    scaleY = glowScale
                    alpha = if (animationsEnabled && isResumed) glowAlpha else 0.3f
                }
                .then(
                    if (Build.VERSION.SDK_INT >= 31) {
                        Modifier.blur(8.dp, BlurredEdgeTreatment.Unbounded)
                    } else {
                        Modifier
                    }
                )
                .drawBehind {
                    if (Build.VERSION.SDK_INT < 31) {
                        val brush = Brush.radialGradient(
                            colors = listOf(activeColor.copy(alpha = glowAlpha), Color.Transparent),
                            radius = size.width.coerceAtLeast(size.height)
                        )
                        drawRoundRect(brush = brush, cornerRadius = CornerRadius(size.height / 2f))
                    } else {
                        drawRoundRect(color = activeColor, cornerRadius = CornerRadius(size.height / 2f))
                    }
                }
        )

        // Button with particles drawn behind content
        Button(
            onClick = {
                if (animationsEnabled) {
                    val cx = if (buttonSize.width > 0f) buttonSize.width / 2f else 65f
                    val cy = if (buttonSize.height > 0f) buttonSize.height / 2f else 19f
                    particleSystem.emit(cx, cy, 8, isBurst = true)
                }
                onClick()
            },
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    buttonSize = androidx.compose.ui.geometry.Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
                }
                .drawBehind {
                    val currentTick = tickState.longValue
                    if (currentTick != 0L || !animationsEnabled) {
                        val pColor = activeColor
                        for (p in particleSystem.particles) {
                            if (p.active) {
                                withTransform({
                                    translate(p.x, p.y)
                                    scale(p.scale, p.scale)
                                }) {
                                    val colorToDraw = if (p.isWhite) Color.White else pColor
                                    val particleAlpha = p.alpha.coerceIn(0f, 1f)
                                    if (p.isStar) {
                                        drawPath(path = starPath, color = colorToDraw, alpha = particleAlpha)
                                    } else {
                                        drawCircle(color = colorToDraw, radius = 2.5.dp.toPx(), alpha = particleAlpha)
                                    }
                                }
                            }
                        }
                    }
                },
            contentPadding = contentPadding,
            colors = ButtonDefaults.buttonColors(containerColor = activeColor),
            shape = RoundedCornerShape(19.dp)
        ) {
            this.content()
        }
    }
}
