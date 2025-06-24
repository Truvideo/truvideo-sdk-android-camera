package com.truvideo.sdk.camera.domain.usecases

import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import kotlinx.coroutines.flow.Flow

interface SetZoomLevelUseCase {
    operator fun invoke(cameraCaptureConfig: CameraCaptureConfig): Flow<Boolean>
}

class SetZoomLevelUseCaseImpl(
    val repository: TruvideoSdkCameraRepository
) : SetZoomLevelUseCase {
    override operator fun invoke(cameraCaptureConfig: CameraCaptureConfig): Flow<Boolean> =
        repository.setZoomLevel(cameraCaptureConfig)
}