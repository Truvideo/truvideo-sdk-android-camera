package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import kotlinx.serialization.Serializable

@Serializable
sealed interface CameraConnectionState {
    @Serializable
    data object Connected : CameraConnectionState
    @Serializable
    data object Disconnected : CameraConnectionState
    @Serializable
    data object Error : CameraConnectionState

    fun isDisconnected() : Boolean = this is Disconnected
    fun isNotConnected() : Boolean = this is Disconnected || this is Error
}