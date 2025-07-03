package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import androidx.compose.ui.unit.IntOffset
import com.truvideo.sdk.camera.adapters.FocusState
import com.truvideo.sdk.camera.model.TruvideoSdkCameraEvent
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia

sealed interface CameraUiEffect {
    object ClosePreview : CameraUiEffect
    data class SendEvent(val event: TruvideoSdkCameraEvent) : CameraUiEffect
    data class ClosePreviewWithResult(val media: List<TruvideoSdkCameraMedia>): CameraUiEffect
    data class ShowFocusIndicator(val touchPosition: IntOffset, val focusState: FocusState) : CameraUiEffect
    object ReportAuthenticationError : CameraUiEffect
    object ReportProperlyAuthenticated : CameraUiEffect
    data class ShowToastMessage(val msg: String) : CameraUiEffect
    data object DismissToast : CameraUiEffect
}

