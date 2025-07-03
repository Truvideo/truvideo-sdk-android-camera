package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import androidx.compose.ui.unit.IntOffset
import com.truvideo.sdk.camera.adapters.FocusState
import com.truvideo.sdk.camera.data.serializer.IntOffsetSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FocusIndicatorState(
    val focusState: FocusState = FocusState.Idle,
    @Serializable(with = IntOffsetSerializer::class)
    val position: IntOffset = IntOffset.Zero
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): FocusIndicatorState {
            if (json.isEmpty()) return FocusIndicatorState()
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }

            return jsonConfig.decodeFromString(json)
        }
    }
}