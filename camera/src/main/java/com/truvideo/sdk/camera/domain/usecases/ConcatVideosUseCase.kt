package com.truvideo.sdk.camera.domain.usecases

import android.util.Log
import com.truvideo.sdk.camera.adapters.TruvideoSdkFileManager
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraFFmpegAdapter
import com.truvideo.sdk.camera.interfaces.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import truvideo.sdk.common.exceptions.TruvideoSdkException
import java.io.File
import kotlin.coroutines.suspendCoroutine

interface ConcatVideoListUseCase {
    suspend operator fun invoke(input: List<File>, output: File) : String
}

class ConcatVideoListUseCaseImpl(
    private val fileManager: TruvideoSdkFileManager,
    private val ffmpegAdapter: TruvideoSdkCameraFFmpegAdapter
) : ConcatVideoListUseCase {

    private suspend fun createTempFile(paths: List<String>): File {
        return suspendCoroutine { cont ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val path = "${fileManager.cacheDirectory}/concat_temp.txt"
                    val file = File(path)
                    if (file.exists()) file.delete()
                    file.writeText(paths.joinToString("\n") { "file $it" })
                    cont.resumeWith(Result.success(file))
                } catch (exception: Exception) {
                    exception.printStackTrace()
                    Log.d("TruvideoSdkVideo", "Error creating temp file ${exception.localizedMessage}")
                    cont.resumeWith(Result.failure(exception))
                }
            }
        }
    }

    override suspend operator fun invoke(input: List<File>, output: File): String {
        val start = System.currentTimeMillis()
        val filesToDelete = mutableListOf<String>()

        try {
            if (input.isEmpty()) throw TruvideoSdkException("Invalid input")

            val outputPath = output.path

            val tempFile = createTempFile( input.map { it.path })
            filesToDelete.add(tempFile.path)

            val command = "-y -f concat -safe 0 -i ${tempFile.path} -c copy $outputPath"

            val sessionResult = ffmpegAdapter.execute(command)
            if (!sessionResult.code.isSuccess) {
                Log.d("TruvideoSdkCamera", "Failed concatenating videos. ${sessionResult.output}")
                throw TruvideoSdkException("Unknown error")
            }

            return outputPath
        } catch (exception: Exception) {
            Log.d("TruvideoSdkCamera", "Failed to concatenate videos. ${exception.localizedMessage}")
            if (exception is TruvideoSdkException) {
                throw exception
            } else {
                throw TruvideoSdkException("Unknown error")
            }
        } finally {
            // Delete all files
            filesToDelete.forEach {
                try {
                    File(it).delete()
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }

            val end = System.currentTimeMillis()
            Log.d("TruvideoSdkCamera", "Concat request completed. Time: ${end - start}")
        }
    }
}