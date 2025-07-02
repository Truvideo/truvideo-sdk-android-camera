package com.truvideo.sdk.camera.model

import kotlinx.serialization.Serializable

@Serializable
enum class TruvideoSdkScannerCodeFormat {
    CODE_39,
    CODE_QR,
    CODE_93,
    DATA_MATRIX;

    companion object  {
        val all = listOf(CODE_39, CODE_93, CODE_QR, DATA_MATRIX)
    }
}