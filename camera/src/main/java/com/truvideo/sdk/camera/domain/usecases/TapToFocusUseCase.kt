package com.truvideo.sdk.camera.domain.usecases

import com.truvideo.sdk.camera.adapters.FocusState
import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import kotlinx.coroutines.flow.Flow

interface RequestFocusOnPositionUseCase {
    operator fun invoke(
        cameraCaptureConfig: CameraCaptureConfig
    ) : Flow<FocusState>
}

class RequestFocusOnPositionUseCaseImpl(
    private val repository: TruvideoSdkCameraRepository
) : RequestFocusOnPositionUseCase {
    override operator fun invoke(
        cameraCaptureConfig: CameraCaptureConfig
    ) =
        repository.requestFocusOnPosition(cameraCaptureConfig)
}