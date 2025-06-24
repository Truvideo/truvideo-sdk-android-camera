package com.truvideo.sdk.camera.adapters

import android.graphics.ImageFormat
import android.media.ImageReader
import android.os.Handler

internal fun ImageReader?.configureImageCapture(
    width: Int,
    height: Int,
    format: Int = ImageFormat.JPEG,
    maxImages: Int = 10,
    imageAvailableListener: ImageReader.OnImageAvailableListener? = null,
    handler: Handler
) : ImageReader  {
//    this?.close()
    return ImageReader.newInstance(width, height, format, maxImages).apply {
        setOnImageAvailableListener(imageAvailableListener, handler)
    }
}