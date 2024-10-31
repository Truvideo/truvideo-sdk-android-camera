package com.truvideo.sdk.camera.model

enum class TruvideoSdkCameraOrientation {
    PORTRAIT,
    LANDSCAPE_LEFT,
    LANDSCAPE_RIGHT,
    PORTRAIT_REVERSE;

    val isPortrait: Boolean
        get() = this == PORTRAIT

    val isLandscapeLeft: Boolean
        get() = this == LANDSCAPE_LEFT

    val isLandscapeRight: Boolean
        get() = this == LANDSCAPE_RIGHT

    val isPortraitReverse: Boolean
        get() = this == PORTRAIT_REVERSE

    val uiRotation: Float
        get() {
            return when (this) {
                PORTRAIT -> 0f
                LANDSCAPE_LEFT -> 90f
                LANDSCAPE_RIGHT -> -90f
                PORTRAIT_REVERSE -> 180f
            }
        }

    fun getMediaRotation(cameraSensorOrientation: Int): Int {
        val cameraSensorOrientationEnum = when (cameraSensorOrientation) {
            90 -> PORTRAIT
            270 -> PORTRAIT_REVERSE
            0 -> LANDSCAPE_LEFT
            180 -> LANDSCAPE_RIGHT
            else -> PORTRAIT
        }

        val result = when (cameraSensorOrientationEnum) {
            PORTRAIT -> when (this) {
                PORTRAIT -> 90
                LANDSCAPE_LEFT -> 0
                LANDSCAPE_RIGHT -> 180
                PORTRAIT_REVERSE -> 270
            }

            PORTRAIT_REVERSE -> when (this) {
                PORTRAIT -> 270
                LANDSCAPE_LEFT -> 0
                LANDSCAPE_RIGHT -> 180
                PORTRAIT_REVERSE -> 90
            }

            else -> 0
        }

        return result
    }
}