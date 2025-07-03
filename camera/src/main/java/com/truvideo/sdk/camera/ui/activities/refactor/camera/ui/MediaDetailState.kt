package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import kotlinx.serialization.Serializable

@Serializable
data class MediaDetailState(
    val media: TruvideoSdkCameraMedia? = null,
    val index: Int = 0
)