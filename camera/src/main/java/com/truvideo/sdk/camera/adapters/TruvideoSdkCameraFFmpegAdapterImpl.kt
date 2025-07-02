package com.truvideo.sdk.camera.adapters

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.truvideo.sdk.camera.interfaces.ExecutionResult
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraFFmpegAdapter
import com.truvideo.sdk.camera.interfaces.toExecutionResultCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.suspendCoroutine


class TruvideoSdkCameraFFmpegAdapterImpl : TruvideoSdkCameraFFmpegAdapter {

    companion object {
        const val TAG = "FFmpegAdapterImpl"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private suspend fun rawExecuteProbe(command: String): ExecutionResult {
        return suspendCoroutine { cont ->
            FFprobeKit.executeAsync(
                command,
            ) { session ->
                if (session == null) return@executeAsync

                val result = ExecutionResult(
                    id = session.sessionId,
                    code = session.returnCode.toExecutionResultCode(),
                    output = session.output
                )
                cont.resumeWith(Result.success(result))
            }
        }
    }

    private suspend fun rawExecuteProbeArray(command: Array<String>): ExecutionResult {
        return suspendCoroutine { cont ->
            FFprobeKit.executeWithArgumentsAsync(
                command,
            ) { session ->
                if (session == null) return@executeWithArgumentsAsync

                val result = ExecutionResult(
                    id = session.sessionId,
                    code = session.returnCode.toExecutionResultCode(),
                    output = session.output
                )
                cont.resumeWith(Result.success(result))
            }
        }
    }

    private suspend fun rawExecute(
        command: String,
        progressCallback: (progress: Long) -> Unit = {},
        onRequestCreated: (id: Long) -> Unit = {}
    ): ExecutionResult {
        return suspendCoroutine { cont ->
            val session = FFmpegKit.executeAsync(
                command,
                { session ->
                    if (session == null) return@executeAsync

                    val result = ExecutionResult(
                        id = session.sessionId,
                        code = session.returnCode.toExecutionResultCode(),
                        output = session.output
                    )
                    cont.resumeWith(Result.success(result))
                },
                { _ -> },
                { statistics ->
                    val time = statistics.time.toLong()
                    progressCallback(time)
                }
            )
            onRequestCreated(session.sessionId)
        }
    }

    private suspend fun rawExecuteArray(
        command: Array<String>,
        progressCallback: (progress: Long) -> Unit = {},
        onRequestCreated: (id: Long) -> Unit = {}
    ): ExecutionResult {
        return suspendCoroutine { cont ->
            val session = FFmpegKit.executeWithArgumentsAsync(
                command,
                { session ->
                    if (session == null) return@executeWithArgumentsAsync

                    val result = ExecutionResult(
                        id = session.sessionId,
                        code = session.returnCode.toExecutionResultCode(),
                        output = session.output
                    )
                    cont.resumeWith(Result.success(result))
                },
                { _ -> },
                { statistics ->
                    val time = statistics.time.toLong()
                    progressCallback(time)
//                    Log.d("TruvideoSdkVideo", "Statistics: $statistics")
                }
            )
            onRequestCreated(session.sessionId)
        }
    }

    override fun executeAsync(
        command: String,
        onRequestCreated: (id: Long) -> Unit,
        progressCallback: (progress: Long) -> Unit,
        callback: (result: ExecutionResult) -> Unit
    ) {
        scope.launch {
            val result = rawExecute(
                command,
                progressCallback = progressCallback,
                onRequestCreated = onRequestCreated,
            )

            callback(result)
        }
    }

    override fun executeArrayAsync(
        command: Array<String>,
        onRequestCreated: (id: Long) -> Unit,
        progressCallback: (progress: Long) -> Unit,
        callback: (result: ExecutionResult) -> Unit
    ) {
        scope.launch {
            val result = rawExecuteArray(
                command,
                progressCallback = progressCallback,
                onRequestCreated = onRequestCreated,
            )
            callback(result)
        }
    }

    override suspend fun execute(
        command: String,
        progressCallback: (progress: Long) -> Unit
    ) = rawExecute(
        command = command,
        progressCallback = progressCallback
    )

    override suspend fun executeArray(
        command: Array<String>,
        progressCallback: (progress: Long) -> Unit
    ) = rawExecuteArray(
        command = command,
        progressCallback = progressCallback
    )

    override suspend fun executeProbe(command: String) = rawExecuteProbe(command)

    override suspend fun executeProbeArray(command: Array<String>) = rawExecuteProbeArray(command)

    override fun cancel(executionId: Long) {
        try {
            FFmpegKit.cancel(executionId)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }
}