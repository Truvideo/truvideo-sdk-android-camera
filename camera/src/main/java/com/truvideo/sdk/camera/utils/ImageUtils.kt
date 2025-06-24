package com.truvideo.sdk.camera.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import com.truvideo.sdk.camera.service.camera.TruvideoSdkCameraService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

internal suspend fun Image.save(
    path: String,
    name: String,
    extension: String,
    rotation: Int = 0,
    flipHorizontal: Boolean = false,
    flipVertical: Boolean = false
): String = withContext(Dispatchers.IO) {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val matrix = Matrix().apply {
            if (rotation != 0) postRotate(rotation.toFloat())
//            val scaleX = if (flipHorizontal) -1f else 1f
//            val scaleY = if (flipVertical) -1f else 1f
//            postScale(scaleX, scaleY)
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        try {
            val directory = File(path)
            if (!directory.exists() && !directory.mkdirs()) {
                Log.e(TruvideoSdkCameraService.TAG, "Failed to create directory: $path")
                throw IllegalArgumentException("Failed to create directory")
            }

            val output = createFile(directory.path, name, extension)
            if (output == null) {
                Log.e(TruvideoSdkCameraService.TAG, "Failed to create file: ${directory.path}")
                throw IllegalArgumentException("Failed to create file")
            }

            FileOutputStream(output).use { outputStream ->
                when (extension) {
                    "png" -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    "jpg", "jpeg" -> bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    else -> throw IllegalArgumentException("Unsupported format: $format")
                }
            }
            Log.d(TruvideoSdkCameraService.TAG, "Image saved as PNG")
            return@withContext output.path
        } catch (exception: Exception) {
            Log.e(TruvideoSdkCameraService.TAG, "Unable to write PNG image to file", exception)
            exception.printStackTrace()
            throw exception
        }
}

internal fun Bitmap.save(
    path: String,
    name: String,
    extension: String,
    rotation: Int,
    onSuccess: (File) -> Unit = { _ -> }
) {
    val r = rotation % 360
    val matrix = Matrix().apply { postRotate(r.toFloat()) }
    val bitmap = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    try {
        val directory = File(path)
        directory.mkdirs()
        val output = createFile(directory.path, name, extension) ?: return
        FileOutputStream(output).use { outputStream ->
            when (extension) {
                "png" -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                "jpg", "jpeg" -> bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                else -> throw IllegalArgumentException("Unsupported format: $extension")
            }
        }
        Log.d(TruvideoSdkCameraService.TAG, "Image saved as PNG")
        onSuccess(output)
    } catch (exception: Exception) {
        Log.e(TruvideoSdkCameraService.TAG, "Unable to write PNG image to file", exception)
        exception.printStackTrace()
    }
}