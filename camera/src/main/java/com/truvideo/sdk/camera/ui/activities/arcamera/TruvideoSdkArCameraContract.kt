package com.truvideo.sdk.camera.ui.activities.arcamera

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia

class TruvideoSdkArCameraContract : ActivityResultContract<TruvideoSdkCameraConfiguration, List<TruvideoSdkCameraMedia>>() {

    override fun createIntent(context: Context, input: TruvideoSdkCameraConfiguration): Intent {
        return Intent(context, TruvideoSdkArCameraActivity::class.java).apply {
            putExtra("configuration", input.toJson())
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<TruvideoSdkCameraMedia> {
        return when (resultCode) {
            Activity.RESULT_OK -> {
                val data: ArrayList<String> = intent?.getStringArrayListExtra("media") ?: arrayListOf()
                data.mapNotNull { json ->
                    try {
                        TruvideoSdkCameraMedia.fromJson(json)
                    } catch (e: Exception) {
                        null
                    }
                }.toList()
            }

            else -> emptyList()
        }
    }
}