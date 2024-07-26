package com.truvideo.sdk.camera.utils

import android.media.Image
import android.util.Log
import com.truvideo.sdk.camera.service.camera.TruvideoSdkCameraService
import java.io.File
import java.io.FileOutputStream

fun Image.save(
    path: String,
    name: String,
    extension: String,
    onSuccess: (File) -> Unit = { _ -> }
) {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }

    try {
        val directory = File(path)
        directory.mkdirs()
        val output = createFile(directory.path, name, extension) ?: return
        FileOutputStream(output).use { it.write(bytes) }
        Log.d(TruvideoSdkCameraService.TAG, "Image saved")
        onSuccess(output)
    } catch (exception: Exception) {
        Log.e(TruvideoSdkCameraService.TAG, "Unable to write JPEG image to file", exception)
        exception.printStackTrace()
    }
}