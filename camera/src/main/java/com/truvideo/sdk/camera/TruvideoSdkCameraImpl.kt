package com.truvideo.sdk.camera

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCamera
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraLogAdapter
import com.truvideo.sdk.camera.model.TruvideoSdkCameraEvent
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import com.truvideo.sdk.camera.usecase.GetCameraInformationUseCase
import com.truvideo.sdk.camera.utils.EventConstants
import truvideo.sdk.common.TruvideoSdkContextProvider
import truvideo.sdk.common.model.TruvideoSdkLogSeverity


@SuppressLint("UnspecifiedRegisterReceiverFlag")
internal class TruvideoSdkCameraImpl(
    context: Context,
    private val getCameraInformationUseCase: GetCameraInformationUseCase,
    private val authAdapter: TruvideoSdkCameraAuthAdapter,
    private val logAdapter: TruvideoSdkCameraLogAdapter
) : TruvideoSdkCamera {

    private val _events = MutableLiveData<TruvideoSdkCameraEvent>()

    private val eventReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == EventConstants.ACTION_NAME) {
                val data = intent.getStringExtra(EventConstants.EVENT_DATA) ?: ""
                val model = try {
                    TruvideoSdkCameraEvent.fromJson(data)
                } catch (exception: Exception) {
                    exception.printStackTrace()
                    null
                }

                if (model != null) {
                    _events.value = model
                }
            }
        }
    }

    init {
        TruvideoSdkContextProvider.instance.init(context);

        logAdapter.addLog(
            eventName = "event_camera_init",
            message = "Init camera module",
            severity = TruvideoSdkLogSeverity.INFO
        )

        // Register events
        val filter = IntentFilter(EventConstants.ACTION_NAME)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(eventReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(eventReceiver, filter)
        }
    }

    override fun getInformation(): TruvideoSdkCameraInformation {
        logAdapter.addLog(
            eventName = "event_camera_get_information",
            message = "Getting camera information",
            severity = TruvideoSdkLogSeverity.INFO,
        )
        authAdapter.validateAuthentication()

        return getCameraInformationUseCase()
    }

    override val events: LiveData<TruvideoSdkCameraEvent>
        get() = _events

    override val environment: String
        get() = BuildConfig.FLAVOR


}
