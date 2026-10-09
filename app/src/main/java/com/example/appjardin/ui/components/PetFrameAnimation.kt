package com.example.appjardin.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.appjardin.model.PetMood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale

private const val TAG = "PetFrameAnimation"
private const val FRAME_DURATION_NANOS = 83_333_333L // ~12 FPS (83.33ms)
private const val TOTAL_FRAMES = 120

fun getFolderForPetMood(mood: PetMood): String {
    return when (mood) {
        PetMood.FELIZ -> "lf"
        PetMood.NEUTRAL -> "ln"
        PetMood.TRISTE, PetMood.ASUSTADO -> "lt"
        PetMood.ENOJADO -> "le"
    }
}

fun calculateFrameIndex(
    elapsedNanos: Long,
    frameDurationNanos: Long = FRAME_DURATION_NANOS,
    totalFrames: Int = TOTAL_FRAMES
): Int {
    if (frameDurationNanos <= 0L || totalFrames <= 0) return 1
    val frameNum = ((elapsedNanos / frameDurationNanos) % totalFrames) + 1
    return frameNum.toInt().coerceIn(1, totalFrames)
}

fun calculateContainDstSizeAndOffset(
    srcWidth: Int,
    srcHeight: Int,
    dstMaxWidth: Float,
    dstMaxHeight: Float
): Pair<IntSize, IntOffset> {
    if (srcWidth <= 0 || srcHeight <= 0 || dstMaxWidth <= 0f || dstMaxHeight <= 0f) {
        return Pair(IntSize.Zero, IntOffset.Zero)
    }

    val srcRatio = srcWidth.toFloat() / srcHeight.toFloat()
    val dstRatio = dstMaxWidth / dstMaxHeight

    val scale = if (dstRatio > srcRatio) {
        dstMaxHeight / srcHeight.toFloat()
    } else {
        dstMaxWidth / srcWidth.toFloat()
    }

    val drawWidth = (srcWidth * scale).toInt()
    val drawHeight = (srcHeight * scale).toInt()

    val offsetX = ((dstMaxWidth - drawWidth) / 2f).toInt()
    val offsetY = ((dstMaxHeight - drawHeight) / 2f).toInt()

    return Pair(
        IntSize(drawWidth, drawHeight),
        IntOffset(offsetX, offsetY)
    )
}

@Composable
fun PetFrameAnimation(
    folderName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isAnimationDisabled = remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }

    var currentFrameImage by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(folderName, isAnimationDisabled, lifecycleOwner) {
        if (isAnimationDisabled) {
            withContext(Dispatchers.IO) {
                val path = "larva/$folderName/001.png"
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = 2
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val img = try {
                    context.assets.open(path).use { stream ->
                        BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error decoding static frame asset: $path", e)
                    null
                }
                if (isActive && img != null) {
                    currentFrameImage = img
                }
            }
            return@LaunchedEffect
        }

        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.IO) {
                val startNanos = System.nanoTime()

                while (isActive) {
                    val elapsedNanos = System.nanoTime() - startNanos
                    val frameIndex = calculateFrameIndex(elapsedNanos)
                    val fileName = String.format(Locale.US, "%03d.png", frameIndex)
                    val assetPath = "larva/$folderName/$fileName"

                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 2
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }

                    val decodedImage: ImageBitmap? = try {
                        context.assets.open(assetPath).use { stream ->
                            BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error decoding asset: $assetPath", e)
                        null
                    }

                    if (isActive && decodedImage != null) {
                        currentFrameImage = decodedImage
                    }

                    val nextFrameNanos = (elapsedNanos / FRAME_DURATION_NANOS + 1) * FRAME_DURATION_NANOS
                    val sleepNanos = nextFrameNanos - (System.nanoTime() - startNanos)
                    if (sleepNanos > 0) {
                        val sleepMs = sleepNanos / 1_000_000L
                        delay(sleepMs)
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val img = currentFrameImage
        if (img != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val (dstSize, dstOffset) = calculateContainDstSizeAndOffset(
                    srcWidth = img.width,
                    srcHeight = img.height,
                    dstMaxWidth = size.width,
                    dstMaxHeight = size.height
                )

                drawImage(
                    image = img,
                    dstOffset = dstOffset,
                    dstSize = dstSize
                )
            }
        }
    }
}
