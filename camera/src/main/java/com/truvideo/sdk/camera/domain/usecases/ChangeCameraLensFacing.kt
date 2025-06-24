package com.truvideo.sdk.camera.domain.usecases

import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution

interface ChangeCameraUseCase {
    operator fun invoke(cameraId: String, resolution: TruvideoSdkCameraResolution, cameraCaptureConfig: CameraCaptureConfig)
}

class ChangeCameraUseCaseImpl(
    private val repository: TruvideoSdkCameraRepository
) : ChangeCameraUseCase {
    override fun invoke(
        cameraId: String,
        resolution: TruvideoSdkCameraResolution,
        cameraCaptureConfig: CameraCaptureConfig
    ) = repository.changeCamera(cameraId, resolution, cameraCaptureConfig)
}