package com.truvideo.sdk.camera.model

import kotlinx.serialization.Serializable

@Serializable
enum class TruvideoSdkCameraEventType {
    RECORDING_STARTED,
    RECORDING_FINISHED,
    RECORDING_PAUSED,
    RECORDING_RESUMED,
    IMAGE_TAKEN,
    CAMERA_DEVICE_CHANGED,
    RESOLUTION_CHANGED,
    FLASH_MODE_CHANGED,
    ZOOM_CHANGED,
    MEDIA_DELETED,
    MEDIA_DISCARDED,
    CONTINUE
}


