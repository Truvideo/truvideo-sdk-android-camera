package com.truvideo.sdk.camera.utils

import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.camera2.params.MeteringRectangle

fun getFocusRectangle(
    touchX : Float,
    touchY : Float,
    viewWidth: Float,
    viewHeight: Float,
    sensorSize: Rect,
    orientation: Int,
) : MeteringRectangle {

    val percentage = 0.1f
    val halfMeteringRectWidth = (percentage * sensorSize.width()) / 2
    val halfMeteringRectHeight = (percentage * sensorSize.height()) / 2

    // Normalize the [x,y] touch point in the view port to values in the range of [0,1]
    val normalizedPoint = floatArrayOf(touchX / viewHeight, touchY /viewWidth.toFloat())

    // Scale and rotate the normalized point such that it maps to the sensor region
    Matrix().apply {
        postRotate(-orientation.toFloat(), 0.5f, 0.5f)
        postScale(sensorSize.width().toFloat(), sensorSize.height().toFloat())
        mapPoints(normalizedPoint)
    }

    val meteringRegion = Rect(
        (normalizedPoint[0] - halfMeteringRectWidth).toInt().coerceIn(0, sensorSize.width()),
        (normalizedPoint[1] - halfMeteringRectHeight).toInt().coerceIn(0, sensorSize.height()),
        (normalizedPoint[0] + halfMeteringRectWidth).toInt().coerceIn(0, sensorSize.width()),
        (normalizedPoint[1] + halfMeteringRectHeight).toInt().coerceIn(0, sensorSize.height())
    )

    return MeteringRectangle(meteringRegion, MeteringRectangle.METERING_WEIGHT_MAX)

}

fun getZoomRectangle(zoomLevel: Float, sensorSize: Rect): Rect {
    val centerX = sensorSize.width() / 2
    val centerY = sensorSize.height() / 2
    val deltaX = ((0.5f * sensorSize.width()) / zoomLevel).toInt()
    val deltaY = ((0.5f * sensorSize.height()) / zoomLevel).toInt()
    return Rect(centerX - deltaX, centerY - deltaY, centerX + deltaX, centerY + deltaY);
}