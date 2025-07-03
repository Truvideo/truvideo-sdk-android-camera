package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import com.truvideo.sdk.camera.model.TruvideoSdkCameraImageFormat
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CameraConfig(
    val setUp: Boolean = false,
    val outputPath: String? = null,
    val flashOnByDefault: Boolean = false,
    val defaultLensFacing: TruvideoSdkCameraLensFacing = TruvideoSdkCameraLensFacing.BACK,
    val imageFormat: TruvideoSdkCameraImageFormat = TruvideoSdkCameraImageFormat.PNG,
    val fixedOrientation: TruvideoSdkCameraOrientation? = null,
    val defaultBackResolution: TruvideoSdkCameraResolution? = null,
    val defaultFrontResolution: TruvideoSdkCameraResolution? = null,
    val backResolutions: List<TruvideoSdkCameraResolution> = emptyList(),
    val frontResolutions: List<TruvideoSdkCameraResolution> = emptyList(),
    val cameraMode: TruvideoSdkCameraMode = TruvideoSdkCameraMode.videoAndImage()
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): CameraConfig {
            if (json.isEmpty()) return CameraConfig()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }

}