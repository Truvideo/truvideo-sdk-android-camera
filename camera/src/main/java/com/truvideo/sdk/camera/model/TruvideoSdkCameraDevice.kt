package com.truvideo.sdk.camera.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class TruvideoSdkCameraDevice(
    val id: String,
    val lensFacing: TruvideoSdkCameraLensFacing,
    val resolutions: List<TruvideoSdkCameraResolution>,
    val withFlash: Boolean,
    val isTapToFocusEnabled: Boolean,
    val sensorOrientation: Int,
    val isLogicalCamera: Boolean
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraDevice {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}