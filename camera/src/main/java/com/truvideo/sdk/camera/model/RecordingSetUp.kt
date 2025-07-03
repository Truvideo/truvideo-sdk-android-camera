package com.truvideo.sdk.camera.model

data class RecordingSetUp(
    val videoPath: String,
    val lensFacing: TruvideoSdkCameraLensFacing,
    val orientation: TruvideoSdkCameraOrientation,
    val rotationAngle: Int,
    val resolution: TruvideoSdkCameraResolution,
    val maxDuration: Int?
)