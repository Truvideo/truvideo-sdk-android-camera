package com.truvideo.sdk.camera.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkCameraConfiguration(
    val lensFacing: TruvideoSdkCameraLensFacing = TruvideoSdkCameraLensFacing.BACK,
    val flashMode: TruvideoSdkCameraFlashMode = TruvideoSdkCameraFlashMode.OFF,
    val orientation: TruvideoSdkCameraOrientation? = null,
    val outputPath: String = "",
    val frontResolutions: List<TruvideoSdkCameraResolution> = listOf(),
    val frontResolution: TruvideoSdkCameraResolution? = null,
    val backResolutions: List<TruvideoSdkCameraResolution> = listOf(),
    val backResolution: TruvideoSdkCameraResolution? = null,
    val mode: TruvideoSdkCameraMode = TruvideoSdkCameraMode.videoAndImage()
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraConfiguration {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}


