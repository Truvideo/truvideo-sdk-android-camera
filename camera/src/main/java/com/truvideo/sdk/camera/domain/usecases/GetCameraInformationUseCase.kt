package com.truvideo.sdk.camera.domain.usecases

import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing

interface GetCameraInformationUseCase {
    operator fun invoke(): TruvideoSdkCameraInformation
}

class GetCameraInformationUseCaseImpl (
    private val repository: TruvideoSdkCameraRepository
) : GetCameraInformationUseCase {

    override operator fun invoke(): TruvideoSdkCameraInformation {
        val backCamera = repository.getCameraDeviceByLensFacing(TruvideoSdkCameraLensFacing.BACK)
        val frontCamera = repository.getCameraDeviceByLensFacing(TruvideoSdkCameraLensFacing.FRONT)

        return TruvideoSdkCameraInformation(
            frontCamera = frontCamera, backCamera = backCamera
        )
    }
}