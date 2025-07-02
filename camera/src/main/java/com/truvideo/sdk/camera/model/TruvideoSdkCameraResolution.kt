package com.truvideo.sdk.camera.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkCameraResolution(val width: Int, val height: Int) {

    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraResolution {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }

    val aspectRatio: Float
        get() = width.toFloat() / height.toFloat()

    val label: String
        get() = when {
            width >= 3840 -> "4K"
            width >= 1920 -> "FHD"
            width >= 1280 -> "HD"
            else -> "SD"
        }
}