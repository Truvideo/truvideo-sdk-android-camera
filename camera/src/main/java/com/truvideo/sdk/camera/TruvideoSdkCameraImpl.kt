package com.truvideo.sdk.camera

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCamera
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import com.truvideo.sdk.camera.usecase.GetCameraInformationUseCase
import com.truvideo.sdk.camera.usecase.OpenCameraUseCase
import java.io.File

internal class TruvideoSdkCameraImpl(
    private val getCameraInformationUseCase: GetCameraInformationUseCase,
    private val authAdapter: TruvideoSdkCameraAuthAdapter,
    private val openCameraUseCase: OpenCameraUseCase
) : TruvideoSdkCamera {
    override fun initCameraScreen(activity: ComponentActivity) = openCameraUseCase.init(activity)

    override fun getDefaultOutputUri(context: Context): Uri {
        authAdapter.validateAuthentication()

        val file = File("${context.filesDir.path}/truvideo-sdk/camera")
        return Uri.fromFile(file)
    }

    override fun getInformation(): TruvideoSdkCameraInformation {
        authAdapter.validateAuthentication()
        return getCameraInformationUseCase()
    }

    override val environment: String
        get() = BuildConfig.FLAVOR

}
