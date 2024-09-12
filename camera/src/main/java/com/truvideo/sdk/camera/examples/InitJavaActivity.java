package com.truvideo.sdk.camera.examples;

import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.truvideo.sdk.camera.TruvideoSdkCamera;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraEvent;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMode;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution;
import com.truvideo.sdk.camera.ui.activities.camera.TruvideoSdkCameraContract;

import java.util.ArrayList;
import java.util.List;

class InitJavaActivity extends ComponentActivity {

    private ActivityResultLauncher<TruvideoSdkCameraConfiguration> cameraScreenLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraScreenLauncher = registerForActivityResult(
                new TruvideoSdkCameraContract(),
                result -> {
                    // Handle result
                    if (result == null) return;

                    for (int i = 0; i < result.size(); i++) {
                        TruvideoSdkCameraMedia item = result.get(i);
                    }
                }
        );

        TruvideoSdkCamera.getInstance().getEvents().observe(this, new Observer<TruvideoSdkCameraEvent>() {
            @Override
            public void onChanged(TruvideoSdkCameraEvent event) {
            }
        });
    }


    private void openCameraScreen() {
        TruvideoSdkCameraInformation cameraInfo = TruvideoSdkCamera.getInstance().getInformation();

        // you can choose the default camera lens facing
        // options: Back, Front
        TruvideoSdkCameraLensFacing lensFacing = TruvideoSdkCameraLensFacing.BACK;
        // TruvideoSdkCameraLensFacing lensFacing = TruvideoSdkCameraLensFacing.FRONT;

        // you can choose if the flash its enabled or not by default
        TruvideoSdkCameraFlashMode flashMode = TruvideoSdkCameraFlashMode.OFF;
        // TruvideoSdkCameraFlashMode flashMode = TruvideoSdkCameraFlashMode.ON;

        TruvideoSdkCameraOrientation orientation = null;
        // TruvideoSdkCameraOrientation orientation = TruvideoSdkCameraOrientation.PORTRAIT;
        // TruvideoSdkCameraOrientation orientation = TruvideoSdkCameraOrientation.LANDSCAPE_LEFT;
        // TruvideoSdkCameraOrientation orientation = TruvideoSdkCameraOrientation.LANDSCAPE_RIGHT;
        // TruvideoSdkCameraOrientation orientation = TruvideoSdkCameraOrientation.PORTRAIT_REVERSE;

        String outputPath = getApplicationContext().getFilesDir().getPath() + "/camera";

        // You can decide the list of allowed resolutions for the front camera
        // if you send an empty list, all the resolutions are allowed
        List<TruvideoSdkCameraResolution> frontResolutions = new ArrayList<>();
        if (cameraInfo.getFrontCamera() != null) {
            // if you don't want to decide the list of allowed resolutions, you can send all the resolutions or an empty list
            frontResolutions = cameraInfo.getFrontCamera().getResolutions();
            //frontResolutions = new ArrayList<>();

            // Example of how to allow only the one resolution
            // List<TruvideoSdkCameraResolution> resolutions = new ArrayList<>();
            // resolutions.add(cameraInfo.getFrontCamera().getResolutions().get(0));
            // frontResolutions = resolutions;
        }

        // You can decide the default resolution for the front camera
        TruvideoSdkCameraResolution frontResolution = null;
        if (cameraInfo.getFrontCamera() != null) {
            // Example of how tho pick the first resolution as the default one
            List<TruvideoSdkCameraResolution> resolutions = cameraInfo.getFrontCamera().getResolutions();
            if (!resolutions.isEmpty()) {
                frontResolution = resolutions.get(0);
            }
        }

        List<TruvideoSdkCameraResolution> backResolutions = new ArrayList<>();
        TruvideoSdkCameraResolution backResolution = null;

        // You can decide the mode of the camera
        // Options: video and picture, video, picture
        TruvideoSdkCameraMode mode = TruvideoSdkCameraMode.videoAndPicture();
//        TruvideoSdkCameraMode mode = TruvideoSdkCameraMode.videoAndPicture(
//                5, // max video count
//                5, // max picture count
//                1000 * 60 * 60 // max video duration
//        );
//                TruvideoSdkCameraMode mode = TruvideoSdkCameraMode.video();
//        TruvideoSdkCameraMode mode = TruvideoSdkCameraMode.video(
//                5, // max count,
//                1000 * 60 * 60 // max duration
//        );
//        TruvideoSdkCameraMode mode = TruvideoSdkCameraMode.picture();
//        TruvideoSdkCameraMode mode = TruvideoSdkCameraMode.picture(
//                5 // Max count
//        );
//        TruvideoSdkCameraMode mode = TruvideoSdkCameraMode.singleVideo();
//        TruvideoSdkCameraMode mode = TruvideoSdkCameraMode.singlePicture();

        TruvideoSdkCameraConfiguration configuration = new TruvideoSdkCameraConfiguration(
                lensFacing,
                flashMode,
                orientation,
                outputPath,
                frontResolutions,
                frontResolution,
                backResolutions,
                backResolution,
                mode
        );

        cameraScreenLauncher.launch(configuration);
    }
}
