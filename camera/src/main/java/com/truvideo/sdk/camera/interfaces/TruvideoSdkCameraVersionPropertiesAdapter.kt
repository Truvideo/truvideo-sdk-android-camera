package com.truvideo.sdk.camera.interfaces

internal interface TruvideoSdkCameraVersionPropertiesAdapter {
    fun readProperty(propertyName: String): String?
}