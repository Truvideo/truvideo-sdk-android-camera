package com.truvideo.sdk.camera.examples;

import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.truvideo.sdk.camera.TruvideoSdkCamera;
import com.truvideo.sdk.camera.exceptions.TruvideoSdkCameraException;
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraCallback;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMode;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution;
import com.truvideo.sdk.camera.usecase.TruvideoSdkCameraScreen;

import java.util.ArrayList;
import java.util.List;

class InitJavaActivity extends ComponentActivity {

    private TruvideoSdkCameraScreen cameraScreen = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraScreen = TruvideoSdkCamera.getInstance().initCameraScreen(this);
    }

    private void openCameraScreen() {
        if (cameraScreen == null) return;

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
        TruvideoSdkCameraMode mode = TruvideoSdkCameraMode.VIDEO_AND_PICTURE;
        // TruvideoSdkCameraMode mode = TruvideoSdkCameraMode.VIDEO;
        // TruvideoSdkCameraMode mode = TruvideoSdkCameraMode.PICTURE;

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

        cameraScreen.open(null, new TruvideoSdkCameraCallback<List<TruvideoSdkCameraMedia>>() {
            @Override
            public void onComplete(List<TruvideoSdkCameraMedia> result) {
                // Handle result
            }

            @Override
            public void onError(@NonNull TruvideoSdkCameraException exception) {
                // Handle error
                exception.printStackTrace();
            }
        });
    }
}
