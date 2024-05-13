package com.truvideo.sdk.camera.adapters

import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import truvideo.sdk.common.exception.TruvideoSdkAuthenticationRequiredException
import truvideo.sdk.common.exception.TruvideoSdkNotInitializedException
import truvideo.sdk.common.sdk_common

internal class TruvideoSdkCameraAuthAdapterImpl(
    private val versionPropertiesAdapter: VersionPropertiesAdapter
) : TruvideoSdkCameraAuthAdapter {

    private fun shouldValidate(): Boolean {
        return versionPropertiesAdapter.readProperty("validateAuthentication") != "false"
    }

    override fun validateAuthentication() {
        if (!shouldValidate()) return

        val isAuthenticated = sdk_common.auth.isAuthenticated.value
        if (!isAuthenticated) {
            throw TruvideoSdkAuthenticationRequiredException()
        }

        val isInitialized = sdk_common.auth.isInitialized.value
        if (!isInitialized) {
            throw TruvideoSdkNotInitializedException()
        }
    }

}