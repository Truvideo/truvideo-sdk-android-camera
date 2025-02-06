package com.truvideo.sdk.camera.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkArCameraConfiguration(
    val orientation: TruvideoSdkCameraOrientation? = null,
    val outputPath: String = "",
    val mode: TruvideoSdkCameraMode = TruvideoSdkCameraMode.videoAndImage()
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkArCameraConfiguration {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}


