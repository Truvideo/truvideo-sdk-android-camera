package com.truvideo.sdk.camera.interfaces

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import com.truvideo.sdk.camera.usecase.TruvideoSdkCameraScreen

interface TruvideoSdkCamera {

    /**
     * Initializes the camera functionality within the Truvideo SDK.
     *
     * @param activity The parent ComponentActivity where camera operations will be performed.
     */
    fun initCameraScreen(activity: ComponentActivity): TruvideoSdkCameraScreen

    fun getDefaultOutputUri(context: Context): Uri

    fun getInformation(): TruvideoSdkCameraInformation

    val environment: String
}