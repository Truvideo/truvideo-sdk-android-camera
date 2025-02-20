package com.truvideo.sdk.camera.utils

import android.util.Log
import com.truvideo.sdk.camera.service.camera.TruvideoSdkCameraService
import java.io.File

internal fun createFile(path: String, name: String, extension: String): File? =
    try {
        val directory = File(path)
        directory.mkdirs()
        File(directory.path, "$name.$extension")
    } catch (exception: Exception) {
        Log.d(TruvideoSdkCameraService.TAG, "Error creating video file", exception)
        exception.printStackTrace()
        null
    }

internal fun deleteFile(file: File, onSuccess: () -> Unit = { }) {
    try {
        file.delete()
        Log.d(TruvideoSdkCameraService.TAG, "File ${file.path} deleted")
        onSuccess()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
