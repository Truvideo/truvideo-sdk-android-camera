package com.truvideo.sdk.camera.interfaces

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation

interface TruvideoSdkCamera {

    /**
     * Initializes the camera functionality within the Truvideo SDK.
     *
     * @param activity The parent ComponentActivity where camera operations will be performed.
     */
    fun init(activity: ComponentActivity)

    /**
     * Starts the camera for capturing media and invokes the provided callback with the camera result.
     *
     * @param configuration A set of camera default values.
     * @param callback A callback function to handle the camera result, which includes captured media.
     */
    fun start(
        configuration: TruvideoSdkCameraConfiguration? = null,
        callback: TruvideoSdkCameraCallback
    )

    fun getDefaultOutputUri(context: Context): Uri

    fun getInformation(context: Context): TruvideoSdkCameraInformation

    val environment: String
}