package com.truvideo.sdk.camera.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkCameraMedia(
    val id: String,
    val createdAt: Long,
    val filePath: String,
    val type: TruvideoSdkCameraMediaType,
    val lensFacing: TruvideoSdkCameraLensFacing,
    val orientation: TruvideoSdkCameraOrientation,
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
}