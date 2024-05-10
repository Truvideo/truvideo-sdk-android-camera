@file:JvmName("TruvideoSdkCamera")

package com.truvideo.sdk.camera

import com.truvideo.sdk.camera.interfaces.TruvideoSdkCamera

@get:JvmName("getInstance")
val TruvideoSdkCamera: TruvideoSdkCamera = TruvideoSdkCameraImpl
