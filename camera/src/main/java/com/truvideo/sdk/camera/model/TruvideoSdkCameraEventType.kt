package com.truvideo.sdk.camera.model

import kotlinx.serialization.Serializable

@Serializable
enum class TruvideoSdkCameraEventType {
    RecordingStarted,
    RecordingFinished,
    RecordingPaused,
    RecordingResumed,
    PictureTaken,
    CameraFlipped,
    ResolutionChanged,
    FlashModeChanged,
    ZoomChanged,
    MediaDeleted,
    MediaDiscard,
    Continue
}


