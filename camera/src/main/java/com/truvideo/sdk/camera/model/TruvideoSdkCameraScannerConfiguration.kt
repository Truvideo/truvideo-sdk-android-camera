package com.truvideo.sdk.camera.model

import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraScannerValidation
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkCameraScannerConfiguration(
    val flashMode: TruvideoSdkCameraFlashMode = TruvideoSdkCameraFlashMode.OFF,
    val orientation: TruvideoSdkCameraOrientation? = null,
    val codeFormats: List<TruvideoSdkCameraScannerCodeFormat> = TruvideoSdkCameraScannerCodeFormat.all,
    @Transient val validator: TruvideoSdkCameraScannerValidation? = null,
    val autoClose: Boolean = false
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraScannerConfiguration {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}