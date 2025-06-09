package com.truvideo.sdk.camera.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class TruvideoSdkCameraInformation(
    val frontCamera: TruvideoSdkCameraDevice? = null,
    val backCamera: TruvideoSdkCameraDevice? = null,
) {
    private val cameraCount: Int
        get() {
            var result = 0
            if (frontCamera != null) {
                result += 1
            }

            if (backCamera != null) {
                result += 1
            }

            return result
        }

    val canFlipCamera: Boolean
        get() = cameraCount >= 2

    val withCameras: Boolean
        get() = cameraCount > 0

    fun getDeviceFromFacing(facing: TruvideoSdkCameraLensFacing): TruvideoSdkCameraDevice? =
        when (facing) {
            TruvideoSdkCameraLensFacing.BACK -> this.backCamera
            TruvideoSdkCameraLensFacing.FRONT -> this.frontCamera
        }


    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraInformation {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}