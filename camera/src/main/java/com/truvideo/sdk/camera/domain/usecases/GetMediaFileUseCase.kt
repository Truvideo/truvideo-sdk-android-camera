package com.truvideo.sdk.camera.domain.usecases

import android.util.Log
import com.truvideo.sdk.camera.adapters.TruvideoSdkFileManager
import com.truvideo.sdk.camera.model.TruvideoSdkDirectory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface GetMediaFileUseCase {
    operator fun invoke(
        directory: TruvideoSdkDirectory = TruvideoSdkDirectory.Files(),
        fileName: String? = null,
        extension: String = ""
    ): File?
}

class GetMediaFileUseCaseImpl : GetMediaFileUseCase {

    companion object {
        const val TAG = "GetMediaFileUseCase"
    }

    override fun invoke(
        directory: TruvideoSdkDirectory,
        fileName: String?,
        extension: String
    ): File? {

        val directory = directory.path()
        val name = fileName ?:
            SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US).format(Date())

        return try {
            val directory = File(directory)
            directory.mkdirs()
            File(
                directory.path,
                if (extension.isEmpty())
                    "$name"
                else
                    "$name.$extension"
            )
        } catch (exception: Exception) {
            Log.d(TAG, "Error creating video file", exception)
            exception.printStackTrace()
            null
        }
    }

}