package com.truvideo.sdk.camera.interfaces

import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation

interface TruvideoSdkCamera {

    fun getInformation(): TruvideoSdkCameraInformation

    val environment: String
}