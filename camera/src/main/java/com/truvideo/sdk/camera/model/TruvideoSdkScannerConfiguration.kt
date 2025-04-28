package com.truvideo.sdk.camera.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkScannerConfiguration(
    val flashMode: TruvideoSdkCameraFlashMode = TruvideoSdkCameraFlashMode.OFF,
    val orientation: TruvideoSdkCameraOrientation? = null,
    val codeFormats: List<TruvideoSdkScannerCodeFormat> = TruvideoSdkScannerCodeFormat.all
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkScannerConfiguration {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}


