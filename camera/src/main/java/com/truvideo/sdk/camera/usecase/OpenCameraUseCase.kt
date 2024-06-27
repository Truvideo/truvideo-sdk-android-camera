package com.truvideo.sdk.camera.usecase

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.truvideo.sdk.camera.CameraActivity
import com.truvideo.sdk.camera.exceptions.TruvideoSdkCameraException
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraCallback
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

internal class OpenCameraUseCase(
    private val authAdapter: TruvideoSdkCameraAuthAdapter
) {
    private var handlers = mutableMapOf<ComponentActivity, TruvideoSdkCameraScreen>()

    fun init(activity: ComponentActivity): TruvideoSdkCameraScreen {
        var handler = handlers[activity]
        if (handler == null) {
            handler = TruvideoSdkCameraScreen(activity)
            handlers[activity] = handler
        }

        handler.authAdapter = authAdapter
        return handler
    }
}

class TruvideoSdkCameraScreen(
    val activity: ComponentActivity
) {

    internal var authAdapter: TruvideoSdkCameraAuthAdapter? = null

    private val scope = CoroutineScope(Dispatchers.Main)
    private var continuation: CancellableContinuation<List<TruvideoSdkCameraMedia>>? = null
    private val startForResult =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                val data: ArrayList<String> = intent?.getStringArrayListExtra("media") ?: ArrayList()
                val media = data.map { TruvideoSdkCameraMedia.fromJson(it) }.toList()
                continuation?.resumeWith(Result.success(media))
            } else {
                continuation?.resumeWith(Result.success(listOf()))
            }
        }

    suspend fun open(configuration: TruvideoSdkCameraConfiguration? = null): List<TruvideoSdkCameraMedia> {
        authAdapter?.validateAuthentication()

        val intent = Intent(activity, CameraActivity::class.java)
        intent.putExtra(
            "configuration",
            (configuration ?: TruvideoSdkCameraConfiguration()).toJson()
        )
        startForResult.launch(intent)
        return suspendCancellableCoroutine { continuation = it }
    }

    fun open(configuration: TruvideoSdkCameraConfiguration? = null, callback: TruvideoSdkCameraCallback<List<TruvideoSdkCameraMedia>>) {
        scope.launch {
            try {
                val result = open(configuration)
                callback.onComplete(result)
            } catch (exception: Exception) {
                exception.printStackTrace()

                if (exception is TruvideoSdkCameraException) {
                    throw exception
                } else {
                    throw TruvideoSdkCameraException(exception.localizedMessage ?: "Unknown error")
                }
            }
        }
    }


}