package com.truvideo.sdk.camera.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.service.camera.TruvideoSdkCameraService
import java.io.File
import java.io.FileOutputStream

fun Image.save(
    path: String,
    name: String,
    extension: String,
    rotation: TruvideoSdkCameraOrientation,
    onSuccess: (File) -> Unit = { _ -> }
) {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val degrees = when (rotation) {
        TruvideoSdkCameraOrientation.PORTRAIT -> 90f
        TruvideoSdkCameraOrientation.LANDSCAPE_LEFT -> 0f
        TruvideoSdkCameraOrientation.LANDSCAPE_RIGHT -> 180f
        TruvideoSdkCameraOrientation.PORTRAIT_REVERSE -> 270f
    }
    val matrix = Matrix()
    matrix.postRotate(degrees)
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