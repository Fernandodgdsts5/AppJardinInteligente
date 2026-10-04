package com.example.appjardin.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.exifinterface.media.ExifInterface
import com.example.appjardin.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PlantImageStorage {

    private const val TAG = "PlantImage"

    @DrawableRes
    fun getDefaultDrawableRes(defaultKey: String?): Int? {
        return when (defaultKey?.lowercase()) {
            "tomate" -> R.drawable.tomate
            "geranio" -> R.drawable.geranio
            "rosa" -> R.drawable.rosa
            "helecho" -> R.drawable.helecho
            else -> null
        }
    }

    suspend fun saveImageToInternalStorage(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting image processing for uri: $sourceUri")
            val startTime = System.currentTimeMillis()

            val originalBitmap = decodeSampledBitmapFromUri(context, sourceUri, 1024)
            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from uri: $sourceUri")
                return@withContext null
            }

            // Handle EXIF orientation if applicable
            val rotatedBitmap = handleExifOrientation(context, sourceUri, originalBitmap)

            // Crop to square (center crop) and resize to max 1024px
            val processedBitmap = cropToSquareAndResize(rotatedBitmap, 1024)

            // Create plant_images directory in internal storage
            val directory = File(context.filesDir, "plant_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val fileName = "plant_${UUID.randomUUID()}.jpg"
            val file = File(directory, fileName)

            val outputStream = FileOutputStream(file)
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.flush()
            outputStream.close()

            // Clean up bitmaps
            if (originalBitmap != rotatedBitmap) originalBitmap.recycle()
            if (rotatedBitmap != processedBitmap) rotatedBitmap.recycle()
            processedBitmap.recycle()

            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Image successfully saved to ${file.absolutePath} in ${duration}ms")

            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image from uri: $sourceUri", e)
            null
        }
    }

    fun deleteImageFile(imagePath: String?) {
        if (imagePath.isNullOrBlank()) return
        try {
            val file = File(imagePath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Deleted old image file: $imagePath (success=$deleted)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image file: $imagePath", e)
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqSize: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqSize, reqSize)
            options.inJustDecodeBounds = false

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding sampled bitmap from uri: $uri", e)
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun handleExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling EXIF orientation for uri: $uri", e)
            bitmap
        }
    }

    private fun cropToSquareAndResize(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val dimension = minOf(width, height)
        val x = (width - dimension) / 2
        val y = (height - dimension) / 2

        val squareBitmap = Bitmap.createBitmap(bitmap, x, y, dimension, dimension)

        if (dimension <= maxSize) {
            return squareBitmap
        }

        return Bitmap.createScaledBitmap(squareBitmap, maxSize, maxSize, true).also {
            if (it != squareBitmap) {
                squareBitmap.recycle()
            }
        }
    }
}
