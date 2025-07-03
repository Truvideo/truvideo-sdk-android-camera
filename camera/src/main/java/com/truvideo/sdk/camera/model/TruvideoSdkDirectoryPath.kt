package com.truvideo.sdk.camera.model

sealed class TruvideoSdkDirectory {
    data class Files(
        val prefix: String = "",
        val temp: Boolean = false
    ) : TruvideoSdkDirectory()
    data class Cache(
        val prefix: String = "",
        val temp: Boolean = false
    ) : TruvideoSdkDirectory()
    data class Custom(val path: String) : TruvideoSdkDirectory()

    fun path() : String {
        return  when(this) {
            is Files -> this.prefix + (if (this.temp) "/truvideo-sdk/camera/temp" else "/truvideo-sdk/camera/")
            is Cache -> this.prefix + (if (this.temp) "/truvideo-sdk/camera/temp" else "/truvideo-sdk/camera/")
            is Custom -> path
        }
    }
}