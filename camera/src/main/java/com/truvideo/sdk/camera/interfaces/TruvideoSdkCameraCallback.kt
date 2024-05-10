package com.truvideo.sdk.camera.interfaces

import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia

interface TruvideoSdkCameraCallback {
    fun onResult(media: List<TruvideoSdkCameraMedia>)
}