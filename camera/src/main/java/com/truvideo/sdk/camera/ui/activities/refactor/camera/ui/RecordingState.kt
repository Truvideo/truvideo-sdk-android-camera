package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

enum class RecordingState { RECORDING, PAUSED, IDLE }

fun RecordingState.isRecording() : Boolean = this == RecordingState.RECORDING

fun RecordingState.isPaused() : Boolean = this == RecordingState.PAUSED

fun RecordingState.isIdle() : Boolean = this == RecordingState.IDLE