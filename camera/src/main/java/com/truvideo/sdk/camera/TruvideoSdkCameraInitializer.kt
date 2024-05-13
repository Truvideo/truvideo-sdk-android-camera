package com.truvideo.sdk.camera

import android.content.Context
import androidx.startup.Initializer
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraAuthAdapterImpl
import com.truvideo.sdk.camera.adapters.VersionPropertiesAdapter

@Suppress("unused")
class TruvideoSdkCameraInitializer : Initializer<Unit> {

    companion object {
        fun init(context: Context) {

            val versionPropertiesAdapter = VersionPropertiesAdapter(context)
            val authAdapter = TruvideoSdkCameraAuthAdapterImpl(
                versionPropertiesAdapter = versionPropertiesAdapter
            )

            TruvideoSdkCamera = TruvideoSdkCameraImpl(
                authAdapter = authAdapter
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