package com.truvideo.sdk.camera.domain.repository

import android.view.Surface
import com.truvideo.sdk.camera.adapters.CameraEvent
import com.truvideo.sdk.camera.adapters.FocusState
import com.truvideo.sdk.camera.adapters.ImageCaptureEvent
import com.truvideo.sdk.camera.adapters.RecordingEvent
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import com.truvideo.sdk.camera.model.RecordingSetUp
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface TruvideoSdkCameraRepository {
    fun startPreview(cameraId: String, surface: Surface, resolution: TruvideoSdkCameraResolution, captureConfig: CameraCaptureConfig)
    fun stopPreview()
    fun takeImage(captureConfig: CameraCaptureConfig) : Flow<ImageCaptureEvent>
    fun takeVideoSnapshot(captureConfig: CameraCaptureConfig) : Flow<ImageCaptureEvent>
    fun requestFocus(captureConfig: CameraCaptureConfig): Flow<FocusState>
    fun getCameraDeviceByLensFacing(lensFacingType: TruvideoSdkCameraLensFacing): TruvideoSdkCameraDevice?
    fun release()
    fun changeCamera(cameraId: String, resolution: TruvideoSdkCameraResolution, captureConfig: CameraCaptureConfig)
    fun requestFocusOnPosition(cameraCaptureConfig: CameraCaptureConfig): Flow<FocusState>
    fun setFlash(cameraCaptureConfig: CameraCaptureConfig) : Flow<Boolean>
    fun setZoomLevel(cameraCaptureConfig: CameraCaptureConfig) : Flow<Boolean>
    fun startRecording(
        captureConfig: CameraCaptureConfig,
        recordingSetUp: RecordingSetUp,
    )
    fun stopRecording(maxDurationReached: Boolean)
    fun resumeRecording(
        captureConfig: CameraCaptureConfig,
        recordingSetUp: RecordingSetUp,
    )
    fun pauseRecording()
    fun observeRecordingEvents() : SharedFlow<RecordingEvent>
    fun observeCameraEvents() : SharedFlow<CameraEvent>
    fun restartPreview(
        restartCamera: Boolean,
        cameraId: String?,
        resolution: TruvideoSdkCameraResolution?,
        captureConfig : CameraCaptureConfig
    )
}