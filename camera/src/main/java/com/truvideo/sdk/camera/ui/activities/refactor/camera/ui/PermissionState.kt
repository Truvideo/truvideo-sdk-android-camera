package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PermissionState(
    val permissionGranted: Boolean = false,
    val authenticated: Boolean = false,
) {
    fun toJson(): String = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String): PermissionState {
            if (json.isEmpty()) return PermissionState()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}