package com.truvideo.sdk.camera.interfaces

import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerCode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerValidationResult

interface TruvideoSdkCameraScannerValidation {

    fun validate(code: TruvideoSdkCameraScannerCode): TruvideoSdkCameraScannerValidationResult
}