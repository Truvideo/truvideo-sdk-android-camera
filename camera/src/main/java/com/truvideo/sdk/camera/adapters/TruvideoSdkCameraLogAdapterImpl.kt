package com.truvideo.sdk.camera.adapters

import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraLogAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraVersionPropertiesAdapter
import truvideo.sdk.common.model.TruvideoSdkLog
import truvideo.sdk.common.model.TruvideoSdkLogModule
import truvideo.sdk.common.model.TruvideoSdkLogSeverity
import truvideo.sdk.common.sdk_common

internal class TruvideoSdkCameraLogAdapterImpl(
    versionPropertiesAdapter: TruvideoSdkCameraVersionPropertiesAdapter
) : TruvideoSdkCameraLogAdapter {

    private val moduleVersion = versionPropertiesAdapter.readProperty("versionName") ?: "Unknown"

    override fun addLog(eventName: String, message: String, severity: TruvideoSdkLogSeverity) {
        sdk_common.log.add(
            TruvideoSdkLog(
                tag = eventName,
                message = message,
                severity = severity,
                module = TruvideoSdkLogModule.CAMERA,
                moduleVersion = moduleVersion,
            )
        )
    }
}