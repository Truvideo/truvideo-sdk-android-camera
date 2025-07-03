package com.truvideo.sdk.camera.adapters

import android.content.Context

interface TruvideoSdkFileManager {
    val filesDirectory: String
    val cacheDirectory: String
}


class TruvideoSdkFileManagerImpl (
    val context: Context
) : TruvideoSdkFileManager {
    override val filesDirectory: String = context.filesDir.path
    override val cacheDirectory: String = context.cacheDir.path
}