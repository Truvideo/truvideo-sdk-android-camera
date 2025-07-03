package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

data class ReducedResult<S, E>(
    val newState: S,
    val effect: E? = null,
    val onFlowUpdates: (suspend (MutableStateFlow<S>, MutableSharedFlow<E>) -> Unit)? = null
)