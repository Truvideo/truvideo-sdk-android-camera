package com.truvideo.sdk.camera.model.events

import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkCameraEventResolutionChanged(
    val resolution: TruvideoSdkCameraResolution
) : TruvideoSdkCameraEventData() {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraEventResolutionChanged {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}