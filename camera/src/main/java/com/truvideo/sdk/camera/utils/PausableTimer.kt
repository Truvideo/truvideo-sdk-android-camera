package com.truvideo.sdk.camera.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PausableTimer(
    private val interval: Long = 10,
    private val onUpdate: (time: Long) -> Unit = {}
) {
    private var isRunning: Boolean = false
    private var isPaused: Boolean = false
    private var timer: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var startedAt = 0L
    private var pausedAt = 0L
    private var time = 0L

    fun start() {
        if (isRunning && !isPaused) {
            Log.d("TruvideoSdkCamera", "Cannot start the timer. Already running and not paused")
            return
        }

        if (!isPaused) {
            Log.d("TruvideoSdkCamera", "Starting timer.")
            startedAt = System.currentTimeMillis() - time
            onUpdate(time)
        } else {
            Log.d("TruvideoSdkCamera", "Resuming timer.")
            startedAt = System.currentTimeMillis() - time
            isPaused = false
        }

        isRunning = true

        timer = scope.launch {
            while (isRunning) {
                delay(interval)
                time = System.currentTimeMillis() - startedAt
                onUpdate(time)
            }
        }
    }

    fun pause() {
        if (!isRunning || isPaused) {
            Log.d("TruvideoSdkCamera", "Cannot pause. Timer is not running or its already paused.")
            return
        }

        Log.d("TruvideoSdkCamera", "Pausing timer.")

        isPaused = true
        pausedAt = System.currentTimeMillis()
        timer?.cancel()
    }

    fun resume() {
        if (!isRunning || !isPaused) {
            Log.d("TruvideoSdkCamera", "Cannot resume. Timer is not running or its not already paused.")
            return
        }

        start()
    }

    fun stop() {
        timer?.cancel()
        isRunning = false
        isPaused = false
        time = 0L
        onUpdate(time)
    }
}