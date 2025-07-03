package com.truvideo.sdk.camera.domain.usecases

import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution


interface RestartPreviewUseCase {
    operator fun invoke(restartCamera: Boolean = false,
                        cameraId: String? = null,
                        resolution: TruvideoSdkCameraResolution? = null,
                        captureConfig: CameraCaptureConfig)
}

class RestartPreviewUseCaseImpl(
    private val repository: TruvideoSdkCameraRepository
) : RestartPreviewUseCase {
    override fun invoke(restartCamera: Boolean,
                        cameraId: String?,
                        resolution: TruvideoSdkCameraResolution?,
                        captureConfig : CameraCaptureConfig) {
        repository.restartPreview(restartCamera, cameraId, resolution, captureConfig)
    }
}