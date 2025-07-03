package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PanelsControlState(
    val isMediaPanelVisible: Boolean = false,
    val isResolutionsPanelVisible: Boolean = false,
    val isDiscardPanelVisible: Boolean = false,
    val isMediaPanelDetailVisible: Boolean = false,
    val mediaDetailState: MediaDetailState = MediaDetailState(),
) {
    fun toJson(): String = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String): PanelsControlState {
            if (json.isEmpty()) return PanelsControlState()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}
