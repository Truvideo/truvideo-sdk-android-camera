package com.truvideo.sdk.camera.interfaces

import android.app.Activity
import androidx.lifecycle.LiveData
import com.truvideo.sdk.camera.model.TruvideoSdkCameraEvent
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation

interface TruvideoSdkCamera {

    fun getInformation(): TruvideoSdkCameraInformation

    val events: LiveData<TruvideoSdkCameraEvent>

    val environment: String

    val version: String

    val isAugmentedRealitySupported: Boolean

    val isAugmentedRealityInstalled: Boolean

    fun requestInstallAugmentedReality(activity: Activity)
}