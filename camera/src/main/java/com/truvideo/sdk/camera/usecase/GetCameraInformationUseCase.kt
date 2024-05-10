package com.truvideo.sdk.camera.usecase

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.CamcorderProfile
import android.util.Size
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution

internal class GetCameraInformationUseCase(private val manipulateResolutionsUseCase: ManipulateResolutionsUseCase) {

    operator fun invoke(context: Context): TruvideoSdkCameraInformation {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var backCamera: TruvideoSdkCameraDevice? = null
        var frontCamera: TruvideoSdkCameraDevice? = null

        val ids: Array<String> = cameraManager.cameraIdList

        for (id in ids) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val resolutions = listSupportedVideoResolutions(id)
            val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
            val isTapToFocusEnabled = (characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) ?: 0) >= 1
            val lensFacing = when (cameraLensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> TruvideoSdkCameraLensFacing.FRONT
                CameraCharacteristics.LENS_FACING_BACK -> TruvideoSdkCameraLensFacing.BACK
                else -> null
            } ?: continue

            val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

            val camera = TruvideoSdkCameraDevice(
                id = id,
                lensFacing = lensFacing,
                resolutions = resolutions,
                withFlash = hasFlash,
                isTapToFocusEnabled = isTapToFocusEnabled,
                sensorOrientation = sensorOrientation,
            )

            when (lensFacing) {
                TruvideoSdkCameraLensFacing.BACK -> backCamera = camera
                TruvideoSdkCameraLensFacing.FRONT -> frontCamera = camera
            }
        }

        return TruvideoSdkCameraInformation(
            frontCamera = frontCamera,
            backCamera = backCamera
        )
    }

    private fun listSupportedVideoResolutions(cameraId: String): List<TruvideoSdkCameraResolution> {
        val qualities = listOf(
            CamcorderProfile.QUALITY_QCIF,
            CamcorderProfile.QUALITY_QVGA,
            CamcorderProfile.QUALITY_480P,
            CamcorderProfile.QUALITY_720P,
            CamcorderProfile.QUALITY_1080P,
            CamcorderProfile.QUALITY_2160P,
            CamcorderProfile.QUALITY_LOW,
            CamcorderProfile.QUALITY_HIGH
        );

        var result = mutableListOf<TruvideoSdkCameraResolution>()

        qualities.forEach { quality ->
            if (CamcorderProfile.hasProfile(cameraId.toInt(), quality)) {
                val sizes = mutableListOf<Size>()

                val profile = CamcorderProfile.get(cameraId.toInt(), quality)
                sizes.add(Size(profile.videoFrameWidth, profile.videoFrameHeight))
                sizes.forEach { size ->
                    if (!result.any { r -> r.width == size.width && r.height == size.height }) {
                        result.add(TruvideoSdkCameraResolution(size.width, size.height))
                    }
                }
            }
        }

        result = result.filter { it.width <= 3000 && it.height <= 3000 }.toMutableList()
        return manipulateResolutionsUseCase.sort(result)
    }

}