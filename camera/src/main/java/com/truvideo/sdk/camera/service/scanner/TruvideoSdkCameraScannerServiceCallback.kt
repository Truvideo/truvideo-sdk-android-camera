package com.truvideo.sdk.camera.service.scanner

import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerCode

interface TruvideoSdkCameraScannerCallback {
    fun onCodeScanned(code: TruvideoSdkCameraScannerCode)
    fun onCameraDisconnected()
    fun updateResolution(resolution: TruvideoSdkCameraResolution)
    fun updateIsBusy(isBusy: Boolean)
    fun updateCamera(camera: TruvideoSdkCameraDevice)
    fun getSensorRotation(): TruvideoSdkCameraOrientation
    fun updateFlashMode(flashMode: TruvideoSdkCameraFlashMode)
}