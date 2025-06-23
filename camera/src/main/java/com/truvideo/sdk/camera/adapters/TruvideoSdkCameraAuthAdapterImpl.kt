package com.truvideo.sdk.camera.adapters

import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraLogAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraVersionPropertiesAdapter
import truvideo.sdk.common.exceptions.TruvideoSdkAuthenticationRequiredException
import truvideo.sdk.common.exceptions.TruvideoSdkNotInitializedException
import truvideo.sdk.common.model.TruvideoSdkLogSeverity
import truvideo.sdk.common.sdk_common
import truvideo.sdk.common.util.TruvideoSdkCommonExceptionParser
import truvideo.sdk.common.util.parse

internal class TruvideoSdkCameraAuthAdapterImpl(
    versionPropertiesAdapter: TruvideoSdkCameraVersionPropertiesAdapter,
    private val logAdapter: TruvideoSdkCameraLogAdapter
) : TruvideoSdkCameraAuthAdapter {

    private val validateAuthentication: Boolean = versionPropertiesAdapter.readProperty("validateAuthentication") != "false"

    private fun isAuthenticated(): Boolean {
        if (!validateAuthentication) return true

        try {
            return sdk_common.auth.isAuthenticated()
        } catch (exception: Exception) {
            val parsedException = TruvideoSdkCommonExceptionParser().parse(exception)
            parsedException.printStackTrace()

            logAdapter.addLog(
                eventName = "event_media_auth_validate_is_authenticated",
                message = "Validate is authenticated failed: ${parsedException.localizedMessage}",
                severity = TruvideoSdkLogSeverity.ERROR
            )

            throw exception
        }
    }

    private fun isInitialized(): Boolean {
        if (!validateAuthentication) return true

        try {
            return sdk_common.auth.isInitialized
        } catch (exception: Exception) {
            val parsedException = TruvideoSdkCommonExceptionParser().parse(exception)
            parsedException.printStackTrace()

            logAdapter.addLog(
                eventName = "event_media_auth_validate_is_initialized",
                message = "Validate is initialized failed: ${parsedException.localizedMessage}",
                severity = TruvideoSdkLogSeverity.ERROR
            )

            throw exception
        }
    }

    override fun validateAuthentication() {
        val isAuthenticated = isAuthenticated()
        if (!isAuthenticated) {
            logAdapter.addLog(
                "event_camera_auth_validate",
                "Validate authentication failed: SDK not authenticated",
                TruvideoSdkLogSeverity.ERROR,
            )
            throw TruvideoSdkAuthenticationRequiredException()
        }

        val isInitialized = isInitialized()
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