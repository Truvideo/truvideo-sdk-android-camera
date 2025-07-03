package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

data class ErrorState(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)