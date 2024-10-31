package com.truvideo.sdk.camera.usecase

import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution

class ManipulateResolutionsUseCase {
    fun sort(sizes: List<TruvideoSdkCameraResolution>): List<TruvideoSdkCameraResolution> {
        return sizes.sortedWith(compareBy { it.height * it.width }).reversed().toList()
    }

    fun filter(items: List<TruvideoSdkCameraResolution>, picked: List<TruvideoSdkCameraResolution>): List<TruvideoSdkCameraResolution> {
        val result = mutableListOf<TruvideoSdkCameraResolution>()

        picked.forEach { p ->
            if (items.any { res -> res.width == p.width && res.height == p.height }) {
                result.add(p)
            }
        }

        return result
    }

    fun contain(items: List<TruvideoSdkCameraResolution>, item: TruvideoSdkCameraResolution): Boolean {
        return items.any { res -> res.width == item.width && res.height == item.height }
    }
}