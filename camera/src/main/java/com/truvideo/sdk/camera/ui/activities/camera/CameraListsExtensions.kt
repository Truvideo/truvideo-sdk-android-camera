package com.truvideo.sdk.camera.ui.activities.camera

import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution


infix fun List<TruvideoSdkCameraResolution>.intersection(other: List<TruvideoSdkCameraResolution>): List<TruvideoSdkCameraResolution> {
    return other.filter { picked ->
        this.any { it.width == picked.width && it.height == picked.height }
    }
}

infix fun List<TruvideoSdkCameraResolution>.intersectionOrOriginal(other: List<TruvideoSdkCameraResolution>): List<TruvideoSdkCameraResolution> {
    if (other.isEmpty()) return this
    return this intersection other
}

infix fun Pair<List<TruvideoSdkCameraResolution>, List<TruvideoSdkCameraResolution>>.intersectOrDefault(default: TruvideoSdkCameraResolution?): TruvideoSdkCameraResolution {
    if (first.isEmpty()) return first.first()
    val inter = (first intersectionOrOriginal second)
    if (default != null && default in inter) {  return default }
    return inter.first()
}