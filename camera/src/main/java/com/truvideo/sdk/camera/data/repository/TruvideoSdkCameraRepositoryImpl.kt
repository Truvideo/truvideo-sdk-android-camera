package com.truvideo.sdk.camera.data.repository

import android.view.Surface
import com.truvideo.sdk.camera.adapters.CameraEvent
import com.truvideo.sdk.camera.adapters.FocusState
import com.truvideo.sdk.camera.adapters.ImageCaptureEvent
import com.truvideo.sdk.camera.adapters.RecordingEvent
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraAdapter
import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import dev.romainguy.kotlin.math.max
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

class TruvideoSdkCameraRepositoryImpl (
    private val cameraAdapter: TruvideoSdkCameraAdapter
): TruvideoSdkCameraRepository {

    override fun startPreview(
        cameraId: String,
        surface: Surface,
        resolution: TruvideoSdkCameraResolution,
        captureConfig: CameraCaptureConfig
    ) {
        cameraAdapter.startPreview(cameraId, surface, resolution, captureConfig)
    }

    override fun stopPreview() {
        //
    }

    override fun takeImage(
        cameraCaptureConfig: CameraCaptureConfig
    ): Flow<ImageCaptureEvent> = cameraAdapter.takeImage(cameraCaptureConfig)

    override fun takeVideoSnapshot(cameraCaptureConfig: CameraCaptureConfig): Flow<ImageCaptureEvent> =
        cameraAdapter.takeVideoSnapshot(cameraCaptureConfig)

    override fun requestFocus(cameraCaptureConfig: CameraCaptureConfig): Flow<FocusState> =
        cameraAdapter.requestFocus(cameraCaptureConfig)

    override fun getCameraDeviceByLensFacing(lensFacingType: TruvideoSdkCameraLensFacing): TruvideoSdkCameraDevice? {
        val availableCameraIds = cameraAdapter.getAvailableCameraIds()
        val cameraInfos = availableCameraIds
            .map { id -> cameraAdapter.getCameraCharacteristics(id) }
            .filter { it.lensFacing == lensFacingType }
            .map { info ->
                TruvideoSdkCameraDevice(
                    id = info.cameraId,
                    lensFacing = info.lensFacing!!,
                    resolutions = info.supportedSizes,
                    isTapToFocusEnabled = info.supportsManualFocus,
                    withFlash = info.supportsFlash,
                    sensorOrientation = info.sensorOrientation,
                    sensorSize = info.sensorSize,
                    isLogicalCamera = info.isLogicalCamera
                )
            }

        return cameraInfos.find { it.isLogicalCamera } // Priority 1: Logical camera
                ?: cameraInfos.find { it.withFlash } // Priority 2: Camera with flash
                ?: cameraInfos.maxByOrNull { it.resolutions.maxOfOrNull { it.width * it.height } ?: 0 } // Priority 3: Highest Res

    }

    override fun release() {
        cameraAdapter.release()
    }

    override fun changeCamera(
        cameraId: String,
        resolution: TruvideoSdkCameraResolution,
        cameraCaptureConfig: CameraCaptureConfig
    ) {
        cameraAdapter.startCameraById(cameraId, resolution, cameraCaptureConfig)
    }

    override fun requestFocusOnPosition(cameraCaptureConfig: CameraCaptureConfig): Flow<FocusState> =
        cameraAdapter.requestFocusOnPosition(cameraCaptureConfig)

    override fun setFlash(cameraCaptureConfig: CameraCaptureConfig): Flow<Boolean> =
        cameraAdapter.setFlash(cameraCaptureConfig)

    override fun setZoomLevel(cameraCaptureConfig: CameraCaptureConfig): Flow<Boolean> =
        cameraAdapter.setZoomLevel(cameraCaptureConfig)

    override fun startRecording(
        cameraCaptureConfig: CameraCaptureConfig,
        outputFile: File,
        orientation: Int,
        durationLimit: Int?,
        resolution: TruvideoSdkCameraResolution,
    ) =
        cameraAdapter.startRecording(
            cameraCaptureConfig,
            outputFile,
            orientation,
            durationLimit,
            resolution,
        )

    override fun stopRecording(maxDurationReached: Boolean) = cameraAdapter.stopRecording(maxDurationReached)

    override fun resumeRecording(
        cameraConfig: CameraCaptureConfig,
        outputFile: File,
        orientation: Int,
        durationLimit: Int?,
        resolution: TruvideoSdkCameraResolution,
    ) = cameraAdapter.resumeRecording(
        cameraConfig,
        outputFile,
        orientation,
        durationLimit,
        resolution
    )

    override fun pauseRecording() = cameraAdapter.pauseRecording()

    override fun observeRecordingEvents(): SharedFlow<RecordingEvent> =
        cameraAdapter.recordingEvents

    override fun observeCameraEvents(): SharedFlow<CameraEvent> = cameraAdapter.cameraEvents

    override fun restartPreview(
        restartCamera: Boolean,
        cameraId: String?,
        resolution: TruvideoSdkCameraResolution?,
        cameraCaptureConfig: CameraCaptureConfig) = cameraAdapter.restartPreview(
            restartCamera,
            cameraId,
            resolution,
            cameraCaptureConfig)


}