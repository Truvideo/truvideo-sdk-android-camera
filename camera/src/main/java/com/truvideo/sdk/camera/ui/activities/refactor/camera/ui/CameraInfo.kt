package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CameraInfo(
    val info: TruvideoSdkCameraInformation? = null
) {
    fun toJson(): String = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String): CameraInfo {
            if (json.isEmpty()) return CameraInfo()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}