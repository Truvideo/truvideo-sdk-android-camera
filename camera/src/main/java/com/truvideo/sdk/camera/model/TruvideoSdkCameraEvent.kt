package com.truvideo.sdk.camera.model

import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventData
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date

@Serializable
data class TruvideoSdkCameraEvent(
    val type: TruvideoSdkCameraEventType,
    val data: TruvideoSdkCameraEventData,
    private val createdAtMillis: Long
) {
    val createdAt: Date
        get() {
            return Date(createdAtMillis)
        }

    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraEvent {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}
