package com.truvideo.sdk.camera.examples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.truvideo.sdk.camera.TruvideoSdkCamera
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.activities.camera.TruvideoSdkCameraContract

class InitKotlinActivity : ComponentActivity() {

    private lateinit var cameraScreenLauncher: ActivityResultLauncher<TruvideoSdkCameraConfiguration>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraScreenLauncher = registerForActivityResult(TruvideoSdkCameraContract()) { result: List<TruvideoSdkCameraMedia> ->
            // Handle result
        }
    }

    fun openCameraScreen() {
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
        val mode = TruvideoSdkCameraMode.videoAndPicture()
//        val mode = TruvideoSdkCameraMode.videoAndPicture(
//            videoMaxCount = 5,
//            pictureMaxCount = 5,
//            durationLimit = 1000 * 60 * 60
//        )
//        val mode = TruvideoSdkCameraMode.video()
//        val mode = TruvideoSdkCameraMode.video(
//            maxCount = 5,
//            durationLimit = 1000 * 60 * 60
//        )
//        val mode = TruvideoSdkCameraMode.picture()
//        val mode = TruvideoSdkCameraMode.picture(
//            maxCount = 5
//        )
//        val mode = TruvideoSdkCameraMode.singleVideo()
//        val mode = TruvideoSdkCameraMode.singlePicture()

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

        // Launch camera
        cameraScreenLauncher.launch(configuration)
    }
}