package com.truvideo.sdk.camera.usecase

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.CamcorderProfile
import android.os.Build
import android.util.Size
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution

internal class GetCameraInformationUseCase(
    private val context: Context,
    private val manipulateResolutionsUseCase: ManipulateResolutionsUseCase,
) {

    operator fun invoke(): TruvideoSdkCameraInformation {
        val backCamera = setMainCamera(CameraCharacteristics.LENS_FACING_BACK)
        val frontCamera = setMainCamera(CameraCharacteristics.LENS_FACING_FRONT)

        return TruvideoSdkCameraInformation(
            frontCamera = frontCamera, backCamera = backCamera
        )
    }

    private fun setMainCamera(lensFacingType: Int): TruvideoSdkCameraDevice? {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val ids: Array<String> = cameraManager.cameraIdList

        // Step 1: Build the list of cameras
        val cameras = ids.mapNotNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val cameraLensFacing =
                characteristics.get(CameraCharacteristics.LENS_FACING) ?: return@mapNotNull null

            // Skip if it doesn't match the lens type
            if (cameraLensFacing != lensFacingType) return@mapNotNull null

            val resolutions = listSupportedVideoResolutions(id)
            val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
            val isTapToFocusEnabled =
                (characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) ?: 0) >= 1
            val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

            val isLogicalCamera = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val capabilities =
                    characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) == true
            } else {
                false
            }

            val lensFacing = when (cameraLensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> TruvideoSdkCameraLensFacing.FRONT
                CameraCharacteristics.LENS_FACING_BACK -> TruvideoSdkCameraLensFacing.BACK
                else -> null
            } ?: return@mapNotNull null

            TruvideoSdkCameraDevice(
                id = id,
                lensFacing = lensFacing,
                resolutions = resolutions,
                withFlash = hasFlash,
                isTapToFocusEnabled = isTapToFocusEnabled,
                sensorOrientation = sensorOrientation,
                isLogicalCamera = isLogicalCamera
            )
        }

        // Step 2: Select the main camera
        return when {
            // Priority 1: Logical camera
            cameras.any { it.isLogicalCamera } -> cameras.first { it.isLogicalCamera }

            // Priority 2: Camera with flash
            cameras.any { it.withFlash } -> cameras.first { it.withFlash }

            // Priority 3: Camera with the highest resolution
            cameras.isNotEmpty() -> cameras.maxByOrNull { device ->
                device.resolutions.maxOfOrNull { it.width * it.height } ?: 0
            }

            else -> null // No cameras available
        }
    }

    private fun listSupportedVideoResolutions(cameraId: String): List<TruvideoSdkCameraResolution> {
        val qualities = listOf(
            CamcorderProfile.QUALITY_480P,
            CamcorderProfile.QUALITY_720P,
            CamcorderProfile.QUALITY_1080P,
        );

        val result = mutableListOf<TruvideoSdkCameraResolution>()

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

        return manipulateResolutionsUseCase.sort(result)
    }

}