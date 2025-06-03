package com.truvideo.sdk.camera

import android.content.Context
import androidx.startup.Initializer
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraAuthAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraLogAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraVersionPropertiesAdapterImpl
import com.truvideo.sdk.camera.usecase.ArCoreUseCase
import com.truvideo.sdk.camera.usecase.GetCameraInformationUseCase
import com.truvideo.sdk.camera.usecase.ManipulateResolutionsUseCase

@Suppress("unused")
class TruvideoSdkCameraInitializer : Initializer<Unit> {

    companion object {
        fun init(context: Context) {
            val versionPropertiesAdapter = TruvideoSdkCameraVersionPropertiesAdapterImpl(
                context = context
            )
            val logAdapter = TruvideoSdkCameraLogAdapterImpl(
                versionPropertiesAdapter = versionPropertiesAdapter,
            )
            val authAdapter = TruvideoSdkCameraAuthAdapterImpl(
                versionPropertiesAdapter = versionPropertiesAdapter,
                logAdapter = logAdapter
            )
            val manipulateResolutionsUseCase = ManipulateResolutionsUseCase()
            val getCameraInformationUseCase = GetCameraInformationUseCase(
                context = context,
                manipulateResolutionsUseCase = manipulateResolutionsUseCase,
            )

            val arCoreUseCase = ArCoreUseCase(context)

            TruvideoSdkCamera = TruvideoSdkCameraImpl(
                context = context,
                authAdapter = authAdapter,
                getCameraInformationUseCase = getCameraInformationUseCase,
                logAdapter = logAdapter,
                arCoreUseCase = arCoreUseCase,
                versionPropertiesAdapter = versionPropertiesAdapter
            )
        }
    }

    override fun create(context: Context) {
        init(context)
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }
}