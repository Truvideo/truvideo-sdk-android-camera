package com.truvideo.sdk.camera

import com.truvideo.sdk.camera.interfaces.TruvideoSdkCamera
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import com.truvideo.sdk.camera.usecase.GetCameraInformationUseCase

internal class TruvideoSdkCameraImpl(
    private val getCameraInformationUseCase: GetCameraInformationUseCase,
    private val authAdapter: TruvideoSdkCameraAuthAdapter
) : TruvideoSdkCamera {

    override fun getInformation(): TruvideoSdkCameraInformation {
        authAdapter.validateAuthentication()
        return getCameraInformationUseCase()
    }

    override val environment: String
        get() = BuildConfig.FLAVOR

}
