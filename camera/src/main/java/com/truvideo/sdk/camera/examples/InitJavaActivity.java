package com.truvideo.sdk.camera.examples;

import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.truvideo.sdk.camera.TruvideoSdkCamera;
import com.truvideo.sdk.camera.exceptions.TruvideoSdkCameraException;
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraCallback;
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia;
import com.truvideo.sdk.camera.usecase.TruvideoSdkCameraScreen;

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
