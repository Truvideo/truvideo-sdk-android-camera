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
        get() {
            return height.toFloat() / width
        }
}