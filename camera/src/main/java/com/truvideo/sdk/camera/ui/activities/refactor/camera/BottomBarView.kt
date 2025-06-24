package com.truvideo.sdk.camera.ui.activities.refactor.camera

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEvent
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.isIdle
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.isPaused
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.isRecording
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.CameraPreviewViewModel
import com.truvideo.sdk.camera.ui.components.capture_button.CaptureButton
import com.truvideo.sdk.camera.ui.components.pause_button.PauseButton
import com.truvideo.sdk.camera.ui.components.rotate_button.RotateButton
import com.truvideo.sdk.components.animated_opacity.TruvideoAnimatedOpacity
import com.truvideo.sdk.components.animated_rotation.TruvideoAnimatedRotation
import com.truvideo.sdk.components.button.TruvideoIconButton

@Composable
internal fun BottomBarView(
    viewModel: CameraPreviewViewModel
) {

    val orientation by viewModel.orientation.collectAsState()
    val captureState by viewModel.captureState.collectAsState()
    val controlsState by viewModel.controlsState.collectAsState()

    val takeImageButtonVisible = controlsState.isTakingPictureButtonVisible
    val isTakeImageButtonEnabled = controlsState.isTakingPictureButtonEnabled
    val isCaptureButtonEnabled = controlsState.isCaptureButtonEnabled
    val isPauseButtonEnabled = controlsState.isPauseButtonEnabled
    val isRotationButtonEnabled = controlsState.isLensFacingRotationButtonEnabled

    val isRecording = remember(captureState.recordingConfig) { !captureState.recordingConfig.recordingState.isIdle()  }
    val isPaused = remember(captureState.recordingConfig) { captureState.recordingConfig.recordingState.isPaused()  }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {


        Spacer(modifier = Modifier.weight(1f))

        TruvideoAnimatedOpacity(
            opacity = if (takeImageButtonVisible) 1f else 0f
        ) {
            TruvideoAnimatedRotation(
                rotation = orientation.uiRotation,
            ) {
                TruvideoIconButton(
                    icon = Icons.Outlined.CameraAlt,
                    size = 50f,
                    enabled = isTakeImageButtonEnabled,
                    onPressed = {
                        viewModel.onEvent(CameraUiEvent.Media.OnTakeImageButtonPressed)
                    },
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
//
        // Recording
        CaptureButton(
            recording = isRecording,
            enabled = isCaptureButtonEnabled,
            onPressed = {
                viewModel.onEvent(CameraUiEvent.Media.OnCapturedButtonPressed)
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        AnimatedContent(
            targetState = isRecording,
            label = ""
        ) { isRecordingTarget ->
            // Pause or play
            if (isRecordingTarget) {
                PauseButton(
                    isPaused = isPaused,
                    size = 50f,
                    enabled = isPauseButtonEnabled,
                    rotation = orientation.uiRotation,
                    onPressed = {
                        viewModel.onEvent(CameraUiEvent.Media.OnPauseButtonPressed)
                    },
                )
            } else {
                RotateButton(
                    size = 50f,
                    enabled = isRotationButtonEnabled,
                    rotation = orientation.uiRotation,
                    onPressed = {
                        viewModel.onEvent(CameraUiEvent.Controls.OnFlipCameraLensFacingButtonPressed)
                    },
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}