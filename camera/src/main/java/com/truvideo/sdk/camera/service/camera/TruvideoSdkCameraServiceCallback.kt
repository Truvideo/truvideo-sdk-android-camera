package com.truvideo.sdk.camera.service.camera

import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import java.io.File

internal interface TruvideoSdkCameraServiceCallback {
    fun onRecordingStarted()

    fun onTakeImageStarted()

    fun onImage(
        file: File,
        orientation: TruvideoSdkCameraOrientation,
        resolution: TruvideoSdkCameraResolution,
        lensFacing: TruvideoSdkCameraLensFacing
    )

    fun onVideo(
        file: File,
        duration: Long,
        maxDurationReached: Boolean,
        orientation: TruvideoSdkCameraOrientation,
        resolution: TruvideoSdkCameraResolution,
        lensFacing: TruvideoSdkCameraLensFacing
    )

    fun onCameraDisconnected()

    fun updateIsBusy(isBusy: Boolean)

    fun updateIsPaused(isPaused: Boolean)

    fun updateCamera(camera: TruvideoSdkCameraDevice)

    fun updateFlashMode(
        cameraLensFacing: TruvideoSdkCameraLensFacing,
        flashMode: TruvideoSdkCameraFlashMode
    )

    fun getSensorRotation(): TruvideoSdkCameraOrientation

    fun onFocusRequest()

    fun onFocusLocked()

    fun updateZoomVisibility(visible: Boolean)

    fun updateZoom(value: Float)
}