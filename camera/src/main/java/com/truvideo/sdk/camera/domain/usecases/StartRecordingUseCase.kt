package com.truvideo.sdk.camera.domain.usecases

import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import dev.romainguy.kotlin.math.max
import java.io.File

interface StartRecordingUseCase {
    operator fun invoke(
        cameraConfig: CameraCaptureConfig,
        outputFile: File,
        orientation: Int,
        durationLimit: Int?,
        resolution: TruvideoSdkCameraResolution,
    )
}

class StartRecordingUseCaseImpl(
    private val repository: TruvideoSdkCameraRepository
) : StartRecordingUseCase {
    override operator fun invoke(
        cameraConfig: CameraCaptureConfig,
        outputFile: File,
        orientation: Int,
        durationLimit: Int?,
        resolution: TruvideoSdkCameraResolution,
    ) {
        repository.startRecording(cameraConfig, outputFile, orientation, durationLimit, resolution)
    }
}

interface StopRecordingUseCase {
    operator fun invoke(maxDurationReached: Boolean = false)
}

class StopRecordingUseCaseImpl(
    private val repository: TruvideoSdkCameraRepository
) : StopRecordingUseCase {
    override operator fun invoke(maxDurationReached: Boolean) {
        repository.stopRecording(maxDurationReached)
    }
}


interface PauseRecordingUseCase {
    operator fun invoke()
}

class PauseRecordingUseCaseImpl(
    private val repository: TruvideoSdkCameraRepository
) : PauseRecordingUseCase {
    override operator fun invoke() {
        repository.pauseRecording()
    }
}

interface ResumeRecordingUseCase {
    operator fun invoke(
        cameraConfig: CameraCaptureConfig,
        outputFile: File,
        orientation: Int,
        durationLimit: Int?,
        resolution: TruvideoSdkCameraResolution,
    )
}

class ResumeRecordingUseCaseImpl(
    private val repository: TruvideoSdkCameraRepository
) : ResumeRecordingUseCase {
    override operator fun invoke(
        cameraConfig: CameraCaptureConfig,
        outputFile: File,
        orientation: Int,
        durationLimit: Int?,
        resolution: TruvideoSdkCameraResolution,
    ) {
        repository.startRecording(cameraConfig, outputFile, orientation, durationLimit, resolution)
    }
}

