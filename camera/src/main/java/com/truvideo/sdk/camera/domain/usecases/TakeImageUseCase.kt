package com.truvideo.sdk.camera.domain.usecases

import com.truvideo.sdk.camera.adapters.ImageCaptureEvent
import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import kotlinx.coroutines.flow.Flow

interface TakeImageUseCase {
    operator fun invoke(
        captureConfig: CameraCaptureConfig
    ) : Flow<ImageCaptureEvent>
}

class TakeImageUseCaseImpl (
    private val repository: TruvideoSdkCameraRepository
) : TakeImageUseCase {
    override operator fun invoke(
       captureConfig: CameraCaptureConfig
    ) : Flow<ImageCaptureEvent> = repository.takeImage(captureConfig)
}

