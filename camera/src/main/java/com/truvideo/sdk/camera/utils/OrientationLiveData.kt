/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.truvideo.sdk.camera.utils

import android.content.Context
import android.view.OrientationEventListener
import androidx.lifecycle.LiveData
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation


/**
 * Calculates closest 90-degree orientation to compensate for the device
 * rotation relative to sensor orientation, i.e., allows user to see camera
 * frames with the expected orientation.
 */
class OrientationLiveData(context: Context) : LiveData<TruvideoSdkCameraOrientation>() {

    private val listener = object : OrientationEventListener(context.applicationContext) {
        override fun onOrientationChanged(orientation: Int) {
            val rotation = when {
                orientation <= 45 -> TruvideoSdkCameraOrientation.PORTRAIT
                orientation <= 135 -> TruvideoSdkCameraOrientation.LANDSCAPE_RIGHT
                orientation <= 225 -> TruvideoSdkCameraOrientation.PORTRAIT_REVERSE
                orientation <= 315 -> TruvideoSdkCameraOrientation.LANDSCAPE_LEFT
                else -> TruvideoSdkCameraOrientation.PORTRAIT
            }

            if (rotation != value) {
                postValue(rotation)
            }
        }
    }

    override fun onActive() {
        super.onActive()
        listener.enable()
    }

    override fun onInactive() {
        super.onInactive()
        listener.disable()
    }
}
