package com.truvideo.sdk.camera.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import com.truvideo.sdk.camera.service.camera.TruvideoSdkCameraService
import java.io.File
import java.io.FileOutputStream

internal fun Image.save(
    path: String,
    name: String,
    extension: String,
    rotation: Int,
    onSuccess: (File) -> Unit = { _ -> }
) {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val matrix = Matrix()

    val r = rotation % 360
    matrix.postRotate(r.toFloat())
    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    try {
        val directory = File(path)
        directory.mkdirs()
        val output = createFile(directory.path, name, extension) ?: return
        FileOutputStream(output).use { outputStream ->
            when (extension) {
                "png" -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                "jpg", "jpeg" -> bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                else -> throw IllegalArgumentException("Unsupported format: $format")
            }
        }
        Log.d(TruvideoSdkCameraService.TAG, "Image saved as PNG")
        onSuccess(output)
    } catch (exception: Exception) {
        Log.e(TruvideoSdkCameraService.TAG, "Unable to write PNG image to file", exception)
        exception.printStackTrace()
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