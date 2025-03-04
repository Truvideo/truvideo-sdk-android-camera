package com.truvideo.sdk.camera.model.events

import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkCameraEventImageTaken(
    val media: TruvideoSdkCameraMedia
) : TruvideoSdkCameraEventData() {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraEventImageTaken {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}