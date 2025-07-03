package com.truvideo.sdk.camera.domain.utils

import android.util.Log
import com.truvideo.sdk.camera.adapters.RecordingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class RecordingTimer(
    private val interval: Long = 10L,
    private val onUpdate: (Long) -> Unit = {}
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null
    var time = 0L
    private var startedAt = 0L

    private var state: TimerState = TimerState.Stopped

    fun start() = transitionTo(TimerState.Running)

    fun pause() = transitionTo(TimerState.Paused)

    fun resume() = transitionTo(TimerState.Running)

    fun stop() = transitionTo(TimerState.Stopped)

    private fun transitionTo(newState: TimerState) {
        log("Current timer state: ${state.javaClass.simpleName}, newState: ${newState.javaClass.simpleName}")
        state = when (newState) {
            is TimerState.Running -> when (state) {
                is TimerState.Running -> {
                    log("Timer already running.")
                    return
                }

                is TimerState.Paused -> {
                    log("Resuming timer.")
                    // - time
                    launchTimer()
                    TimerState.Running
                }

                is TimerState.Stopped -> {
                    log("Starting timer.")
                    startedAt = System.currentTimeMillis() - time
                    launchTimer()
                    TimerState.Running
                }
            }

            is TimerState.Paused ->
            {
                log("Current timer state: ${state.javaClass.simpleName}, newState: ${newState.javaClass.simpleName}")
                when (state) {

                    is TimerState.Running -> {
                        job?.cancel()
                        log("Timer paused.")
                        TimerState.Paused
                    }
                    else -> {
                        log("Cannot pause. Timer is not running.")
                        return
                    }
                }
            }

            is TimerState.Stopped -> {
                job?.cancel()
                time = 0L
                onUpdate(time)
                log("Timer stopped.")
                TimerState.Stopped
            }
        }

        log("Current timer newState: ${state.javaClass.simpleName}")
    }

    private fun launchTimer() {
        job = scope.launch {
            while (state is TimerState.Running) {
                time = System.currentTimeMillis() - startedAt
                onUpdate(time)
                delay(interval)
            }
        }
    }

    private fun log(msg: String) {
        Log.d("TruvideoSdkCamera", msg)
    }

    private sealed class TimerState {
        object Running : TimerState()
        object Paused : TimerState()
        object Stopped : TimerState()
    }
}