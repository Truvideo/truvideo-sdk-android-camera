package com.truvideo.sdk.camera.model

import android.graphics.Rect
import com.truvideo.sdk.camera.data.serializer.RectSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

@Serializable
class TruvideoSdkCameraDevice(
    val id: String,
    val lensFacing: TruvideoSdkCameraLensFacing,
    val resolutions: List<TruvideoSdkCameraResolution>,
    val withFlash: Boolean,
    val isTapToFocusEnabled: Boolean,
    val sensorOrientation: Int,
    @Serializable(with = RectSerializer::class)
    val sensorSize: Rect,
    val isLogicalCamera: Boolean
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TruvideoSdkCameraDevice {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}

