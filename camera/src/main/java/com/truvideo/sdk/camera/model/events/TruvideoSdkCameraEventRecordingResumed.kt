package com.truvideo.sdk.camera.model.events

import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkCameraEventRecordingResumed(
    val resolution: TruvideoSdkCameraResolution,
    val orientation: TruvideoSdkCameraOrientation,
    val lensFacing: TruvideoSdkCameraLensFacing
) : TruvideoSdkCameraEventData() {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraEventRecordingResumed {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}