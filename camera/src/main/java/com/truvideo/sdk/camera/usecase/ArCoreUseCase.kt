package com.truvideo.sdk.camera.usecase

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.ar.core.ArCoreApk
import truvideo.sdk.common.exceptions.TruvideoSdkException

class ArCoreUseCase(val context: Context) {

    val isSupported: Boolean
        get() {
            try {
                val status = ArCoreApk.getInstance().checkAvailability(context)
                Log.d("TruvideoSdkCamera", "ArCoreStatus: $status")

                return when (status) {
                    ArCoreApk.Availability.UNKNOWN_ERROR -> false
                    ArCoreApk.Availability.UNKNOWN_CHECKING -> true
                    ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> true
                    ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> false
                    ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> true
                    ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> true
                    ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
                }
            }catch (e: Exception){
                e.printStackTrace()
                return false
            }
        }

    val isInstalled: Boolean
        get() {
            try {
                if (!isSupported) {
                    return false
                }

                val status = ArCoreApk.getInstance().checkAvailability(context)
                return status == ArCoreApk.Availability.SUPPORTED_INSTALLED
            }catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

    fun requestInstall(activity: Activity) {
        if (!isSupported) {
            throw TruvideoSdkException(message = "Augmented reality not supported")
        }

        val status = ArCoreApk.getInstance().checkAvailability(context)
        if (status == ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            return
        }

        ArCoreApk.getInstance().requestInstall(activity, true)
    }
}