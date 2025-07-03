package com.truvideo.sdk.camera.domain.models

import android.graphics.Rect
import android.hardware.camera2.params.MeteringRectangle
import com.truvideo.sdk.camera.data.serializer.MeteringRectangleSerializer
import com.truvideo.sdk.camera.data.serializer.RectSerializer
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.RecordingState
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RecordingConfig(
    val recordingPath: String? = null,
    val recordingState: RecordingState = RecordingState.IDLE,
    val recordingResolution: TruvideoSdkCameraResolution? = null,
    val recordingOrientation: TruvideoSdkCameraOrientation? = null,
    val recordingLensFacing: TruvideoSdkCameraLensFacing? = null,
    val recordingTimer: Long = 0L,
    val maxRecordingTimeReached: Boolean = false,
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): RecordingConfig {
            if (json.isEmpty()) return RecordingConfig()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}

@Serializable
data class CameraCaptureConfig(
    val template: CaptureTemplate = CaptureTemplate.Preview,
    val autoFocus: AutoFocusMode = AutoFocusMode.Off,
    val flash: FlashMode = FlashMode.Off,
    @Serializable(with = MeteringRectangleSerializer::class)
    val focusArea: MeteringRectangle? = null,
    val autoWhiteBalance: AutoWhiteBalanceMode = AutoWhiteBalanceMode.Auto,
    val autoFocusTrigger: AutoFocusTrigger = AutoFocusTrigger.Off,
    @Serializable(with = RectSerializer::class)
    val zoomArea : Rect? = null,
    val zoomLevel : Float = 1.0f,
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): CameraCaptureConfig {
            if (json.isEmpty()) return CameraCaptureConfig()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}

enum class CaptureTemplate { Preview, StillCapture, Record, Snapshot }
enum class AutoFocusMode { Off, Auto, Continuous }
enum class AutoWhiteBalanceMode { Off, Auto }
enum class AutoFocusTrigger { Off,Start }
enum class FlashMode { Off, Single, Torch }

fun FlashMode.toTruvideoSdkFlashMode() : TruvideoSdkCameraFlashMode =
    when(this) {
        FlashMode.Off -> TruvideoSdkCameraFlashMode.OFF
        else -> TruvideoSdkCameraFlashMode.ON
    }
fun FlashMode.enabled(): Boolean = this != FlashMode.Off
fun FlashMode.toggled(
    canTakeImage: Boolean,
): FlashMode =
    if (this == FlashMode.Off)
            (if (canTakeImage) FlashMode.Single else FlashMode.Torch)
    else FlashMode.Off