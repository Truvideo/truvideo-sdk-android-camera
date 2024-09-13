package com.truvideo.sdk.camera.adapters

import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraLogAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraVersionPropertiesAdapter
import truvideo.sdk.common.exception.TruvideoSdkAuthenticationRequiredException
import truvideo.sdk.common.exception.TruvideoSdkNotInitializedException
import truvideo.sdk.common.model.TruvideoSdkLogSeverity
import truvideo.sdk.common.sdk_common

internal class TruvideoSdkCameraAuthAdapterImpl(
    versionPropertiesAdapter: TruvideoSdkCameraVersionPropertiesAdapter,
    private val logAdapter: TruvideoSdkCameraLogAdapter
) : TruvideoSdkCameraAuthAdapter {

    private val shouldValidate = versionPropertiesAdapter.readProperty("validateAuthentication") != "false"

    override fun validateAuthentication() {
        if (!shouldValidate) return

        val isAuthenticated = sdk_common.auth.isAuthenticated
        if (!isAuthenticated) {
            logAdapter.addLog(
                "event_camera_auth_validate",
                "Validate authentication failed: SDK not authenticated",
                TruvideoSdkLogSeverity.ERROR,
            )
            throw TruvideoSdkAuthenticationRequiredException()
        }

        val isInitialized = sdk_common.auth.isInitialized
        if (!isInitialized) {
            logAdapter.addLog(
                "event_camera_auth_validate",
                "Validate authentication failed: SDK not initialized",
                TruvideoSdkLogSeverity.ERROR,
            )
            throw TruvideoSdkNotInitializedException()
        }
    }
}