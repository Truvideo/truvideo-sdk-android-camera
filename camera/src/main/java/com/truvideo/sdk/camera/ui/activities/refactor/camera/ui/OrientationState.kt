package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class OrientationState(
    val orientation: TruvideoSdkCameraOrientation = TruvideoSdkCameraOrientation.PORTRAIT,
    val fixedOrientation: TruvideoSdkCameraOrientation? = null,
) {
    fun toJson(): String = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String): OrientationState {
            if (json.isEmpty()) return OrientationState()
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}