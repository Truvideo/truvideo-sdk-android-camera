package com.truvideo.sdk.camera.model

enum class TruvideoSdkCameraFlashMode {
    OFF,
    ON;

    val isOff: Boolean
        get() {
            return this == OFF
        }

    val isOn: Boolean
        get() {
            return this == ON
        }

}
