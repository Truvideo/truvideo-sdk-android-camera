package com.truvideo.sdk.camera.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkCameraMedia(
    val createdAt: Long,
    val filePath: String,
    val type: TruvideoSdkCameraMediaType,
    val cameraLensFacing: TruvideoSdkCameraLensFacing,
    val rotation: TruvideoSdkCameraOrientation,
    val resolution: TruvideoSdkCameraResolution,
    val duration: Long,
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraMedia {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }

    val fixedResolution: TruvideoSdkCameraResolution
        get() {
            return when (rotation) {
                TruvideoSdkCameraOrientation.PORTRAIT -> resolution
                TruvideoSdkCameraOrientation.LANDSCAPE_LEFT -> TruvideoSdkCameraResolution(resolution.height, resolution.width)
                TruvideoSdkCameraOrientation.LANDSCAPE_RIGHT -> TruvideoSdkCameraResolution(resolution.height, resolution.width)
                TruvideoSdkCameraOrientation.PORTRAIT_REVERSE -> resolution
            }
        }
}