package com.truvideo.sdk.camera.model.events

import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkCameraEventCameraFlipped(
    val lensFacing: TruvideoSdkCameraLensFacing
) : TruvideoSdkCameraEventData(){
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraEventCameraFlipped {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}