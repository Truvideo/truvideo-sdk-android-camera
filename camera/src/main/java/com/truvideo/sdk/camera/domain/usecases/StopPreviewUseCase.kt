package com.truvideo.sdk.camera.domain.usecases

import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository

interface StopPreviewUseCase {
}

class StopPreviewUseCaseImpl (
    private val repository: TruvideoSdkCameraRepository
) : StopPreviewUseCase {
    operator fun invoke() {
        repository.stopPreview()
    }
}