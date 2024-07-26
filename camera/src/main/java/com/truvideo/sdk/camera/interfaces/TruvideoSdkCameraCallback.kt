package com.truvideo.sdk.camera.interfaces

import com.truvideo.sdk.camera.exceptions.TruvideoSdkCameraException

interface TruvideoSdkCameraCallback<T> {
    fun onComplete(result: T)

    fun onError(exception: TruvideoSdkCameraException)
}