package com.truvideo.sdk.camera.model.events

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkCameraEventZoomChanged(
    val zoom: Float
) : TruvideoSdkCameraEventData() {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraEventZoomChanged {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}