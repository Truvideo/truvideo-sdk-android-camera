package com.truvideo.sdk.camera.model

enum class TruvideoSdkCameraLensFacing {
    BACK,
    FRONT;

    val isBack: Boolean
        get() {
            return this == BACK
        }

    val isFront: Boolean
        get() {
            return this == FRONT
        }

    val reversed : TruvideoSdkCameraLensFacing
        get() =
            if (this.isFront) TruvideoSdkCameraLensFacing.BACK
            else TruvideoSdkCameraLensFacing.FRONT


}
