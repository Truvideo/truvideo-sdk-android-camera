package com.truvideo.sdk.camera.service.camera

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.truvideo.sdk.camera.R

/**
 * Foreground service used to host [TruvideoSdkCameraService] so that recording
 * can continue even when the associated activity is destroyed under memory
 * pressure.
 */
class CameraForegroundService : Service() {

    private val binder = LocalBinder()
    var cameraService: TruvideoSdkCameraService? = null
        private set

    inner class LocalBinder : Binder() {
        fun getService(): CameraForegroundService = this@CameraForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        return START_STICKY
    }

    fun initialize(service: TruvideoSdkCameraService) {
        cameraService = service
    }

    override fun onDestroy() {
        Log.d(TAG, "CameraForegroundService destroyed")
        super.onDestroy()
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_ID,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.truvideo_sdk_camera_service_name))
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build()
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val TAG = "CameraForegroundService"
        private const val CHANNEL_ID = "truvideo_camera"
        private const val NOTIFICATION_ID = 1
    }
}
