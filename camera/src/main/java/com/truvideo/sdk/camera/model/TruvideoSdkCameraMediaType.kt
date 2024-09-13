package com.truvideo.sdk.camera.model

enum class TruvideoSdkCameraMediaType {
    VIDEO,
    PICTURE;

    val isVideo: Boolean
        get() = this == VIDEO

    val isPicture: Boolean
        get() = this == PICTURE
}