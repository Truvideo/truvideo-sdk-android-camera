package com.truvideo.sdk.camera.examples

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.truvideo.sdk.camera.TruvideoSdkCamera
import com.truvideo.sdk.camera.usecase.TruvideoSdkCameraScreen

class InitKotlinActivity : ComponentActivity() {

    private var cameraScreen: TruvideoSdkCameraScreen? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraScreen = TruvideoSdkCamera.initCameraScreen(this)
    }

    suspend fun openCameraScreen() {
        val result = cameraScreen?.open()
        // Handle result
    }
}