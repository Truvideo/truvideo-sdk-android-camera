package com.truvideo.sdk.camera.service.ar

import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import java.io.File

internal interface TruvideoSdkArCameraServiceCallback {
    fun getSensorRotation(): TruvideoSdkCameraOrientation

    fun updateIsRecording(isRecording: Boolean)

    fun updateIsBusy(isBusy: Boolean)

    fun updateIsPaused(isPaused: Boolean)

    fun onCameraDisconnected()

    fun onVideo(
        file: File,
        duration: Long,
        orientation: TruvideoSdkCameraOrientation,
        resolution: TruvideoSdkCameraResolution,
        maxVideoDurationReached: Boolean
    )

    fun onImage(
        file: File,
        orientation: TruvideoSdkCameraOrientation,
        resolution: TruvideoSdkCameraResolution
    )

    fun onRecordingStarted()
}