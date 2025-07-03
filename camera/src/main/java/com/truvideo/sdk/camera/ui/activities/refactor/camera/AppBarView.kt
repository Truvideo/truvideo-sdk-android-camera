package com.truvideo.sdk.camera.ui.activities.refactor.camera

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Hd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.images
import com.truvideo.sdk.camera.model.videos
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEvent
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.CameraPreviewViewModel
import com.truvideo.sdk.camera.ui.components.media_count_indicator.MediaCountIndicator
import com.truvideo.sdk.components.animated_rotation.TruvideoAnimatedRotation
import com.truvideo.sdk.components.button.TruvideoContinueButton
import com.truvideo.sdk.components.button.TruvideoIconButton

@Composable
internal fun AppBarView(
    viewModel: CameraPreviewViewModel
) {
    val orientation by viewModel.orientation.collectAsState()
    val mediaState by viewModel.mediaState.collectAsState()
    val videoCount = remember(mediaState.media) { mediaState.media.videos.size }
    val imageCount = remember(mediaState.media) { mediaState.media.images.size }
    val controlsState by viewModel.controlsState.collectAsState()
    val shouldShowResolutionsButton = controlsState.isResolutionsButtonVisible
    val shouldShowFlash = controlsState.isFlashButtonVisible
    val isResolutionPanelButtonEnabled = controlsState.isResolutionsButtonEnabled
    val isFlashButtonEnabled = controlsState.isFlashButtonEnabled
    val isFlashButtonSelected by viewModel.flashButtonSelected.collectAsState()
    val isMediaCounterButtonEnabled = controlsState.isMediaCounterButtonEnabled
    val isCloseButtonVisible = controlsState.isContinueButtonVisible
    val isCloseButtonEnabled = controlsState.isContinueButtonEnabled
    val cameraMode by viewModel.cameraMode.collectAsState()
    val shouldShowMediaCountPortrait = (orientation == TruvideoSdkCameraOrientation.PORTRAIT)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TruvideoAnimatedRotation(
            rotation = orientation.uiRotation,
        ) {
            TruvideoIconButton(
                icon = Icons.Default.Close,
                small = true,
                enabled = true,
                onPressed = {
                    viewModel.onEvent(CameraUiEvent.Controls.ClosePreview)
                }
            )
        }

        // Media count indicator
        AnimatedContent(
            targetState = shouldShowMediaCountPortrait, label = ""
        ) { isPortraitTarget ->
            if (isPortraitTarget) {
                Row {
                    Box(modifier = Modifier.width(4.dp))

                    MediaCountIndicator(
                        videoCount = videoCount,
                        imageCount = imageCount,
                        mode = cameraMode,
                        enabled = isMediaCounterButtonEnabled,
                        onPressed = {
                            viewModel.onEvent(CameraUiEvent.UI.OnMediaCounterButtonPressed)
                        },
                    )
                }
            } else {
                Box(modifier = Modifier.height(30.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Resolutions button
        AnimatedContent(
            targetState = shouldShowResolutionsButton,
            label = ""
        ) { buttonVisibleTarget ->
            if (buttonVisibleTarget) {
                Row {
                    Box(modifier = Modifier.width(4.dp))

                    TruvideoAnimatedRotation(
                        rotation = orientation.uiRotation,
                    ) {
                        TruvideoIconButton(
                            icon = Icons.Default.Hd,
                            small = true,
                            enabled = isResolutionPanelButtonEnabled,
                            onPressed = {
                                viewModel.onEvent(CameraUiEvent.UI.OnResolutionsButtonPressed)
                            }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.height(30.dp))
            }
        }

        // Flash button
        AnimatedContent(
            targetState = shouldShowFlash,
            label = ""
        ) { shouldShowFlashTarget ->
            if (shouldShowFlashTarget) {
                Row {
                    Box(modifier = Modifier.width(4.dp))
                    TruvideoAnimatedRotation(
                        rotation = orientation.uiRotation,
                    ) {
                        TruvideoIconButton(
                            icon = Icons.Default.FlashOn,
                            small = true,
                            enabled = isFlashButtonEnabled,
                            onPressed = {
                                viewModel.onEvent(CameraUiEvent.Controls.OnFlashButtonPressed)
                            },
                            selected = isFlashButtonSelected
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.height(30.dp))
            }
        }

        val isPortraitButtonVisible = isCloseButtonVisible && orientation.isPortrait

        AnimatedContent(
            targetState = isPortraitButtonVisible, label = ""
        ) { portraitContinueButtonVisibleTarget ->
            if (portraitContinueButtonVisibleTarget) {
                Row {
                    Box(modifier = Modifier.width(4.dp))
                    TruvideoContinueButton(
                        small = true,
                        enabled = isCloseButtonEnabled,
                        onPressed = {
                            viewModel.onEvent(CameraUiEvent.UI.OnContinueButtonPressed)
                        })
                }
            } else {
                Box(modifier = Modifier.height(30.dp))
            }
        }
    }

}