package com.truvideo.sdk.camera.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkCameraScannerValidationResult(
    internal val accept: Boolean,
    internal  val message: String?
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraScannerValidationResult {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }

        @JvmStatic
        fun success(): TruvideoSdkCameraScannerValidationResult {
            return TruvideoSdkCameraScannerValidationResult(
                accept = true,
                message = null
            )
        }

        @JvmStatic
        fun fail(message: String): TruvideoSdkCameraScannerValidationResult {
            return TruvideoSdkCameraScannerValidationResult(
                accept = false,
                message = message
            )
        }
    }
}