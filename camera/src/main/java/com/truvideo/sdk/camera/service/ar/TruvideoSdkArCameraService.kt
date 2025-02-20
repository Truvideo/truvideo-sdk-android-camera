package com.truvideo.sdk.camera.service.ar

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.PixelCopy
import com.google.ar.sceneform.ArSceneView
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.service.camera.TruvideoSdkCameraService
import com.truvideo.sdk.camera.utils.PausableTimer
import com.truvideo.sdk.camera.utils.createFile
import com.truvideo.sdk.camera.utils.deleteFile
import com.truvideo.sdk.camera.utils.getVideoDuration
import com.truvideo.sdk.camera.utils.save
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import truvideo.sdk.common.exceptions.TruvideoSdkException
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class TruvideoSdkArCameraService(
    private val context: Context,
    private val information: TruvideoSdkCameraInformation?,
    private val serviceCallback: TruvideoSdkArCameraServiceCallback,
) {

    private var isRecording: Boolean = false
    private var isBusy: Boolean = false
    private var isPaused: Boolean = false

    companion object {
        const val TAG: String = "[TruvideoSdkCamera][TruvideoSdkArCameraService]"
    }

    var maxDuration: Int? = null
    private var maxDurationReported = false
    private var videoDurationTimer: PausableTimer? = null

    lateinit var outputPath: String
    private var mediaRecorder: MediaRecorder? = null
    private var backgroundHandler: Handler? = null

    var videoDuration = MutableStateFlow(0L)

    private lateinit var videoFile: File
    private lateinit var sceneView: ArSceneView

    val videoSize: Size
        get() {
            val backCamera = information?.backCamera ?: throw TruvideoSdkException("No back camera found")
            val resolution = backCamera.resolutions.first()
            return Size(
                resolution.width,
                resolution.height
            )
        }

    init {
        val name = "ArCameraServiceThread-${System.currentTimeMillis()}"
        val thread = HandlerThread(name)
        thread.start()
        backgroundHandler = Handler(thread.looper)
    }

    fun setUpSceneView(sceneView: ArSceneView) {
        this.sceneView = sceneView
    }

    private fun startTimer() {
        stopTimer()

        videoDurationTimer = PausableTimer(
            onUpdate = { videoDuration.value = it },
        )
        videoDurationTimer?.start()
    }

    private fun stopTimer() {
        videoDurationTimer?.stop()
        videoDurationTimer = null
        videoDuration.value = 0L
    }

    fun toggleRecording(maxVideoDurationReached: Boolean = false) {
        if (isBusy) return
        updateIsBusy(true)
        scope.launch {
            if (isRecording) {
                videoDurationTimer?.pause()
                updateIsRecording(false)
                stopMediaRecording(maxVideoDurationReached)
                updateIsBusy(false)
            } else {
                updateIsRecording(true)
                startRecording()
            }
        }
    }

    private fun updateIsRecording(value: Boolean) {
        isRecording = value
        serviceCallback.updateIsRecording(value)
    }

    private fun updateIsPaused(value: Boolean) {
        isPaused = value
        serviceCallback.updateIsPaused(value)
    }

    private fun updateIsBusy(value: Boolean) {
        isBusy = value
        serviceCallback.updateIsBusy(value)
    }

    private fun startRecording(callback: (() -> Unit)? = null) {
        setupMediaRecorder()
        val m = mediaRecorder ?: throw RuntimeException("Media recorder not found")
        val recordingSurface = m.surface

        try {
            sceneView.startMirroringToSurface(
                recordingSurface,
                0,
                0,
                videoSize.height,
                videoSize.width
            )
        } catch (e: IOException) {
            Log.e("TruvideoSdkArCameraService", "Exception setting up recorder", e)
            updateIsBusy(false)
            updateIsRecording(false)
            if (callback != null) callback()
            return
        }

        Log.d(TAG, "Camera recording started")
        updateIsBusy(false)
        serviceCallback.onRecordingStarted()
//        m.start()
        startTimer()
        if (callback != null) callback()

        Log.d(TAG, "Media recording created. Path ${videoFile.path}")

    }

    private fun createVideoFile(): File? {
        val name = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US).format(Date())
        return createFile(outputPath, name, "mp4")
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private fun setupMediaRecorder() {
//        stopMediaRecording()

        val newFile = createVideoFile() ?: throw RuntimeException("Error creating video file")

        videoFile = newFile

        val rotation = serviceCallback.getSensorRotation()

        val m = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }
        maxDurationReported = false
        m.setOnInfoListener { _, what, _ ->
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                scope.launch {
                    if (!maxDurationReported) {
                        Log.d(TAG, "Max duration reached")
                        maxDurationReported = true
                        toggleRecording(maxVideoDurationReached = true)
                    }
                }
            }
        }


        m.setAudioSource(MediaRecorder.AudioSource.MIC)
        m.setVideoSource(MediaRecorder.VideoSource.SURFACE)

        // Video Format & FPS
        m.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        m.setVideoFrameRate(24)

        // Video size, bitrate & encoder
        m.setVideoSize(videoSize.height, videoSize.width)
        m.setVideoEncodingBitRate(10_000_000)
        m.setVideoEncoder(MediaRecorder.VideoEncoder.H264)

        // Audio Config
        m.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        m.setAudioSamplingRate(44100)  // 44.1 kHz
        m.setAudioEncodingBitRate(128000)  // 128 kbps

        val orientationHint = when (rotation) {
            TruvideoSdkCameraOrientation.PORTRAIT -> 0
            TruvideoSdkCameraOrientation.LANDSCAPE_LEFT -> 270
            TruvideoSdkCameraOrientation.LANDSCAPE_RIGHT -> 90
            TruvideoSdkCameraOrientation.PORTRAIT_REVERSE -> 180
        }
        m.setOrientationHint(orientationHint)
        m.setOutputFile(videoFile.absolutePath)

        if (maxDuration != null) {
            m.setMaxDuration(maxDuration!!)
        }

        m.prepare()
        mediaRecorder = m

        m.start()

        Log.d(TAG, "Media recording created. Path ${videoFile.path}")
    }

    private fun stopMediaRecording(maxVideoDurationReached: Boolean) {
        val sensorRotation = serviceCallback.getSensorRotation()
        val rotation = calculateRotation(
            sensorRotation = 0,
            orientation = sensorRotation.rotation
        )
        val resolution = when (rotation) {
            90, 270 -> TruvideoSdkCameraResolution(videoSize.width, videoSize.width)
            else -> TruvideoSdkCameraResolution(videoSize.height, videoSize.width)
        }

        val m = mediaRecorder
        if (m != null) {
            try {
                sceneView.stopMirroringToSurface(m.surface)
                m.stop()
            } catch (e: Exception) {
                Log.d(TAG, "Failed to stop media recorder", e)
                deleteFile(videoFile)
            }

            m.reset()
            m.release()
            mediaRecorder = null

            if (videoFile.exists()) {
                val duration = getVideoDuration(videoFile)
                Log.d(TAG, "Media recording stopped. Path: ${videoFile.path}. Duration: $duration")
                serviceCallback.onVideo(
                    file = videoFile,
                    duration = duration,
                    resolution = resolution,
                    orientation = sensorRotation,
                    maxVideoDurationReached = maxVideoDurationReached
                )
            } else {
                Log.d(TAG, "No video to add")
            }
        }
    }

    private fun calculateRotation(
        sensorRotation: Int,
        orientation: Int
    ): Int {
        val result = sensorRotation - orientation
        return ((result % 360) + 360) % 360
    }

    fun takeImage() {
        updateIsBusy(true)
        val bitmap: Bitmap = Bitmap.createBitmap(sceneView.width, sceneView.height, Bitmap.Config.ARGB_8888)

        val sensorRotation = serviceCallback.getSensorRotation()
        val rotation = calculateRotation(
            sensorRotation = 0,
            orientation = sensorRotation.rotation
        )

        val fixedResolution = when (rotation) {
            90, 270 -> TruvideoSdkCameraResolution(sceneView.height, sceneView.width)
            else -> TruvideoSdkCameraResolution(sceneView.width, sceneView.height)
        }

        PixelCopy.request(sceneView, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                // Save bitmap
                val name = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US).format(Date())
                bitmap.save(outputPath, name, "png", rotation) {
                    serviceCallback.onImage(
                        file = it,
                        orientation = sensorRotation,
                        resolution = fixedResolution
                    )
                    updateIsBusy(false)
                }
            } else {
                // Error
                updateIsBusy(false)
            }
        }, backgroundHandler!!)
    }

    fun disconnect(
        stopRecording: Boolean = true,
        callback: (() -> Unit)? = null
    ) {
        Log.d(TAG, "Disconnect camera")

        if (stopRecording) {
            updateIsRecording(false)
        }

        CoroutineScope(Dispatchers.IO).launch {
            stopMediaRecording(false)
            callback?.invoke()
        }
    }

    fun pauseRecording() {
        if (!isRecording || isPaused) {
            Log.d(TruvideoSdkCameraService.TAG, "Recording not in progress or already paused")
            return
        }

        videoDurationTimer?.pause()

        try {
            mediaRecorder?.pause()
            updateIsPaused(true)
        } catch (e: Exception) {
            Log.e(TruvideoSdkCameraService.TAG, "Error pausing video recording", e)
        }
    }

    fun resumeRecording() {
        if (!isRecording || !isPaused) {
            Log.d(TruvideoSdkCameraService.TAG, "Recording not paused or not in progress")
            return
        }

        videoDurationTimer?.resume()

        try {
            mediaRecorder?.resume()
            updateIsPaused(false)
        } catch (e: Exception) {
            Log.e(TruvideoSdkCameraService.TAG, "Error resuming video recording", e)
        }
    }

}


