package com.truvideo.sdk.camera.model

enum class TruvideoSdkCameraMediaType {
    VIDEO,
    IMAGE;

    val isVideo: Boolean
        get() = this == VIDEO

    val isImage: Boolean
        get() = this == IMAGE
}