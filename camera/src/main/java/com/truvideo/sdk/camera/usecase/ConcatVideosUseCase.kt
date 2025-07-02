package com.truvideo.sdk.camera.usecase

import android.content.Context
import android.util.Log
import com.truvideo.sdk.camera.interfaces.ExecutionResultCode
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraFFmpegAdapter
import com.truvideo.sdk.camera.interfaces.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import truvideo.sdk.common.exceptions.TruvideoSdkException
import java.io.File
import kotlin.coroutines.suspendCoroutine

internal class ConcatVideosUseCase(
    private val ffmpegAdapter: TruvideoSdkCameraFFmpegAdapter
) {
        private val scope = CoroutineScope(Dispatchers.IO)

        private suspend fun createTempFile(
            context: Context,
            paths: List<String>
        ): File {
            return suspendCoroutine { cont ->
                scope.launch {
                    try {
                        val path = "${context.cacheDir.path}/concat_temp.txt"
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

        suspend operator fun invoke(
            context: Context,
            input: List<File>,
            output: File
        ): String {
            val start = System.currentTimeMillis()
            val filesToDelete = mutableListOf<String>()

            try {
                if (input.isEmpty()) throw TruvideoSdkException("Invalid input")

                val outputPath = output.path

                val tempFile = createTempFile(context, input.map { it.path })
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