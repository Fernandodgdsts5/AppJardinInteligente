package com.example.appjardin.domain.ai

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

object ImagePreprocessor {

    fun fixOrientation(bitmap: Bitmap, inputStream: InputStream): Bitmap {
        val exif = ExifInterface(inputStream)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        val corrected = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (corrected != bitmap) {
            bitmap.recycle()
        }
        return corrected
    }

    /**
     * Resizes short side to targetSize (224) and crops center targetSize x targetSize.
     */
    fun centerCrop(bitmap: Bitmap, targetSize: Int = 224): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width == targetSize && height == targetSize) return bitmap

        val scale = targetSize.toFloat() / minOf(width, height)
        val scaledW = (width * scale).roundToInt()
        val scaledH = (height * scale).roundToInt()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)
        val x = (scaledW - targetSize) / 2
        val y = (scaledH - targetSize) / 2

        val cropped = Bitmap.createBitmap(scaledBitmap, x, y, targetSize, targetSize)
        if (scaledBitmap != bitmap && scaledBitmap != cropped) {
            scaledBitmap.recycle()
        }
        return cropped
    }

    /**
     * Converts a 224x224 ARGB_8888 bitmap to a Float ByteBuffer.
     * Supports both NCHW (planar: [1, 3, H, W]) and NHWC (interleaved: [1, H, W, 3]).
     * Values normalized to [0.0f, 1.0f].
     */
    fun toByteBuffer(
        bitmap: Bitmap,
        targetSize: Int = 224,
        channelOrder: TensorChannelOrder = TensorChannelOrder.NHWC,
        reusableBuffer: ByteBuffer? = null
    ): ByteBuffer {
        val totalPixels = targetSize * targetSize
        val floatCount = 3 * totalPixels
        val byteCount = floatCount * 4

        val buffer = if (reusableBuffer != null && reusableBuffer.capacity() >= byteCount) {
            reusableBuffer.clear()
            reusableBuffer
        } else {
            ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder())
        }

        val floatBuffer = buffer.asFloatBuffer()
        val intValues = IntArray(totalPixels)
        bitmap.getPixels(intValues, 0, targetSize, 0, 0, targetSize, targetSize)

        if (channelOrder == TensorChannelOrder.NHWC) {
            // NHWC: Interleaved RGBRGB...
            for (i in 0 until totalPixels) {
                val pixel = intValues[i]
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f

                floatBuffer.put(r)
                floatBuffer.put(g)
                floatBuffer.put(b)
            }
        } else {
            // NCHW: Planar RRR...GGG...BBB...
            val rOffset = 0
            val gOffset = totalPixels
            val bOffset = 2 * totalPixels

            for (i in 0 until totalPixels) {
                val pixel = intValues[i]
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f

                floatBuffer.put(rOffset + i, r)
                floatBuffer.put(gOffset + i, g)
                floatBuffer.put(bOffset + i, b)
            }
        }

        buffer.rewind()
        return buffer
    }
}
