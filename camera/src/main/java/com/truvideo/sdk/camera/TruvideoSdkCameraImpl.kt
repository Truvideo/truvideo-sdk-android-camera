package com.truvideo.sdk.camera

import android.content.Context
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCamera
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraLogAdapter
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import com.truvideo.sdk.camera.usecase.GetCameraInformationUseCase
import truvideo.sdk.common.TruvideoSdkContextProvider
import truvideo.sdk.common.model.TruvideoSdkLogSeverity

internal class TruvideoSdkCameraImpl(
    context: Context,
    private val getCameraInformationUseCase: GetCameraInformationUseCase,
    private val authAdapter: TruvideoSdkCameraAuthAdapter,
    private val logAdapter: TruvideoSdkCameraLogAdapter
) : TruvideoSdkCamera {

    init {
        TruvideoSdkContextProvider.instance.init(context);

        logAdapter.addLog(
            eventName = "event_camera_init",
            message = "Init camera module",
            severity = TruvideoSdkLogSeverity.INFO
        )
    }

    override fun getInformation(): TruvideoSdkCameraInformation {
        logAdapter.addLog(
            eventName = "event_camera_get_information",
            message = "Getting camera information",
            severity = TruvideoSdkLogSeverity.INFO,
        )
        authAdapter.validateAuthentication()

        return getCameraInformationUseCase()
    }

    override val environment: String
        get() = BuildConfig.FLAVOR

}
