package com.truvideo.sdk.camera.model.events

import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkCameraEventFlashModeChanged(
    val flashMode: TruvideoSdkCameraFlashMode
) : TruvideoSdkCameraEventData() {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraEventFlashModeChanged {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}