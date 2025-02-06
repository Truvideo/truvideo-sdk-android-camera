@file:Suppress("unused")

package com.truvideo.sdk.camera.ui.activities.scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraScannerValidation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerCode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerConfiguration

class TruvideoSdkCameraScannerContract : ActivityResultContract<TruvideoSdkCameraScannerConfiguration, TruvideoSdkCameraScannerCode?>() {

    companion object {
        var validator: TruvideoSdkCameraScannerValidation? = null
        const val INPUT = "configuration"
        const val RESULT = "result"
    }

    override fun createIntent(context: Context, input: TruvideoSdkCameraScannerConfiguration): Intent {
        validator = input.validator

        return Intent(context, TruvideoSdkCameraScannerActivity::class.java).apply {
            putExtra(INPUT, input.toJson())
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): TruvideoSdkCameraScannerCode? {
        validator = null

        return when (resultCode) {
            Activity.RESULT_OK -> {
                val result = intent?.getStringExtra(RESULT) ?: return null

                try {
                    return TruvideoSdkCameraScannerCode.fromJson(result)
                } catch (exception: Exception) {
                    exception.printStackTrace()
                    return null
                }
            }

            else -> null
        }
    }
}