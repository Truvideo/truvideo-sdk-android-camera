package com.truvideo.sdk.camera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCamera
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraCallback
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.usecase.GetCameraInformationUseCase
import com.truvideo.sdk.camera.usecase.ManipulateResolutionsUseCase
import truvideo.sdk.common.exception.TruvideoSdkAuthenticationRequiredException
import truvideo.sdk.common.exception.TruvideoSdkException
import truvideo.sdk.common.exception.TruvideoSdkNotInitializedException
import truvideo.sdk.common.sdk_common
import java.io.File

internal object TruvideoSdkCameraImpl : TruvideoSdkCamera {
    private var cameraListener: TruvideoSdkCameraCallback? = null
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private var activity: ComponentActivity? = null

    override fun init(activity: ComponentActivity) {
        this.activity = activity

        this.startForResult =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val intent = result.data

                    val data: ArrayList<String> = intent?.getStringArrayListExtra("media") ?: ArrayList()
                    val media = data.map { TruvideoSdkCameraMedia.fromJson(it) }.toList()
                    media.forEach {
                        Log.d("CameraService", "Media: ${it.toJson()}")
                    }
                    cameraListener?.onResult(media)
                } else {
                    cameraListener?.onResult(listOf())
                }
            }
    }

    private fun  validateAuth(){
        val isAuthenticated = sdk_common.auth.isAuthenticated.value
        if (!isAuthenticated) {
            throw TruvideoSdkAuthenticationRequiredException()
        }

        val isInitialized = !sdk_common.auth.isInitialized.value
        if (!isInitialized) {
            throw TruvideoSdkNotInitializedException()
        }
    }

    override fun start(
        configuration: TruvideoSdkCameraConfiguration?,
        callback: TruvideoSdkCameraCallback
    ) {
        validateAuth()

        if (activity == null) {
            throw TruvideoSdkException("You have to call 'init' first")
        }

        this.cameraListener = callback

        val intent = Intent(activity, CameraActivity::class.java)
        intent.putExtra(
            "configuration",
            (configuration ?: TruvideoSdkCameraConfiguration()).toJson()
        )
        startForResult.launch(intent)
    }

    override fun getDefaultOutputUri(context: Context): Uri {
        validateAuth()

        val file = File("${context.filesDir.path}/truvideo-sdk/camera")
        return Uri.fromFile(file)
    }

    override fun getInformation(context: Context): TruvideoSdkCameraInformation {
        validateAuth()

        val manipulateResolutionsUseCase = ManipulateResolutionsUseCase()
        return GetCameraInformationUseCase(manipulateResolutionsUseCase)(context)
    }

    override val environment: String
        get() = BuildConfig.FLAVOR

}
