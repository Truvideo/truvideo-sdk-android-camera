package com.truvideo.sdk.camera.data.mapper

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE
import android.hardware.camera2.CameraDevice.TEMPLATE_VIDEO_SNAPSHOT
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import com.truvideo.sdk.camera.domain.models.AutoFocusMode
import com.truvideo.sdk.camera.domain.models.AutoFocusTrigger
import com.truvideo.sdk.camera.domain.models.AutoWhiteBalanceMode
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import com.truvideo.sdk.camera.domain.models.CaptureTemplate
import com.truvideo.sdk.camera.domain.models.FlashMode

fun CaptureTemplate.toTemplate() = when (this) {
    CaptureTemplate.Preview -> TEMPLATE_PREVIEW
    CaptureTemplate.Record -> TEMPLATE_RECORD
    CaptureTemplate.StillCapture -> TEMPLATE_STILL_CAPTURE
    CaptureTemplate.Snapshot -> TEMPLATE_VIDEO_SNAPSHOT
}

fun FlashMode.toCaptureFlashMode() = when (this) {
    FlashMode.Off -> CaptureRequest.FLASH_MODE_OFF
    FlashMode.Single -> CaptureRequest.FLASH_MODE_SINGLE
    FlashMode.Torch -> CaptureRequest.FLASH_MODE_TORCH
}

fun AutoFocusMode.toCaptureAutoFocusMode() = when (this) {
    AutoFocusMode.Off -> CaptureRequest.CONTROL_AF_MODE_OFF
    AutoFocusMode.Auto -> CaptureRequest.CONTROL_AF_MODE_AUTO
    AutoFocusMode.Continuous -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
}

fun AutoWhiteBalanceMode.toCaptureAutoFocusMode() = when (this) {
    AutoWhiteBalanceMode.Off -> CaptureRequest.CONTROL_AWB_MODE_OFF
    AutoWhiteBalanceMode.Auto -> CaptureRequest.CONTROL_AWB_MODE_AUTO
}

fun AutoFocusTrigger.toCaptureAutoFocusTrigger() = when (this) {
    AutoFocusTrigger.Off -> CaptureRequest.CONTROL_AF_TRIGGER_IDLE
    AutoFocusTrigger.Start -> CaptureRequest.CONTROL_AF_TRIGGER_START
}

fun CameraCaptureConfig.toCaptureRequest(
    device: CameraDevice,
    surfaceList : List<Surface>,
    template: CaptureTemplate = this.template
) : CaptureRequest.Builder {
    val config = this@toCaptureRequest
    return device.createCaptureRequest(
        template.toTemplate()
    ).apply {
        surfaceList.forEach {
            addTarget(it)
        }

        // set flash
        set(
            CaptureRequest.FLASH_MODE,
            config.flash.toCaptureFlashMode()
        )

        // set zoom if available
        if (zoomArea != null) set(CaptureRequest.SCALER_CROP_REGION, zoomArea)

        // set autofocus

        set(
            CaptureRequest.CONTROL_AF_MODE,
            config.autoFocus.toCaptureAutoFocusMode()
        )

        // set awb

        set(
            CaptureRequest.CONTROL_AWB_MODE,
            config.autoWhiteBalance.toCaptureAutoFocusMode()
        )

        // set auto focus trigger
        set(
            CaptureRequest.CONTROL_AF_TRIGGER,
            config.autoFocusTrigger.toCaptureAutoFocusTrigger()
        )

        // set focus area

        if (focusArea != null)
            set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(focusArea))
    }
}