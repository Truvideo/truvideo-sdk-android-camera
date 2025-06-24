package com.truvideo.sdk.camera.domain.usecases

import android.view.Surface
import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution

interface StartPreviewUseCase {
    operator fun invoke(
        cameraId: String,
        surface: Surface,
        resolution: TruvideoSdkCameraResolution,
        cameraMode: CameraCaptureConfig,
    )
}

class StartPreviewUseCaseImpl (
    private val repository: TruvideoSdkCameraRepository
) : StartPreviewUseCase {
    override operator fun invoke(
        cameraId: String,
        surface: Surface,
        resolution: TruvideoSdkCameraResolution,
        cameraCaptureConfig: CameraCaptureConfig
    ) {
        repository.startPreview(cameraId, surface, resolution, cameraCaptureConfig)
    }
}