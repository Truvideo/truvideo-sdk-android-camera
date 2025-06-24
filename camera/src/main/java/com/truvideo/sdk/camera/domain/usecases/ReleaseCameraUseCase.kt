package com.truvideo.sdk.camera.domain.usecases

import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository

class ReleaseCameraUseCase (
    private val repository: TruvideoSdkCameraRepository
) {

    operator fun invoke() = repository.release()
}