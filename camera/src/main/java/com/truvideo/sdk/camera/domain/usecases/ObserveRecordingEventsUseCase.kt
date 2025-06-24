package com.truvideo.sdk.camera.domain.usecases

import com.truvideo.sdk.camera.adapters.RecordingEvent
import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository
import kotlinx.coroutines.flow.Flow

interface ObserveRecordingEventsUseCase {
    operator fun invoke(): Flow<RecordingEvent>
}

class ObserveRecordingEventsUseCaseImpl(
    private val repository: TruvideoSdkCameraRepository
) : ObserveRecordingEventsUseCase {

    override fun invoke(): Flow<RecordingEvent> =
        repository.observeRecordingEvents()
}