package com.truvideo.sdk.camera.domain.usecases

import com.truvideo.sdk.camera.adapters.CameraEvent
import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository
import kotlinx.coroutines.flow.Flow

interface ObserveCameraEventsUseCase {
    operator fun invoke() : Flow<CameraEvent>
}

class ObserveCameraEventsUseCaseImpl(
    private val repository: TruvideoSdkCameraRepository
) : ObserveCameraEventsUseCase {
    override fun invoke(): Flow<CameraEvent> =
        repository.observeCameraEvents()
}