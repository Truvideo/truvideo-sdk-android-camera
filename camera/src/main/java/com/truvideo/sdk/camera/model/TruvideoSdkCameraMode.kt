package com.truvideo.sdk.camera.model

enum class TruvideoSdkCameraMode {
    VIDEO_AND_PICTURE,
    VIDEO,
    PICTURE;

    val isVideoAndPicture: Boolean
        get() {
            return this == VIDEO_AND_PICTURE
        }

    val isVideo: Boolean
        get() {
            return this == VIDEO
        }

    val isPicture: Boolean
        get() {
            return this == PICTURE
        }

    val withVideo: Boolean
        get() {
            return this == VIDEO_AND_PICTURE || this == VIDEO
        }

    val withPicture: Boolean
        get() {
            return this == VIDEO_AND_PICTURE || this == PICTURE
        }
}