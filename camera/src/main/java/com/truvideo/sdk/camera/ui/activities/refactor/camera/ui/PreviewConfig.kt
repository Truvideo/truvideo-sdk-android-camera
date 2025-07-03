package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.components.zoom_indicator.ZoomIndicatorMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PreviewConfig(
    val isBusy: Boolean = false,
    val currentResolution : TruvideoSdkCameraResolution? = null,
    val currentLensFacing: TruvideoSdkCameraLensFacing = TruvideoSdkCameraLensFacing.BACK,
    val viewPortWidth: Int = 0,
    val viewPortHeight: Int = 0,
    val zoomIndicatorMode: ZoomIndicatorMode = ZoomIndicatorMode.Indicator,
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): PreviewConfig {
            if (json.isEmpty()) return PreviewConfig()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}
