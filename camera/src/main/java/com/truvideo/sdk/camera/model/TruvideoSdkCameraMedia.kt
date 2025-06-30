package com.truvideo.sdk.camera.model

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TruvideoSdkCameraMedia(
    val id: String,
    val createdAt: Long,
    val filePath: String,
    val type: TruvideoSdkCameraMediaType,
    val lensFacing: TruvideoSdkCameraLensFacing,
    val orientation: TruvideoSdkCameraOrientation,
    val resolution: TruvideoSdkCameraResolution,
    val duration: Long,
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraMedia {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }

    fun getVideoThumbnail(): Bitmap? = try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        retriever.release()
        frame
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}