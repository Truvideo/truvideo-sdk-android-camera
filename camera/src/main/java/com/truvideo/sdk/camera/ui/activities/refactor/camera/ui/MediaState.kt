package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MediaState(
    val media: List<TruvideoSdkCameraMedia> = emptyList(),
    val tempMedia: List<TruvideoSdkCameraMedia> = emptyList(),
    val mediaOutputDirectory: String? = null,
    val maxVideoCountReached: Boolean = false,
    val maxImageCountReached: Boolean = false,
    val maxMediaCountReached: Boolean = false,
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): MediaState {
            if (json.isEmpty()) return MediaState()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}