package com.truvideo.sdk.camera.adapters

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.view.Surface
import com.truvideo.sdk.camera.service.camera.TruvideoSdkCameraService.Companion.TAG
import kotlinx.coroutines.delay
import java.io.File

interface TruvideoSdkCameraRecorderAdapter {
    fun prepare(
        videoWidth: Int,
        videoHeight: Int,
        orientation: Int,
        outputFile: File,
        durationLimit: Int?,
        onMaxDurationReached: () -> Unit = {}
    ) : Surface?
    fun start(onStarted: () -> Unit,)
    fun stop(onStopped: () -> Unit = {})
    val recordingSurface: Surface?
}

class TruvideoSdkCameraRecorderAdapterImpl(
    val  context: Context
) : TruvideoSdkCameraRecorderAdapter {

    private var mediaRecorder : MediaRecorder? = null

    override val recordingSurface: Surface?
        get() = mediaRecorder?.surface

    override fun prepare(
        videoWidth: Int,
        videoHeight: Int,
        orientation: Int,
        outputFile: File,
        durationLimit: Int?,
        onMaxDurationReached: () -> Unit
    ) : Surface? {
        val m = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }

        var maxDurationReported = false

        m.apply {

            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)

            // Video Format & FPS
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoFrameRate(24)

            // Video size, bitrate & encoder
            setVideoSize(videoWidth, videoHeight)
            val bitRate = when (videoWidth) {
                480 -> 1_000_000
                720 -> 2_500_000
                1080 -> 4_000_000
                else -> 2_000_000
            }
            setVideoEncodingBitRate(bitRate)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setOrientationHint(orientation)

            // Audio Config
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)  // 44.1 kHz
            setAudioEncodingBitRate(128000)  // 128 kbps

            setOutputFile(outputFile.absolutePath)

            if (durationLimit != null) {
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        if (!maxDurationReported) {
                            maxDurationReported = true
                            onMaxDurationReached()
                        }
                    }
                }
                setMaxDuration(durationLimit)
            }

            prepare()
        }
        Log.d(TAG, "Media recording created. Path ${outputFile.path}")

        mediaRecorder = m


        return mediaRecorder?.surface ?: throw NullPointerException("MediaRecorder surface is null")

    }

    override fun start(onStarted: () -> Unit)  {
        val recorder = mediaRecorder ?: return
        recorder.start()
        onStarted()
    }

    override fun stop(onStopped: () -> Unit) {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            Log.d(TAG, "Media recording started successfully")
            onStopped()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "Media recording failed.")
        }
    }
}