@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.truvideo.sdk.camera.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class TruvideoSdkCameraMode private constructor(
    val videoLimit: Int? = null,
    val imageLimit: Int? = null,
    val mediaLimit: Int? = null,
    val videoDurationLimit: Int? = null,
    val autoClose: Boolean = false
) {
    val canTakeVideo: Boolean
        get() {
            if (mediaLimit == 0) return false
            if (videoLimit == 0) return false
            return true
        }

    val canTakeImage: Boolean
        get() {
            if (mediaLimit == 0) return false
            if (imageLimit == 0) return false
            return true
        }

    val isSingleMediaMode : Boolean get() = mediaLimit == 1

    fun toJson(): String = Json.encodeToString(this)

    companion object {

        @JvmStatic
        fun fromJson(json: String): TruvideoSdkCameraMode {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }

        @JvmStatic
        fun videoAndImage() = TruvideoSdkCameraMode()

        @JvmStatic
        fun videoAndImage(
            durationLimit: Int? = null
        ) = TruvideoSdkCameraMode(
            videoDurationLimit = durationLimit
        )

        @JvmStatic
        fun videoAndImage(
            maxCount: Int? = null,
            durationLimit: Int? = null
        ) = TruvideoSdkCameraMode(
            mediaLimit = maxCount,
            videoDurationLimit = durationLimit
        )

        @JvmStatic
        fun videoAndImage(
            videoMaxCount: Int? = null,
            imageMaxCount: Int? = null,
            durationLimit: Int? = null,
        ) = TruvideoSdkCameraMode(
            videoLimit = videoMaxCount,
            imageLimit = imageMaxCount,
            videoDurationLimit = durationLimit
        )

        @JvmStatic
        fun singleVideo() = TruvideoSdkCameraMode(
            imageLimit = 0,
            videoLimit = 1,
            autoClose = true
        )

        @JvmStatic
        fun singleVideo(
            durationLimit: Int? = null
        ) = TruvideoSdkCameraMode(
            imageLimit = 0,
            videoLimit = 1,
            videoDurationLimit = durationLimit,
            autoClose = true
        )

        @JvmStatic
        fun singleImage() = TruvideoSdkCameraMode(
            videoLimit = 0,
            imageLimit = 1,
            autoClose = true
        )

        @JvmStatic
        fun singleVideoOrImage() = TruvideoSdkCameraMode(
            mediaLimit = 1,
            autoClose = true
        )

        @JvmStatic
        fun singleVideoOrImage(
            durationLimit: Int? = null
        ) = TruvideoSdkCameraMode(
            mediaLimit = 1,
            videoDurationLimit = durationLimit,
            autoClose = true
        )

        @JvmStatic
        fun video() = TruvideoSdkCameraMode(
            imageLimit = 0
        )


        @JvmStatic
        fun video(
            maxCount: Int? = null,
        ) = TruvideoSdkCameraMode(
            videoLimit = maxCount,
            imageLimit = 0,
        )

        @JvmStatic
        fun video(
            maxCount: Int? = null,
            durationLimit: Int? = null
        ) = TruvideoSdkCameraMode(
            videoLimit = maxCount,
            imageLimit = 0,
            videoDurationLimit = durationLimit
        )

        @JvmStatic
        fun image() = TruvideoSdkCameraMode(
            videoLimit = 0
        )

        @JvmStatic
        fun image(
            maxCount: Int? = null
        ) = TruvideoSdkCameraMode(
            videoLimit = 0,
            imageLimit = maxCount
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TruvideoSdkCameraMode) return false

        if (videoLimit != other.videoLimit) return false
        if (imageLimit != other.imageLimit) return false
        if (mediaLimit != other.mediaLimit) return false
        if (videoDurationLimit != other.videoDurationLimit) return false
        if (autoClose != other.autoClose) return false

        return true
    }

    override fun hashCode(): Int {
        var result = videoLimit?.hashCode() ?: 0
        result = 31 * result + (imageLimit?.hashCode() ?: 0)
        result = 31 * result + (mediaLimit?.hashCode() ?: 0)
        result = 31 * result + (videoDurationLimit?.hashCode() ?: 0)
        result = 31 * result + autoClose.hashCode()
        return result
    }
}