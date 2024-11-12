package com.truvideo.sdk.camera.interfaces

import truvideo.sdk.common.model.TruvideoSdkLogSeverity

internal interface TruvideoSdkCameraLogAdapter {
    fun addLog(
        eventName: String,
        message: String,
        severity: TruvideoSdkLogSeverity
    )
}