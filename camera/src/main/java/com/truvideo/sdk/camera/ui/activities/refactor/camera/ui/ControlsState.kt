package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ControlsState(
    val isTakingPictureButtonEnabled: Boolean = false,
    val isTakingPictureButtonVisible: Boolean = false,
    val isCaptureButtonEnabled: Boolean = false,
    val isLensFacingRotationButtonEnabled: Boolean = false,
    val isLensFacingRotationButtonVisible: Boolean = false,
    val isPauseButtonEnabled: Boolean = false,
    val isPauseButtonVisible: Boolean = false,
    val isFlashButtonVisible: Boolean = false,
    val isFlashButtonEnabled: Boolean = false,
    val isResolutionsButtonVisible: Boolean = false,
    val isResolutionsButtonEnabled: Boolean = false,
    val isMediaCounterButtonVisible: Boolean = false,
    val isMediaCounterButtonEnabled: Boolean = false,
    val isContinueButtonVisible: Boolean = false,
    val isContinueButtonEnabled: Boolean = false,
) {
    fun toJson(): String = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String): ControlsState {
            if (json.isEmpty()) return ControlsState()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}