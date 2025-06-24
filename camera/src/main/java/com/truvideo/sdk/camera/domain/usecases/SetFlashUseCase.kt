package com.truvideo.sdk.camera.domain.usecases

import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import kotlinx.coroutines.flow.Flow

interface SetFlashUseCase {
    operator fun invoke(cameraCaptureConfig: CameraCaptureConfig): Flow<Boolean>
}

class SetFlashUseCaseImpl(
    val repository: TruvideoSdkCameraRepository
) : SetFlashUseCase {
    override operator fun invoke(cameraCaptureConfig: CameraCaptureConfig) : Flow<Boolean> =
        repository.setFlash(cameraCaptureConfig)

}