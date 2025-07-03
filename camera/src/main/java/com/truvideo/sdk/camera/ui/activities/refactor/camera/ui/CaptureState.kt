package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import com.truvideo.sdk.camera.domain.models.AutoFocusMode
import com.truvideo.sdk.camera.domain.models.AutoWhiteBalanceMode
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import com.truvideo.sdk.camera.domain.models.CaptureTemplate
import com.truvideo.sdk.camera.domain.models.FlashMode
import com.truvideo.sdk.camera.domain.models.RecordingConfig
import com.truvideo.sdk.camera.domain.models.enabled
import com.truvideo.sdk.camera.model.RecordingSetUp
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CaptureState(
    val previewConfig: PreviewConfig = PreviewConfig(),
    val recordingConfig: RecordingConfig = RecordingConfig(),
    val captureConfig: CameraCaptureConfig = CameraCaptureConfig(),
) {
    fun toJson(): String = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String): CaptureState {
            if (json.isEmpty()) return CaptureState()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}

fun CaptureState.toRecordingState(
    recordingSetUp: RecordingSetUp
) : CaptureState =
    copy(
        recordingConfig = recordingConfig.copy(
            recordingState = RecordingState.RECORDING,
            recordingPath = recordingSetUp.videoPath,
            recordingResolution = recordingSetUp.resolution,
            recordingOrientation = recordingSetUp.orientation,
            recordingLensFacing = recordingSetUp.lensFacing,
        ),
        previewConfig = previewConfig.copy(
            isBusy = true,
        ),
        captureConfig = captureConfig.copy(
            template = CaptureTemplate.Record,
            autoWhiteBalance = AutoWhiteBalanceMode.Auto,
            autoFocus = AutoFocusMode.Auto,
        )
    )

fun CaptureState.toPausedState() : CaptureState =
    copy(
        previewConfig = previewConfig.copy(isBusy = false),
        recordingConfig = recordingConfig.copy(
            recordingState = RecordingState.PAUSED
        ),
        captureConfig = captureConfig.copy(
            template = CaptureTemplate.Preview,
            autoWhiteBalance = AutoWhiteBalanceMode.Off,
            autoFocus = AutoFocusMode.Auto,
            flash = if (captureConfig.flash.enabled()) FlashMode.Single else FlashMode.Off
        )
    )