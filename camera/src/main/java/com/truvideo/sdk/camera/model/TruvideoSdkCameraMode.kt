@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.truvideo.sdk.camera.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class TruvideoSdkCameraMode private constructor(
    val videoLimit: Int? = null,
    val pictureLimit: Int? = null,
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

    val canTakePicture: Boolean
        get() {
            if (mediaLimit == 0) return false
            if (pictureLimit == 0) return false
            return true
        }

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
        fun videoAndPicture() = TruvideoSdkCameraMode()

        @JvmStatic
        fun videoAndPicture(
            durationLimit: Int? = null
        ) = TruvideoSdkCameraMode(
            videoDurationLimit = durationLimit
        )

        @JvmStatic
        fun videoAndPicture(
            maxCount: Int? = null,
            durationLimit: Int? = null
        ) = TruvideoSdkCameraMode(
            mediaLimit = maxCount,
            videoDurationLimit = durationLimit
        )

        @JvmStatic
        fun videoAndPicture(
            videoMaxCount: Int? = null,
            pictureMaxCount: Int? = null,
            durationLimit: Int? = null,
        ) = TruvideoSdkCameraMode(
            videoLimit = videoMaxCount,
            pictureLimit = pictureMaxCount,
            videoDurationLimit = durationLimit
        )

        @JvmStatic
        fun singleVideo() = TruvideoSdkCameraMode(
            pictureLimit = 0,
            videoLimit = 1,
            autoClose = true
        )

        @JvmStatic
        fun singleVideo(
            durationLimit: Int? = null
        ) = TruvideoSdkCameraMode(
            pictureLimit = 0,
            videoLimit = 1,
            videoDurationLimit = durationLimit,
            autoClose = true
        )

        @JvmStatic
        fun singlePicture() = TruvideoSdkCameraMode(
            videoLimit = 0,
            pictureLimit = 1,
            autoClose = true
        )

        @JvmStatic
        fun singleVideoOrPicture() = TruvideoSdkCameraMode(
            mediaLimit = 1,
            autoClose = true
        )

        @JvmStatic
        fun singleVideoOrPicture(
            durationLimit: Int? = null
        ) = TruvideoSdkCameraMode(
            mediaLimit = 1,
            videoDurationLimit = durationLimit,
            autoClose = true
        )

        @JvmStatic
        fun video() = TruvideoSdkCameraMode(
            pictureLimit = 0
        )


        @JvmStatic
        fun video(
            maxCount: Int? = null,
        ) = TruvideoSdkCameraMode(
            videoLimit = maxCount,
            pictureLimit = 0,
        )

        @JvmStatic
        fun video(
            maxCount: Int? = null,
            durationLimit: Int? = null
        ) = TruvideoSdkCameraMode(
            videoLimit = maxCount,
            pictureLimit = 0,
            videoDurationLimit = durationLimit
        )

        @JvmStatic
        fun picture() = TruvideoSdkCameraMode(
            videoLimit = 0
        )

        @JvmStatic
        fun picture(
            maxCount: Int? = null
        ) = TruvideoSdkCameraMode(
            videoLimit = 0,
            pictureLimit = maxCount
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TruvideoSdkCameraMode) return false

        if (videoLimit != other.videoLimit) return false
        if (pictureLimit != other.pictureLimit) return false
        if (mediaLimit != other.mediaLimit) return false
        if (videoDurationLimit != other.videoDurationLimit) return false
        if (autoClose != other.autoClose) return false

        return true
    }

    override fun hashCode(): Int {
        var result = videoLimit?.hashCode() ?: 0
        result = 31 * result + (pictureLimit?.hashCode() ?: 0)
        result = 31 * result + (mediaLimit?.hashCode() ?: 0)
        result = 31 * result + (videoDurationLimit?.hashCode() ?: 0)
        result = 31 * result + autoClose.hashCode()
        return result
    }
}