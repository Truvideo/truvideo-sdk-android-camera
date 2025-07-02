package com.truvideo.sdk.camera.interfaces

import com.arthenica.ffmpegkit.ReturnCode

interface TruvideoSdkCameraFFmpegAdapter {
    fun executeAsync(
        command: String,
        onRequestCreated: (id: Long) -> Unit = {},
        progressCallback: (frame: Long) -> Unit = {},
        callback: (result: ExecutionResult) -> Unit = { }
    )

    fun executeArrayAsync(
        command: Array<String>,
        onRequestCreated: (id: Long) -> Unit = {},
        progressCallback: (frame: Long) -> Unit = {},
        callback: (result: ExecutionResult) -> Unit = { }
    )

    suspend fun execute(
        command: String,
        progressCallback: (frame: Long) -> Unit = {}
    ): ExecutionResult

    suspend fun executeArray(
        command: Array<String>,
        progressCallback: (frame: Long) -> Unit = {}
    ): ExecutionResult

    suspend fun executeProbe(command: String): ExecutionResult

    suspend fun executeProbeArray(command: Array<String>): ExecutionResult

    fun cancel(executionId: Long)
}

data class ExecutionResult(
    val id: Long,
    val code: ExecutionResultCode,
    val output: String
)


enum class ExecutionResultCode {
    Success,
    Canceled,
    Error
}

val ExecutionResultCode.isSuccess: Boolean
    get() {
        return this == ExecutionResultCode.Success
    }

val ExecutionResultCode.isCanceled: Boolean
    get() {
        return this == ExecutionResultCode.Canceled
    }

val ExecutionResultCode.isError: Boolean
    get() {
        return this == ExecutionResultCode.Error
    }


internal fun ReturnCode.toExecutionResultCode(): ExecutionResultCode {
    if (this.isValueSuccess) {
        return ExecutionResultCode.Success
    }

    if (this.isValueCancel) {
        return ExecutionResultCode.Canceled
    }

    return ExecutionResultCode.Error
}