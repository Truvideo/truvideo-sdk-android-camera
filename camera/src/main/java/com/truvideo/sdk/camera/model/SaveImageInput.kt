package com.truvideo.sdk.camera.model

import android.media.Image

data class SaveImageInput(
    val image: Image,
    val name: String? = null,
    val outputPath: String,
    val rotation: Int,
    val format: TruvideoSdkCameraImageFormat
)
