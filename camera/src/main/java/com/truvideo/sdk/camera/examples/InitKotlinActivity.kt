package com.truvideo.sdk.camera.examples

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.truvideo.sdk.camera.TruvideoSdkCamera
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.usecase.TruvideoSdkCameraScreen

class InitKotlinActivity : ComponentActivity() {

    private var cameraScreen: TruvideoSdkCameraScreen? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraScreen = TruvideoSdkCamera.initCameraScreen(this)
    }

    suspend fun openCameraScreen() {
        val cameraInfo = TruvideoSdkCamera.getInformation()

        // you can choose the default camera lens facing
        // options: Back, Front
        val lensFacing = TruvideoSdkCameraLensFacing.BACK
        // TruvideoSdkCameraLensFacing lensFacing = TruvideoSdkCameraLensFacing.FRONT;

        // You can choose if the flash is on or off by default
        val flashMode = TruvideoSdkCameraFlashMode.OFF
        // val flashMode = TruvideoSdkCameraFlashMode.ON

        // You can choose the camera orientation
        // Options: null, Portrait, LandscapeLeft, LandscapeRight, PortraitReverse
        // Null means any orientation
        val orientation: TruvideoSdkCameraOrientation? = null
        // TruvideoSdkCameraOrientation orientation = TruvideoSdkCameraOrientation.PORTRAIT;
        // TruvideoSdkCameraOrientation orientation = TruvideoSdkCameraOrientation.LANDSCAPE_LEFT;
        // TruvideoSdkCameraOrientation orientation = TruvideoSdkCameraOrientation.LANDSCAPE_RIGHT;
        // TruvideoSdkCameraOrientation orientation = TruvideoSdkCameraOrientation.PORTRAIT_REVERSE;

        // You can choose where the files will be saved
        val outputPath = applicationContext.filesDir.path + "/camera"

        // You can decide the list of allowed resolutions for the front camera
        // if you send an empty list, all the resolutions are allowed
        var frontResolutions: List<TruvideoSdkCameraResolution> = ArrayList()
        if (cameraInfo.frontCamera != null) {
            // if you don't want to decide the list of allowed resolutions, you can send all the resolutions or an empty list
            frontResolutions = cameraInfo.frontCamera.resolutions

            //frontResolutions = new ArrayList<>();

            // Example of how to allow only the one resolution
            // List<TruvideoSdkCameraResolution> resolutions = new ArrayList<>();
            // resolutions.add(cameraInfo.getFrontCamera().getResolutions().get(0));
            // frontResolutions = resolutions;
        }


        // You can decide the default resolution for the front camera
        var frontResolution: TruvideoSdkCameraResolution? = null
        if (cameraInfo.frontCamera != null) {
            // Example of how tho pick the first resolution as the default one
            val resolutions = cameraInfo.frontCamera.resolutions
            if (resolutions.isNotEmpty()) {
                frontResolution = resolutions[0]
            }
        }

        val backResolutions: List<TruvideoSdkCameraResolution> = ArrayList()
        val backResolution: TruvideoSdkCameraResolution? = null


        // You can decide the mode of the camera
        // Options: video and picture, video, picture
        val mode = TruvideoSdkCameraMode.VIDEO_AND_PICTURE

        // TruvideoSdkCameraMode mode = TruvideoSdkCameraMode.VIDEO;
        // TruvideoSdkCameraMode mode = TruvideoSdkCameraMode.PICTURE;
        val configuration = TruvideoSdkCameraConfiguration(
            lensFacing = lensFacing,
            flashMode = flashMode,
            orientation = orientation,
            outputPath = outputPath,
            frontResolutions = frontResolutions,
            frontResolution = frontResolution,
            backResolutions = backResolutions,
            backResolution = backResolution,
            mode = mode
        )

        val result = cameraScreen?.open(configuration)
        // Handle result
    }
}