package com.truvideo.sdk.camera.service.scanner

import com.google.mlkit.vision.barcode.common.Barcode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation

interface TruvideoSdkScannerCallback {
    fun onBarcodeScanned(barcode: Barcode?)
    fun onCameraDisconnected()
    fun updateIsBusy(isBusy: Boolean)
    fun updateCamera(camera: TruvideoSdkCameraDevice)
    fun getSensorRotation(): TruvideoSdkCameraOrientation
    fun updateFlashMode(flashMode: TruvideoSdkCameraFlashMode)
}