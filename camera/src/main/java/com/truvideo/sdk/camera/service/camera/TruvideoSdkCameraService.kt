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
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraImageFormat
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.utils.PausableTimer
import com.truvideo.sdk.camera.utils.createFile
import com.truvideo.sdk.camera.utils.deleteFile
import com.truvideo.sdk.camera.utils.getFocusRectangle
import com.truvideo.sdk.camera.utils.getVideoDuration
import com.truvideo.sdk.camera.utils.getZoomRectangle
import com.truvideo.sdk.camera.utils.save
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import truvideo.sdk.common.exceptions.TruvideoSdkException
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class TruvideoSdkCameraService(
    private val context: Context,
    val information: TruvideoSdkCameraInformation,
    private var textureView: TextureView,
    private var serviceCallback: TruvideoSdkCameraServiceCallback
) {

    companion object {
        const val TAG: String = "[TruvideoSdkCamera][TruvideoSdkCameraService]"
    }

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var cameraDevice: CameraDevice? = null
    private var cameraModel: TruvideoSdkCameraDevice? = null

    var lensFacing: TruvideoSdkCameraLensFacing = TruvideoSdkCameraLensFacing.BACK

    lateinit var outputPath: String
    lateinit var imageFormat: TruvideoSdkCameraImageFormat
    lateinit var backResolution: TruvideoSdkCameraResolution
    lateinit var frontResolution: TruvideoSdkCameraResolution
    lateinit var frontFlashMode: TruvideoSdkCameraFlashMode
    lateinit var backFlashMode: TruvideoSdkCameraFlashMode

    private var _isCameraOpened = MutableStateFlow(false)
    var isCameraOpened: StateFlow<Boolean> = _isCameraOpened

    private var isRecording: Boolean = false
    private var isBusy: Boolean = true
    private var isPaused: Boolean = false

    private var imageOrientation = TruvideoSdkCameraOrientation.PORTRAIT
    private var imageSensorRotation = 0
    private var imageLensFacing = TruvideoSdkCameraLensFacing.FRONT

    private var videoSensorRation = 0
    private var videoResolution = TruvideoSdkCameraResolution(0, 0)
    private var videoOrientation = TruvideoSdkCameraOrientation.PORTRAIT
    private var videoLensFacing = TruvideoSdkCameraLensFacing.FRONT

    fun updateTextureView(value: TextureView) {
        textureView = value
    }

    fun updateServiceCallback(value: TruvideoSdkCameraServiceCallback) {
        serviceCallback = value
    }

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
    var maxDuration: Int? = null
    private var maxDurationReported = false
    private var videoDurationTimer: PausableTimer? = null
    var videoDuration = MutableStateFlow(0L)

    fun isRecording() = isRecording

    fun isPaused() = isPaused

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

    private fun calculateRotation(
        sensorRotation: Int,
        isFront: Boolean,
        orientation: Int
    ): Int {
        val result = if (isFront) {
            sensorRotation + orientation
        } else {
            sensorRotation - orientation + 360
        }
        return (result % 360) % 360
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image: Image = reader.acquireLatestImage()

        val rotation = calculateRotation(
            sensorRotation = imageSensorRotation,
            isFront = imageLensFacing.isFront,
            orientation = imageOrientation.rotation
        )

        val fixedResolution = when (rotation) {
            90, 270 -> TruvideoSdkCameraResolution(image.height, image.width)
            else -> TruvideoSdkCameraResolution(image.width, image.height)
        }

        val name = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US).format(Date())

        scope.launch {
            try {
                val result = image.save(
                    path = outputPath,
                    name = name,
                    extension = imageFormat.code,
                    rotation = rotation
                )

                serviceCallback.onImage(
                    file = File(result),
                    lensFacing = imageLensFacing,
                    orientation = imageOrientation,
                    resolution = fixedResolution
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
            } finally {
                image.close()
                updateIsBusy(false)
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun openCamera() {
        if (_isCameraOpened.value) return

        suspendCancellableCoroutine { cont ->

            scope.launch {
                startBackgroundThread()
                updateIsBusy(true)

                val newCamera = information.getDeviceFromFacing(lensFacing) ?: run {
                    cont.resumeWith(Result.failure(RuntimeException("Cannot open the camera. Info for lens facing $lensFacing not found")))
                    return@launch
                }

                cameraDevice = null
                cameraDevice = null

                updateCamera(newCamera)
                initCamera()

                cameraManager.openCamera(
                    newCamera.id,
                    object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            cameraDevice = camera
                            _isCameraOpened.value = true

                            scope.launch {
                                if (isRecording) {
                                    startRecording()
                                } else {
                                    startPreview()
                                }
                                cont.resumeWith(Result.success(Unit))
                            }
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            Log.d(TAG, "Camera disconnected")
                            scope.launch {
                                disconnect(camera)
                            }
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            when(error) {
                                ERROR_CAMERA_DISABLED -> Log.d(TAG, "Camera was disabled")
                                ERROR_CAMERA_DEVICE -> Log.d(TAG, "Error on camera")
                                ERROR_CAMERA_IN_USE -> Log.d(TAG, "Error camera in use")
                                ERROR_CAMERA_SERVICE -> Log.d(TAG, "Error camera in use")
                                ERROR_MAX_CAMERAS_IN_USE -> Log.d(TAG, "Max number of cameras in use")
                            }

                            scope.launch {
                                disconnect(camera)
                            }
                        }
                    },
                    backgroundHandler
                )
            }
        }
    }

    private fun getCurrentSensorSize(): Rect {
        val characteristics = cameraManager.getCameraCharacteristics(getCurrentCameraDevice().id)
        return characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!
    }

    fun performZoom(zoomLevel: Float) {
        if (!_isCameraOpened.value) return

        zoomRatio = zoomLevel

        scope.launch {
            getCurrentRequest().apply {
                val zoomRect = getZoomRectangle(zoomRatio, getCurrentSensorSize())
                set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
                getCurrentSession().setRepeatingRequest(build(), null, backgroundHandler)
            }
        }

        serviceCallback.updateZoom(zoomLevel)
    }

    private fun getCurrentSession(): CameraCaptureSession {
        val result = if (isRecording) {
            recordCaptureSession
        } else {
            previewCaptureSession
        }

        if (result == null) {
            throw RuntimeException("Session not found")
        }
        return result
    }

    private fun getCurrentRequest(): CaptureRequest.Builder {
        val result: CaptureRequest.Builder? = if (isRecording) {
            recordingCaptureRequest
        } else {
            previewCaptureRequest
        }

        if (result == null) {
            throw RuntimeException("Request not found")
        }
        return result
    }

    private fun getCurrentCameraModel(): TruvideoSdkCameraDevice {
        return cameraModel ?: run {
            throw TruvideoSdkException("Camera model not found")
        }
    }

    private fun getCurrentCameraDevice(): CameraDevice {
        return cameraDevice ?: run {
            throw TruvideoSdkException("Camera device not found")
        }
    }

    private fun performTapToFocus(motionEvent: MotionEvent): Boolean {
        if (!_isCameraOpened.value) return false

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
                val validStated = listOf(
                    CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED,
                    CaptureRequest.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                )

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
            getCurrentSession().setRepeatingRequest(
                build(), captureCallbackHandler, backgroundHandler
            )
        }

        return true
    }

    suspend fun disconnect(device: CameraDevice? = null, stopRecording : Boolean = true) {
        if (!_isCameraOpened.value) return

        val currentCameraDevice : CameraDevice? = device ?: cameraDevice

        Log.d(TAG, "Disconnect camera")

        updateIsBusy(true)

        if (stopRecording && isRecording) {
            stopRecording()
        }

        _isCameraOpened.value = false

        previewCaptureSession?.close()
        previewCaptureSession = null

        recordCaptureSession?.close()
        recordCaptureSession = null

        currentCameraDevice?.close()
        cameraDevice = null

        Log.d(TAG, "disconnect. camera device null")
        updateIsBusy(false)

        stopBackgroundThread()
    }

    private fun initCamera() {
        val camera = cameraModel ?: throw RuntimeException("Cannot init the camera. Info not found")

        val resolution = when (camera.lensFacing) {
            TruvideoSdkCameraLensFacing.BACK -> backResolution
            TruvideoSdkCameraLensFacing.FRONT -> frontResolution
        }

        Log.d(
            TAG,
            "Resolution for ${camera.lensFacing} resolution ${resolution.width} ${resolution.height}"
        )

        previewSize = resolution
        imageSize = resolution
        videoSize = resolution
        imageReader = ImageReader.newInstance(
            resolution.width,
            resolution.height,
            ImageFormat.JPEG,
            1
        )
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

    fun takeImage() {
        serviceCallback.onTakeImageStarted()

        val cameraDevice = getCurrentCameraDevice()
        val cameraModel = getCurrentCameraModel()

        scope.launch {

            updateIsBusy(true)

            imageOrientation = serviceCallback.getSensorRotation()
            imageSensorRotation = cameraModel.sensorOrientation
            imageLensFacing = cameraModel.lensFacing

            fun performTakeImage() {
                val imageRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                if (getCameraFlashMode().isOn) {
                    imageRequest.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE)
                }
                val cropRegion = getZoomRectangle(zoomRatio, getCurrentSensorSize())
                imageRequest.set(CaptureRequest.SCALER_CROP_REGION, cropRegion)

                imageRequest.addTarget(imageReader.surface)
                getCurrentSession().capture(imageRequest.build(), object : CaptureCallback() {
                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        super.onCaptureFailed(session, request, failure)
                        updateIsBusy(false)
                    }
                }, backgroundHandler)
            }

            val flash = getCameraFlashMode().isOn
            if (!flash || isRecording) {
                performTakeImage()
                return@launch
            }

            getCurrentRequest().apply {
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            }

            var focusLocked = false
            getCurrentSession().setRepeatingRequest(
                getCurrentRequest().build(), object : CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
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
                            performTakeImage()

                            getCurrentRequest().apply {
                                set(
                                    CaptureRequest.CONTROL_AF_TRIGGER,
                                    CameraMetadata.CONTROL_AF_TRIGGER_IDLE
                                )
                                if (isRecording) {
                                    if (flash) {
                                        set(
                                            CaptureRequest.FLASH_MODE,
                                            CaptureRequest.FLASH_MODE_TORCH
                                        )
                                    } else {
                                        set(
                                            CaptureRequest.FLASH_MODE,
                                            CaptureRequest.FLASH_MODE_OFF
                                        )
                                    }
                                    set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                                    )
                                } else {
                                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                                    set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                    )
                                }
                            }

                            getCurrentSession().setRepeatingRequest(
                                getCurrentRequest().build(), null, backgroundHandler
                            )
                        }
                    }
                },
                backgroundHandler
            )
        }
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

    suspend fun toggleRecording(
        maxVideoDurationReached: Boolean = false,
    ) {
        if (!_isCameraOpened.value) return

        if (isRecording) {
            stopRecording(maxDurationReached = maxVideoDurationReached)
        } else {
            startRecording()
        }
    }

    fun toggleFlash() {
        if (!_isCameraOpened.value) return

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
                scope.launch {
                    setRepeatingRequest(getCurrentRequest().build(), null, backgroundHandler)
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    suspend fun startPreview() {
        if (!_isCameraOpened.value) return

        val surfaceTexture = textureView.surfaceTexture ?: run {
            throw TruvideoSdkException("Texture not found")
        }

        updateIsBusy(true)

        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(surfaceTexture)

        previewCaptureRequest =
            getCurrentCameraDevice().createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(previewSurface)
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
                set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                val zoomRect = getZoomRectangle(zoomRatio, getCurrentSensorSize())
                set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
            }

        suspendCancellableCoroutine { cont ->
            scope.launch {
                @Suppress("DEPRECATION")
                getCurrentCameraDevice().createCaptureSession(
                    listOf(previewSurface, imageReader.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.d(TAG, "Failed to configure camera preview")
                            updateIsBusy(false)
                            cont.resumeWith(Result.success(Unit))
                        }

                        override fun onConfigured(session: CameraCaptureSession) {
                            Log.d(TAG, "Camera preview configured")
                            previewCaptureSession = session

                            scope.launch {
                                var firstTime = true
                                try {
                                    session.setRepeatingRequest(
                                        getCurrentRequest().build(),
                                        object : CaptureCallback() {
                                            override fun onCaptureStarted(
                                                session: CameraCaptureSession,
                                                request: CaptureRequest,
                                                timestamp: Long,
                                                frameNumber: Long
                                            ) {
                                                if (firstTime) {
                                                    firstTime = false

                                                    Log.d(TAG, "Camera preview started")
                                                    updateIsBusy(false)
                                                    cont.resumeWith(Result.success(Unit))
                                                }
                                            }
                                        },
                                        backgroundHandler
                                    )
                                } catch (e: CameraAccessException) {
                                    e.printStackTrace()
                                    Log.e(
                                        TAG,
                                        "Failed to start camera preview because it couldn't access the camera"
                                    )
                                    updateIsBusy(false)
                                    cont.resumeWith(Result.success(Unit))
                                } catch (e: IllegalStateException) {
                                    e.printStackTrace()
                                    Log.e(TAG, "Failed to start camera preview")
                                    updateIsBusy(false)
                                    cont.resumeWith(Result.success(Unit))
                                }
                            }
                        }
                    },
                    backgroundHandler
                )
            }
        }
    }

    private suspend fun startRecording() {
        if (isRecording) throw TruvideoSdkException(message = "Already recording")

        val cameraModel = getCurrentCameraModel()
        videoSensorRation = cameraModel.sensorOrientation
        videoResolution = videoSize
        videoOrientation = serviceCallback.getSensorRotation()
        videoLensFacing = cameraModel.lensFacing

        updateIsBusy(true)
        updateIsRecording(true)
        setupMediaRecorder()

        val m = mediaRecorder ?: throw RuntimeException("Media recorder not found")
        val surfaceTexture =
            textureView.surfaceTexture ?: throw RuntimeException("Texture not found")
        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(surfaceTexture)
        val recordingSurface = m.surface

        suspendCancellableCoroutine { cont ->
            scope.launch {
                recordingCaptureRequest =
                    getCurrentCameraDevice().createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                        .apply {
                            addTarget(previewSurface)
                            addTarget(recordingSurface)
                            set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                            )
                            set(
                                CaptureRequest.CONTROL_AWB_MODE,
                                CaptureRequest.CONTROL_AWB_MODE_AUTO
                            )
                            val zoomRect = getZoomRectangle(zoomRatio, getCurrentSensorSize())
                            set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
                            if (getCameraFlashMode().isOn) {
                                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                            } else {
                                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                            }
                        }

                @Suppress("DEPRECATION") getCurrentCameraDevice().createCaptureSession(
                    listOf(previewSurface, recordingSurface, imageReader.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.d(TAG, "Failed to configure camera recording")
                            updateIsBusy(false)
                            updateIsRecording(false)
                            cont.resumeWith(Result.success(Unit))
                        }

                        override fun onConfigured(session: CameraCaptureSession) {
                            Log.d(TAG, "Camera recording configured")
                            recordCaptureSession = session

                            scope.launch {
                                var firstTime = true
                                try {
                                    session.setRepeatingRequest(
                                        getCurrentRequest().build(),
                                        object : CaptureCallback() {
                                            override fun onCaptureStarted(
                                                session: CameraCaptureSession,
                                                request: CaptureRequest,
                                                timestamp: Long,
                                                frameNumber: Long
                                            ) {
                                                if (firstTime) {
                                                    Log.d(TAG, "Camera recording started")
                                                    updateIsBusy(false)
                                                    firstTime = false
                                                    serviceCallback.onRecordingStarted()
                                                    m.start()
                                                    startTimer()
                                                    cont.resumeWith(Result.success(Unit))
                                                }
                                            }
                                        },
                                        backgroundHandler
                                    )
                                } catch (e: CameraAccessException) {
                                    e.printStackTrace()
                                    updateIsRecording(false)
                                    updateIsBusy(false)
                                    Log.e(
                                        TAG,
                                        "Failed to start camera recording because it couldn't access the camera"
                                    )
                                    cont.resumeWith(Result.success(Unit))
                                } catch (e: IllegalStateException) {
                                    e.printStackTrace()
                                    updateIsRecording(false)
                                    updateIsBusy(false)
                                    Log.e(TAG, "Failed to start camera recording")
                                    cont.resumeWith(Result.success(Unit))
                                }
                            }
                        }
                    },
                    backgroundHandler
                )
            }
        }
    }

    private suspend fun stopRecording(maxDurationReached: Boolean = false) {
        if (!isRecording) throw TruvideoSdkException(message = "Not recording")

        updateIsBusy(true)
        updateIsPaused(false)
        videoDurationTimer?.pause()

        val result = stopMediaRecording()
        if (result) {
            val duration = getVideoDuration(videoFile)

            val rotation = calculateRotation(
                sensorRotation = videoSensorRation,
                isFront = videoLensFacing.isFront,
                orientation = videoOrientation.rotation
            )

            val fixedResolution = when (rotation) {
                90, 270 -> TruvideoSdkCameraResolution(
                    videoResolution.height,
                    videoResolution.width
                )

                else -> videoResolution
            }

            serviceCallback.onVideo(
                file = videoFile,
                duration = duration,
                maxDurationReached = maxDurationReached,
                orientation = videoOrientation,
                resolution = fixedResolution,
                lensFacing = videoLensFacing
            )
        }

        updateIsBusy(false)
        updateIsRecording(false)
        stopTimer()
    }

    private suspend fun stopMediaRecording(): Boolean = suspendCancellableCoroutine { cont ->
        scope.launch {
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
                    cont.resumeWith(Result.success(true))
                } else {
                    Log.d(TAG, "No video to add")
                    cont.resumeWith(Result.success(false))
                }
            } else {
                cont.resumeWith(Result.success(false))
            }
        }
    }

    private suspend fun setupMediaRecorder() {
        stopMediaRecording()

        val newFile = createVideoFile() ?: throw RuntimeException("Error creating video file")
        videoFile = newFile

        val m = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }

        // Max duration
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
        m.setVideoSize(videoSize.width, videoSize.height)
        val bitRate = when (videoSize.height) {
            480 -> 1_000_000
            720 -> 2_500_000
            1080 -> 4_000_000
            else -> 2_000_000
        }
        m.setVideoEncodingBitRate(bitRate)
        m.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        val orientationHint = calculateRotation(
            sensorRotation = videoSensorRation,
            isFront = videoLensFacing.isFront,
            orientation = videoOrientation.rotation
        )
        m.setOrientationHint(orientationHint)

        // Audio Config
        m.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        m.setAudioSamplingRate(44100)  // 44.1 kHz
        m.setAudioEncodingBitRate(128000)  // 128 kbps

        m.setOutputFile(videoFile.absolutePath)

        if (maxDuration != null) {
            m.setMaxDuration(maxDuration!!)
        }

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
    }

    private fun updateIsPaused(value: Boolean) {
        isPaused = value
        serviceCallback.updateIsPaused(value)
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

    fun pauseRecording() {
        if (!isRecording || isPaused) {
            Log.d(TAG, "Recording not in progress or already paused")
            return
        }

        videoDurationTimer?.pause()

        try {
            mediaRecorder?.pause()
            updateIsPaused(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing video recording", e)
        }
    }

    fun resumeRecording() {
        if (!isRecording || !isPaused) {
            Log.d(TAG, "Recording not paused or not in progress")
            return
        }

        videoDurationTimer?.resume()

        try {
            mediaRecorder?.resume()
            updateIsPaused(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming video recording", e)
        }
    }
}


