package com.truvideo.sdk.camera.service.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.TextureView
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.utils.createFile
import com.truvideo.sdk.camera.utils.deleteFile
import com.truvideo.sdk.camera.utils.getFocusRectangle
import com.truvideo.sdk.camera.utils.getVideoDuration
import com.truvideo.sdk.camera.utils.getZoomRectangle
import com.truvideo.sdk.camera.utils.save
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


internal class TruvideoSdkCameraService(
    private val context: Context,
    val information: TruvideoSdkCameraInformation,
    val configuration: TruvideoSdkCameraConfiguration,
    private val textureView: TextureView,
    private val serviceCallback: TruvideoSdkCameraServiceCallback,
) {

    companion object {
        const val TAG: String = "CameraService"
    }

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private var cameraDevice: CameraDevice? = null
    private var cameraModel: TruvideoSdkCameraDevice? = null

    var lensFacing: TruvideoSdkCameraLensFacing = TruvideoSdkCameraLensFacing.BACK

    lateinit var outputPath: String
    lateinit var backResolution: TruvideoSdkCameraResolution
    lateinit var frontResolution: TruvideoSdkCameraResolution
    lateinit var frontFlashMode: TruvideoSdkCameraFlashMode
    lateinit var backFlashMode: TruvideoSdkCameraFlashMode

    private var isRecording: Boolean = false
    private var isBusy: Boolean = true

    private var backgroundHandlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var previewCaptureSession: CameraCaptureSession? = null
    private var previewCaptureRequest: CaptureRequest.Builder? = null
    private var recordCaptureSession: CameraCaptureSession? = null
    private var recordingCaptureRequest: CaptureRequest.Builder? = null
    private var mediaRecorder: MediaRecorder? = null
    private var zoomRatio = 1f

    private lateinit var previewSize: TruvideoSdkCameraResolution
    private lateinit var imageSize: TruvideoSdkCameraResolution
    private lateinit var videoSize: TruvideoSdkCameraResolution
    private lateinit var imageReader: ImageReader
    private lateinit var videoFile: File

    val tapToFocusListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(event: MotionEvent): Boolean {
            return performTapToFocus(event)
        }
    }

    val scaleGestureListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            serviceCallback.updateZoomVisibility(true)
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoomRatio = (zoomRatio * detector.scaleFactor).coerceIn(1.0f, 10.0f)
            performZoom(zoomRatio)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
            serviceCallback.updateZoomVisibility(false)
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image: Image = reader.acquireLatestImage()
        Log.d(TAG, "Image ready (${image.width}x${image.height})")

        val name = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US).format(Date());
        image.save(
            outputPath,
            name,
            "jpg"
        ) { imageFile ->
            serviceCallback.onPicture(imageFile)
            image.close()
        }

    }


    @SuppressLint("MissingPermission")
    fun openCamera(callback: (() -> Unit)? = null) {
        startBackgroundThread()
        updateIsBusy(true)

        val newCamera = information.getDeviceFromFacing(lensFacing)
            ?: throw RuntimeException("Cannot open the camera. Info for lens facing $lensFacing not found")

        cameraDevice?.close()
        cameraDevice = null

        updateCamera(newCamera)
        initCamera()

        cameraManager.openCamera(
            newCamera.id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    CoroutineScope(Dispatchers.IO).launch {
                        if (isRecording) {
                            startRecording(callback)
                        } else {
                            startPreview(callback)
                        }
                    }

                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.d(TAG, "Camera disconnected")
                    updateIsBusy(false)
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.d(TAG, "Error opening the camera. Error $error")
                    updateIsBusy(false)
                }
            }, backgroundHandler
        )
    }

    suspend fun openCameraSuspend() {
        suspendCancellableCoroutine {
            openCamera {
                it.resumeWith(Result.success(Unit))
            }
        }
    }


    private fun getCurrentSensorSize(): Rect {
        val characteristics = cameraManager.getCameraCharacteristics(getCurrentCameraDevice().id)
        return characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!
    }

    fun performZoom(zoomLevel: Float) {
        zoomRatio = zoomLevel

        getCurrentRequest().apply {
            val zoomRect = getZoomRectangle(zoomRatio, getCurrentSensorSize())
            set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
            getCurrentSession().setRepeatingRequest(build(), null, backgroundHandler)
        }

        serviceCallback.updateZoom(zoomLevel)
    }

    private fun getCurrentSession(): CameraCaptureSession {
        val result = if (isRecording) {
            recordCaptureSession
        } else {
            previewCaptureSession
        }

        if (result == null) throw RuntimeException("Session not found")
        return result
    }

    private fun getCurrentRequest(): CaptureRequest.Builder {
        val result: CaptureRequest.Builder? = if (isRecording) {
            recordingCaptureRequest
        } else {
            previewCaptureRequest
        }

        if (result == null) throw RuntimeException("Request not found")
        return result
    }

    private fun getCurrentCameraModel(): TruvideoSdkCameraDevice {
        return cameraModel ?: throw RuntimeException("Camera model not found")
    }

    private fun getCurrentCameraDevice(): CameraDevice {
        return cameraDevice ?: throw RuntimeException("Camera device not found")
    }

    private fun performTapToFocus(motionEvent: MotionEvent): Boolean {
        var focused = false

        fun restoreCamera() {
            getCurrentRequest().apply {
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE)
            }

            getCurrentSession().apply {
                setRepeatingRequest(getCurrentRequest().build(), null, backgroundHandler)
            }
        }

        val captureCallbackHandler: CaptureCallback = object : CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult
            ) {
                super.onCaptureCompleted(session, request, result)
                if (focused) return
                val autoFocusState = result[CaptureResult.CONTROL_AF_STATE]
                val validStated = listOf(CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED, CaptureRequest.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED)

                if (autoFocusState == CaptureRequest.CONTROL_AF_STATE_INACTIVE) {
                    Log.d(TAG, "Tap to focus: Fail")
                    focused = true
                } else if (validStated.contains(autoFocusState)) {
                    Log.d(TAG, "Tap to focus: OK")
                    focused = true
                }

                if (focused) {
                    serviceCallback.onFocusLocked()
                    restoreCamera()
                }
            }

            override fun onCaptureFailed(
                session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure
            ) {
                super.onCaptureFailed(session, request, failure)
                Log.d(TAG, "Tap to focus: Failure $failure")

                serviceCallback.onFocusLocked()
                restoreCamera()
            }
        }

        getCurrentRequest().apply {
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            if (getCurrentCameraModel().isTapToFocusEnabled) {
                val meteringRectangle = meteringRectangle(motionEvent)
                set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(meteringRectangle))
            }

            serviceCallback.onFocusRequest()
            getCurrentSession().setRepeatingRequest(build(), captureCallbackHandler, backgroundHandler)
        }

        return true
    }

    fun disconnect(
        stopRecording: Boolean = true,
        callback: (() -> Unit)? = null
    ) {
        Log.d(TAG, "Disconnect camera")

        updateIsBusy(true)
        if (stopRecording) {
            updateIsRecording(false)
        }

        CoroutineScope(Dispatchers.IO).launch {
            stopMediaRecording()

            previewCaptureSession?.close()
            previewCaptureSession = null

            recordCaptureSession?.close()
            recordCaptureSession = null

            cameraDevice?.close()
            cameraDevice = null

            updateIsBusy(false)

            stopBackgroundThread()
            callback?.invoke()
        }
    }

    suspend fun disconnectSuspend(stopRecording: Boolean = true) = suspendCancellableCoroutine {
        disconnect(stopRecording) {
            it.resumeWith(Result.success(Unit))
        }
    }

    private fun initCamera() {
        val camera = cameraModel ?: throw RuntimeException("Cannot init the camera. Info not found")

        val resolution = when (camera.lensFacing) {
            TruvideoSdkCameraLensFacing.BACK -> backResolution
            TruvideoSdkCameraLensFacing.FRONT -> frontResolution
        }

        Log.d(TAG, "Resolution for ${camera.lensFacing} resolution ${resolution.width} ${resolution.height}")

        previewSize = resolution
        imageSize = resolution
        videoSize = resolution
        imageReader = ImageReader.newInstance(resolution.width, resolution.height, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
    }

    private fun getCameraFlashMode(): TruvideoSdkCameraFlashMode {
        val model = getCurrentCameraModel()

        val currentValue = when (model.lensFacing) {
            TruvideoSdkCameraLensFacing.BACK -> backFlashMode
            TruvideoSdkCameraLensFacing.FRONT -> frontFlashMode
        }

        val newValue = if (model.withFlash) {
            currentValue
        } else {
            TruvideoSdkCameraFlashMode.OFF
        }

        if (currentValue != newValue) {
            updateFlashMode(newValue)
        }

        return newValue
    }

    fun takePicture() {
        val cameraDevice = getCurrentCameraDevice()
        val cameraModel = getCurrentCameraModel()

        fun performTakePicture() {
            val pictureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            if (getCameraFlashMode().isOn) {
                pictureRequest.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE)
            }
            pictureRequest.addTarget(imageReader.surface)
            pictureRequest.set(
                CaptureRequest.JPEG_ORIENTATION,
                serviceCallback.getSensorRotation().getMediaRotation(cameraModel.sensorOrientation)
            )

            getCurrentSession().capture(pictureRequest.build(), null, backgroundHandler)
        }

        val flash = getCameraFlashMode().isOn
        if (!flash || isRecording) {
            performTakePicture()
            return
        }

        getCurrentRequest().apply {
            set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
        }

        var focusLocked = false
        getCurrentSession().setRepeatingRequest(
            getCurrentRequest().build(),
            object : CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    super.onCaptureCompleted(session, request, result)
                    if (focusLocked) return

                    val state = result[CaptureResult.CONTROL_AF_STATE]
                    val validStates = listOf(
                        CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED,
                        CaptureRequest.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                    )
                    val invalidStates = listOf(CaptureRequest.CONTROL_AF_STATE_INACTIVE)

                    if (invalidStates.contains(state)) {
                        focusLocked = true
                        Log.d(TAG, "Fail to lock the focus")
                    } else if (validStates.contains(state)) {
                        focusLocked = true
                        Log.d(TAG, "Focus locked")
                    }


                    if (focusLocked) {
                        performTakePicture()

                        getCurrentRequest().apply {
                            set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
                            if (isRecording) {
                                if (flash) {
                                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                                } else {
                                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                                }
                                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                            } else {
                                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            }
                        }

                        getCurrentSession().setRepeatingRequest(
                            getCurrentRequest().build(),
                            null,
                            backgroundHandler
                        )
                    }
                }
            },
            backgroundHandler
        )
    }

    fun toggleRecording() {
        if (isBusy) return
        updateIsBusy(true)

        CoroutineScope(Dispatchers.IO).launch {
            if (isRecording) {
                updateIsRecording(false)
                startPreview()
            } else {
                updateIsRecording(true)
                startRecording()
            }
        }
    }

    fun toggleFlash() {
        val cameraModel = getCurrentCameraModel()

        val newFlashMode = if (cameraModel.withFlash) {
            if (getCameraFlashMode().isOn) {
                TruvideoSdkCameraFlashMode.OFF
            } else {
                TruvideoSdkCameraFlashMode.ON
            }
        } else {
            TruvideoSdkCameraFlashMode.OFF
        }

        updateFlashMode(newFlashMode)

        getCurrentRequest().apply {
            if (newFlashMode.isOn && isRecording) {
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            } else {
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            }
        }

        try {
            getCurrentSession().apply {
                setRepeatingRequest(getCurrentRequest().build(), null, backgroundHandler)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private suspend fun startPreview(callback: (() -> Unit)? = null) {
        stopMediaRecording()

        val surfaceTexture = textureView.surfaceTexture ?: throw RuntimeException("Texture not found")
        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(surfaceTexture)

        previewCaptureRequest = getCurrentCameraDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(previewSurface)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
            set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            val zoomRect = getZoomRectangle(zoomRatio, getCurrentSensorSize())
            set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
        }

        @Suppress("DEPRECATION")
        getCurrentCameraDevice().createCaptureSession(
            listOf(previewSurface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.d(TAG, "Failed to configure camera preview")
                    updateIsBusy(false)
                    if (callback != null) callback()
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    Log.d(TAG, "Camera preview configured")
                    previewCaptureSession = session

                    var firstTime = true
                    try {
                        session.setRepeatingRequest(
                            getCurrentRequest().build(),
                            object : CaptureCallback() {
                                override fun onCaptureStarted(
                                    session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long
                                ) {
                                    if (firstTime) {
                                        Log.d(TAG, "Camera preview started")
                                        updateIsBusy(false)
                                        if (callback != null) callback()
                                        firstTime = false
                                    }
                                }
                            },
                            backgroundHandler
                        )
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                        Log.e(TAG, "Failed to start camera preview because it couldn't access the camera")
                        updateIsBusy(false)
                        if (callback != null) callback()
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                        Log.e(TAG, "Failed to start camera preview")
                        updateIsBusy(false)
                        if (callback != null) callback()
                    }
                }
            },
            backgroundHandler
        )
    }

    private suspend fun startRecording(callback: (() -> Unit)? = null) {
        setupMediaRecorder()

        val m = mediaRecorder ?: throw RuntimeException("Media recorder not found")
        val surfaceTexture = textureView.surfaceTexture ?: throw RuntimeException("Texture not found")
        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(surfaceTexture)
        val recordingSurface = m.surface

        recordingCaptureRequest = getCurrentCameraDevice().createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            addTarget(previewSurface)
            addTarget(recordingSurface)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
            val zoomRect = getZoomRectangle(zoomRatio, getCurrentSensorSize())
            set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
            if (getCameraFlashMode().isOn) {
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            } else {
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            }
        }

        @Suppress("DEPRECATION")
        getCurrentCameraDevice().createCaptureSession(
            listOf(previewSurface, recordingSurface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.d(TAG, "Failed to configure camera recording")
                    updateIsBusy(false)
                    updateIsRecording(false)
                    if (callback != null) callback()
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    Log.d(TAG, "Camera recording configured")
                    recordCaptureSession = session

                    var firstTime = true
                    try {
                        session.setRepeatingRequest(
                            getCurrentRequest().build(),
                            object : CaptureCallback() {
                                override fun onCaptureStarted(
                                    session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long
                                ) {
                                    if (firstTime) {
                                        Log.d(TAG, "Camera recording started")
                                        updateIsBusy(false)
                                        firstTime = false
                                        serviceCallback.onRecordingStarted()
                                        m.start()
                                        if (callback != null) callback()
                                    }
                                }
                            },
                            backgroundHandler
                        )
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                        updateIsRecording(false)
                        updateIsBusy(false)
                        Log.e(TAG, "Failed to start camera recording because it couldn't access the camera")
                        if (callback != null) callback()
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                        updateIsRecording(false)
                        updateIsBusy(false)
                        Log.e(TAG, "Failed to start camera recording")
                        if (callback != null) callback()
                    }

                }
            },
            backgroundHandler
        )
    }

    private suspend fun stopMediaRecording() = suspendCancellableCoroutine { cont ->
        CoroutineScope(Dispatchers.IO).launch {
            val m = mediaRecorder
            if (m != null) {
                try {
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
                    serviceCallback.onVideo(videoFile, duration)
                } else {
                    Log.d(TAG, "No video to add")
                }
            }

            cont.resumeWith(Result.success(Unit))
        }
    }

    private suspend fun setupMediaRecorder() {
        stopMediaRecording()

        val newFile = createVideoFile() ?: throw RuntimeException("Error creating video file")
        val model = cameraModel ?: throw RuntimeException("No camera model found")

        videoFile = newFile

        val rotation = serviceCallback.getSensorRotation()

        val m = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }

        // Video
        m.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        m.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        m.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        m.setVideoSize(videoSize.width, videoSize.height)
        m.setVideoFrameRate(24)
        m.setVideoEncodingBitRate(10_000_000)
        // Audio
        m.setAudioSource(MediaRecorder.AudioSource.MIC)
        m.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        m.setAudioSamplingRate(16_000_000)

        m.setOrientationHint(rotation.getMediaRotation(model.sensorOrientation))
        m.setOutputFile(videoFile.absolutePath)
        m.prepare()
        mediaRecorder = m

        Log.d(TAG, "Media recording created. Path ${videoFile.path}")
    }

    private fun createVideoFile(): File? {
        val name = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US).format(Date())
        return createFile(outputPath, name, "mp4")
    }

    private fun startBackgroundThread() {
        stopBackgroundThread()

        val name = "CameraServiceThread-${System.currentTimeMillis()}"
        Log.d(TAG, "Thread name $name")

        val thread = HandlerThread(name)
        thread.start()
        backgroundHandler = Handler(thread.looper)
        backgroundHandlerThread = thread
    }

    private fun stopBackgroundThread() {
        val thread = backgroundHandlerThread ?: return
        thread.quitSafely()
        try {
            thread.join()
            backgroundHandlerThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
            backgroundHandlerThread = null
            backgroundHandler = null
        }
    }

    private fun updateIsBusy(value: Boolean) {
        isBusy = value
        serviceCallback.updateIsBusy(value)
    }

    private fun updateIsRecording(value: Boolean) {
        isRecording = value
        serviceCallback.updateIsRecording(value)
    }

    private fun updateFlashMode(value: TruvideoSdkCameraFlashMode) {
        val camera = getCurrentCameraModel()
        when (camera.lensFacing) {
            TruvideoSdkCameraLensFacing.BACK -> backFlashMode = value
            TruvideoSdkCameraLensFacing.FRONT -> frontFlashMode = value
        }

        serviceCallback.updateFlashMode(camera.lensFacing, value)
    }

    private fun updateCamera(value: TruvideoSdkCameraDevice) {
        cameraModel = value
        serviceCallback.updateCamera(value)
    }

    private fun meteringRectangle(event: MotionEvent): MeteringRectangle {
        val camera = cameraModel ?: throw RuntimeException("No camera information")

        val sensorOrientation = camera.sensorOrientation
        val sensorSize = getCurrentSensorSize()

        return getFocusRectangle(
            touchX = event.x,
            touchY = event.y,
            viewWidth = textureView.width.toFloat(),
            viewHeight = textureView.height.toFloat(),
            sensorSize = sensorSize,
            orientation = sensorOrientation
        )
    }
}


