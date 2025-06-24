package com.truvideo.sdk.camera.adapters

import android.Manifest
import android.graphics.Rect
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.CamcorderProfile
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.truvideo.sdk.camera.data.mapper.toCaptureRequest
import com.truvideo.sdk.camera.domain.models.AutoFocusMode
import com.truvideo.sdk.camera.domain.models.AutoFocusTrigger
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import com.truvideo.sdk.camera.domain.models.CaptureTemplate
import com.truvideo.sdk.camera.exceptions.TruvideoSdkCameraException
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
import truvideo.sdk.common.exceptions.TruvideoSdkException
import java.io.File
import java.util.concurrent.Executors
import kotlin.collections.contains

interface TruvideoSdkCameraAdapter {
    fun startPreview(
        cameraId: String,
        surface: Surface,
        resolution: TruvideoSdkCameraResolution,
        cameraCaptureConfig: CameraCaptureConfig
    )
    fun getAvailableCameraIds(): List<String>
    fun takeImage(cameraCaptureConfig: CameraCaptureConfig): Flow<ImageCaptureEvent>
    fun takeVideoSnapshot(cameraCaptureConfig: CameraCaptureConfig) : Flow<ImageCaptureEvent>
    fun requestFocus(cameraCaptureConfig: CameraCaptureConfig) : Flow<FocusState>
    fun getCameraCharacteristics(cameraId: String): CameraInformation
    fun release()
    fun startCameraById(
        cameraId: String,
        resolution: TruvideoSdkCameraResolution,
        cameraCaptureConfig: CameraCaptureConfig
    )
    fun requestFocusOnPosition(cameraCaptureConfig: CameraCaptureConfig): Flow<FocusState>
    fun setFlash(cameraCaptureConfig: CameraCaptureConfig): Flow<Boolean>
    fun setZoomLevel(cameraCaptureConfig: CameraCaptureConfig): Flow<Boolean>
    fun startRecording(
        cameraCaptureConfig: CameraCaptureConfig,
        outputFile: File,
        orientation: Int,
        durationLimit: Int?,
        resolution: TruvideoSdkCameraResolution,
    )
    fun pauseRecording()
    fun restartPreview(restartCamera: Boolean,
                       cameraId: String?,
                       resolution: TruvideoSdkCameraResolution?,
                       cameraCaptureConfig: CameraCaptureConfig)
    fun resumeRecording(
        cameraCaptureConfig: CameraCaptureConfig,
        outputFile: File,
        orientation: Int,
        durationLimit: Int?,
        resolution: TruvideoSdkCameraResolution,
    )
    fun stopRecording(maxDurationReached: Boolean)
    val recordingEvents: SharedFlow<RecordingEvent>
    val cameraEvents: SharedFlow<CameraEvent>

}

data class CameraInformation(
    val cameraId: String,
    val lensFacing: TruvideoSdkCameraLensFacing?,
    val sensorOrientation: Int,
    val supportedSizes: List<TruvideoSdkCameraResolution>,
    val supportsFlash: Boolean,
    val supportsManualFocus: Boolean,
    val sensorSize: Rect,
    val isLogicalCamera: Boolean,
)

class TruvideoSdkCameraAdapterImpl (
    private val cameraManager: CameraManager,
    private val mediaRecorder: TruvideoSdkCameraRecorderAdapter
) : TruvideoSdkCameraAdapter {

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSurface: Surface? = null
    private var imageCaptureSurface: Surface? = null
    private var recordingSurface: Surface? = null
    private var imageReader: ImageReader? = null

    private val backgroundThread = HandlerThread("CameraThread").apply { start() }
    val cameraHandler = Handler(backgroundThread.looper)
    val cameraExecutor = Executors.newSingleThreadExecutor()

    private val _recordingEvents = MutableSharedFlow<RecordingEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val recordingEvents = _recordingEvents.asSharedFlow()

    private val _cameraEvents = MutableSharedFlow<CameraEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val cameraEvents = _cameraEvents.asSharedFlow()

    companion object {
        const val TAG: String = "[TruvideoSdkCamera][TruvideoSdkCameraAdapter]"
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    override fun startPreview(
        cameraId: String,
        surface: Surface,
        resolution: TruvideoSdkCameraResolution,
        cameraCaptureConfig: CameraCaptureConfig
    ) {
        previewSurface = surface
        startCameraById(cameraId, resolution, cameraCaptureConfig)
    }

    private fun createPreviewSession(
        captureConfig: CameraCaptureConfig
    ) {
        val device = cameraDevice ?: return throw IllegalStateException("CameraDevice is null")
        val surface = previewSurface ?: return throw IllegalStateException("Surface is null")
        val imageSurface = imageReader?.surface ?: return throw IllegalStateException("Image surface is null")

        imageCaptureSurface = imageSurface

        try {
            val previewRequest = captureConfig.toCaptureRequest(device, listOf(surface))

            @Suppress("deprecation")
            device.createCaptureSession(listOf(surface,imageSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    session.setRepeatingRequest(previewRequest.build(), null, cameraHandler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    throw TruvideoSdkCameraException("Preview session configuration failed")
                }

            }, cameraHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun getAvailableCameraIds(): List<String> = cameraManager.cameraIdList.toList()

    private fun cameraSupportsFocus(cameraId: String) : Boolean {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)

        val availableAfModes = characteristics.get(
            CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES
        )

        return availableAfModes != null && availableAfModes.any { it != CameraMetadata.CONTROL_AF_MODE_OFF }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun takeImage(cameraCaptureConfig: CameraCaptureConfig): Flow<ImageCaptureEvent>  = callbackFlow {
        val device = cameraDevice ?: run {
            close()
            return@callbackFlow
        }

        if (!cameraSupportsFocus(device.id)) {
            takeImageFlow(cameraCaptureConfig)
                .onEach { trySend(it) }
                .collect()

            return@callbackFlow
        }

        requestFocus(cameraCaptureConfig)
            .map { it.asImageCaptureEvent() }
            .onEach { trySend(it) }
            .filter { it == ImageCaptureEvent.CaptureFocusLocked }
            .flatMapLatest {
                takeImageFlow(cameraCaptureConfig)
                    .onEach { trySend(it) }
            }.collect()
    }

    override fun takeVideoSnapshot(cameraCaptureConfig: CameraCaptureConfig): Flow<ImageCaptureEvent> = takeImageFlow(cameraCaptureConfig)

    private fun takeImageFlow(
        cameraCaptureConfig: CameraCaptureConfig
    ) : Flow<ImageCaptureEvent> = flow {
        val device = cameraDevice ?: run {
            emit(ImageCaptureEvent.Exception(IllegalStateException("CameraDevice is null")))
            return@flow
        }

        val imageReader = imageReader ?: run {
            emit(ImageCaptureEvent.Exception(IllegalStateException("Image Surface is null")))
            return@flow
        }

        val captureSession = captureSession ?: run {
            emit(ImageCaptureEvent.Exception(IllegalStateException("No capture session")))
            return@flow
        }

        val handler = cameraHandler

        takeSingleImage(
            device = device,
            captureSession = captureSession,
            imageReader = imageReader,
            cameraCaptureConfig = cameraCaptureConfig,
            handler = handler
        ).collect(this)
    }

    private fun takeSingleImage(
        device: CameraDevice,
        captureSession: CameraCaptureSession,
        imageReader: ImageReader,
        cameraCaptureConfig: CameraCaptureConfig,
        handler: Handler
    ) : Flow<ImageCaptureEvent> = callbackFlow {

        val imageListener = ImageReader.OnImageAvailableListener { reader ->
            val image: Image = reader.acquireLatestImage()
            try { trySend(ImageCaptureEvent.Captured(image)) }catch (e: Exception)  {
                e.printStackTrace()
                trySend(ImageCaptureEvent.Exception(TruvideoSdkException("Error while capturing image. Image reader returned null")))
                close()
            }
        }

        imageReader.setOnImageAvailableListener(imageListener, cameraHandler)

        Log.d(TAG, "takeSingleImage: taking image")
        try {
            trySend(ImageCaptureEvent.CaptureStarted)
            val captureRequest =
                cameraCaptureConfig
                    .toCaptureRequest(device, listOf(imageReader.surface, previewSurface!!), imageTemplateType)

            captureSession.capture(
                captureRequest.build(),
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        Log.d(TAG, "takeSingleImage: image captured")
                    }

                    override fun onCaptureStarted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        timestamp: Long,
                        frameNumber: Long
                    ) {
                        super.onCaptureStarted(session, request, timestamp, frameNumber)
                        Log.d(TAG, "takeSingleImage: image capture started")
                    }

                    override fun onCaptureSequenceAborted(
                        session: CameraCaptureSession,
                        sequenceId: Int
                    ) {
                        super.onCaptureSequenceAborted(session, sequenceId)
                        Log.d(TAG, "takeSingleImage: image capture aborted")
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        trySend(ImageCaptureEvent.Exception(TruvideoSdkException("Capture failed: ${failure.reason}")))
                        close()
                    }
            }, handler)

        } catch (e: CameraAccessException) {
            trySend(ImageCaptureEvent.Exception(e))
            close()
        }

        awaitClose {
            imageReader.setOnImageAvailableListener(null, null)
        }
    }

    override fun getCameraCharacteristics(cameraId: String): CameraInformation {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

        val resolutions = getSupportedResolutions(cameraId)
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!
        val isTapToFocusEnabled = (characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) ?: 0) >= 1
        val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        val isLogicalCamera = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) == true
        } else { false }

        val lensFacing = when (cameraLensFacing) {
            CameraCharacteristics.LENS_FACING_FRONT -> TruvideoSdkCameraLensFacing.FRONT
            CameraCharacteristics.LENS_FACING_BACK -> TruvideoSdkCameraLensFacing.BACK
            else -> null
        }

        return CameraInformation(
            cameraId = cameraId,
            lensFacing = lensFacing,
            sensorOrientation = sensorOrientation,
            supportedSizes = resolutions,
            supportsFlash = hasFlash,
            supportsManualFocus = isTapToFocusEnabled,
            sensorSize = sensorSize,
            isLogicalCamera = isLogicalCamera
        )
    }

    private fun getSupportedResolutions(cameraId: String): List<TruvideoSdkCameraResolution> {
        val qualities = listOf(
            CamcorderProfile.QUALITY_480P,
            CamcorderProfile.QUALITY_720P,
            CamcorderProfile.QUALITY_1080P,
        );

        val result = mutableListOf<TruvideoSdkCameraResolution>()

        qualities.forEach { quality ->
            if (CamcorderProfile.hasProfile(cameraId.toInt(), quality)) {
                val sizes = mutableListOf<Size>()

                val profile = CamcorderProfile.get(cameraId.toInt(), quality)
                sizes.add(Size(profile.videoFrameWidth, profile.videoFrameHeight))
                sizes.forEach { size ->
                    if (!result.any { r -> r.width == size.width && r.height == size.height }) {
                        result.add(TruvideoSdkCameraResolution(size.width, size.height))
                    }
                }
            }
        }

        return result.sortResolutions()
    }

    override fun requestFocus(
        cameraCaptureConfig: CameraCaptureConfig
    ) : Flow<FocusState> = callbackFlow {
        val device = cameraDevice ?: run {
            close()
            return@callbackFlow
        }
        val surface = previewSurface ?: run {
            close()
            return@callbackFlow
        }

        val captureRequest =
            cameraCaptureConfig.copy(
                autoFocus = AutoFocusMode.Auto,
                autoFocusTrigger = AutoFocusTrigger.Start
            ).toCaptureRequest(
                device, listOf(surface)
            ).build()

        Log.d(TAG, "requestFocus: requesting focus")

        trySend(FocusState.Started)
        var focusLocked = false
        captureSession?.setRepeatingRequest(
            captureRequest,
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    if (focusLocked) return
                    val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                    if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                        afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        trySend(FocusState.FocusedLocked)
                        Log.d(TAG, "requestFocus: focus locked")
                        focusLocked = true
                        close()
                    }
                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    super.onCaptureFailed(session, request, failure)
                    trySend(FocusState.Failed)
                    close()
                }
            },
            cameraHandler
        )

        awaitClose {}
    }

    override fun release() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
    }

    override fun startCameraById(
        cameraId: String,
        resolution: TruvideoSdkCameraResolution,
        cameraCaptureConfig: CameraCaptureConfig
    ) {
        imageReader = imageReader.configureImageCapture(
            width = resolution.width,
            height = resolution.height,
            handler = cameraHandler
        )

        cameraDevice?.close()

        val callback = object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                cameraDevice = device
                _cameraEvents.tryEmit(CameraEvent.Connected)
                createPreviewSession(cameraCaptureConfig)
            }

            override fun onDisconnected(device: CameraDevice) {
                Log.d(TAG, "onDisconnected: camera ${device.id} was disconnected")
                _cameraEvents.tryEmit(CameraEvent.Disconnected)
                device.close()
                cameraDevice = null
            }

            override fun onError(device: CameraDevice, error: Int) {
                Log.d(TAG, "onError: camera ${device.id} encountered an error $error")
                _cameraEvents.tryEmit(CameraEvent.Error(TruvideoSdkException("Camera error: $error")))
                device.close()
                cameraDevice = null
            }
        }

        try {
            cameraManager.openCamera(cameraId, callback, cameraHandler)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun requestFocusOnPosition(
        cameraCaptureConfig: CameraCaptureConfig
    ): Flow<FocusState> = callbackFlow {
        val device = cameraDevice ?: run {
            close()
            return@callbackFlow
        }
        val surface = previewSurface ?: run {
            close()
            return@callbackFlow
        }

        val captureRequest =
            cameraCaptureConfig.copy(
                autoFocus = AutoFocusMode.Auto,
                autoFocusTrigger = AutoFocusTrigger.Start
            )
            .toCaptureRequest(
                device, listOf(surface)
            ).build()

        Log.d(TAG, "requestFocusOnPosition: requesting focus")

        trySend(FocusState.Started)
        var focusLocked = false
        captureSession?.setRepeatingRequest(
            captureRequest,
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    if (focusLocked) return
                    val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                    if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                        afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        trySend(FocusState.FocusedLocked)

                        Log.d(TAG, "requestFocus: focus locked completed")
                        focusLocked = true
                        val newRequest =
                            cameraCaptureConfig.copy(
                                autoFocus = AutoFocusMode.Auto,
                                autoFocusTrigger = AutoFocusTrigger.Off,
                            ).toCaptureRequest(device, listOf(surface)).build()

                        session.setRepeatingRequest(newRequest, object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                result: TotalCaptureResult
                            ) {
                                super.onCaptureCompleted(session, request, result)
                                trySend(FocusState.Idle)
                                close()
                            }

                            override fun onCaptureFailed(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                failure: CaptureFailure
                            ) {
                                super.onCaptureFailed(session, request, failure)
                                trySend(FocusState.Failed)
                                close()
                            }
                        }, cameraHandler)
                    }
                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    Log.d(TAG, "requestFocusOnPosition: requesting focus failed")
                    super.onCaptureFailed(session, request, failure)
                    trySend(FocusState.Failed)
                    close()
                }
            },
            cameraHandler
        )

        awaitClose {}
    }

    override fun setFlash(
        cameraCaptureConfig: CameraCaptureConfig
    ): Flow<Boolean> = callbackFlow {
        val device = cameraDevice ?: run {
            close()
            return@callbackFlow
        }

        val surface = previewSurface ?: run {
            close()
            return@callbackFlow
        }

        val captureRequest =
            cameraCaptureConfig
                .toCaptureRequest(device, listOf(surface))
                    .build()

        captureSession?.setRepeatingRequest(
            captureRequest,
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    trySend(true)
                    close()
                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    super.onCaptureFailed(session, request, failure)
                    trySend(false)
                    close()
                }
            },
            cameraHandler
        )

        awaitClose()
    }

    override fun setZoomLevel(captureConfig: CameraCaptureConfig): Flow<Boolean> = callbackFlow {
        val device = cameraDevice ?: run {
            close()
            return@callbackFlow
        }

        val surface = previewSurface ?: run {
            close()
            return@callbackFlow
        }

        val captureRequest =
            captureConfig.toCaptureRequest(device, listOf(surface)).build()

        captureSession?.setRepeatingRequest(
            captureRequest,
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    trySend(true)
                    close()
                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    super.onCaptureFailed(session, request, failure)
                    trySend(false)
                    close()
                }
            },
            cameraHandler
        )

        awaitClose()

    }

    override fun startRecording(
        cameraCaptureConfig: CameraCaptureConfig,
        outputFile: File,
        orientation: Int,
        durationLimit: Int?,
        resolution: TruvideoSdkCameraResolution,
    ) {

        recordingSurface = mediaRecorder.prepare(
            outputFile = outputFile,
            orientation = orientation,
            durationLimit = durationLimit,
            videoWidth = resolution.width,
            videoHeight = resolution.height
        ) {
            _recordingEvents.tryEmit(RecordingEvent.MaxDurationReached)
        }

        val videoSurface = recordingSurface ?: return
        val device = cameraDevice ?: return
        val surface = previewSurface ?: return
        val imageSurface = imageReader?.surface ?: return

        try {
            val previewRequest =
                cameraCaptureConfig
                    .toCaptureRequest(device, listOf(surface, videoSurface))
                    .build()

            device.createCameraSession(
                listOf(surface, videoSurface, imageSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        var firstTime = true
                        session.setRepeatingRequest(previewRequest, object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                result: TotalCaptureResult
                            ) {
                                super.onCaptureCompleted(session, request, result)
                                if (!firstTime) return
                                mediaRecorder.start{
                                    Log.d(TAG, "Media recording started successfully")
                                    _recordingEvents.tryEmit(RecordingEvent.Started)
                                    firstTime = false
                                }
                            }

                            override fun onCaptureStarted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                timestamp: Long,
                                frameNumber: Long
                            ) {
                                super.onCaptureStarted(session, request, timestamp, frameNumber)

                            }

                            override fun onCaptureSequenceAborted(
                                session: CameraCaptureSession,
                                sequenceId: Int
                            ) {
                                super.onCaptureSequenceAborted(session, sequenceId)
                            }

                            override fun onCaptureFailed(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                failure: CaptureFailure
                            ) {
                                super.onCaptureFailed(session, request, failure)
                                Log.d(TAG, "onCaptureFailed: Media recording failed with error ${failure.reason}")
                                _recordingEvents.tryEmit(RecordingEvent.Started)
                            }
                        }, cameraHandler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        _recordingEvents.tryEmit(RecordingEvent.Exception(TruvideoSdkCameraException("Preview session configuration failed")))
                    }
                }
            )

        } catch (e: CameraAccessException) {
            Log.d(TAG, "onCaptureException: Media recording failed with error ${e.reason}")

            _recordingEvents.tryEmit(RecordingEvent.Exception(e))
        }

    }

    private fun CameraDevice.createCameraSession(
        surfaceList: List<Surface>,
        callback: CameraCaptureSession.StateCallback,
    ) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            createCaptureSession(
                SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    surfaceList.map {
                        OutputConfiguration(it)
                    },
                    cameraExecutor,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(p0: CameraCaptureSession) {
                            TODO("Not yet implemented")
                        }

                        override fun onConfigureFailed(p0: CameraCaptureSession) {
                            TODO("Not yet implemented")
                        }

                    }
                )
            )
            return
        }

        createCaptureSession(surfaceList, callback, cameraHandler)
    }

    override fun pauseRecording() {
        mediaRecorder.stop {
            _recordingEvents.tryEmit(RecordingEvent.Paused)
        }
    }

    override fun restartPreview(
        restartCamera: Boolean,
        cameraId: String?,
        resolution: TruvideoSdkCameraResolution?,
        cameraCaptureConfig: CameraCaptureConfig) {
        if (restartCamera) {
            startCameraById(
                cameraId = cameraId!!, resolution = resolution!!,
                cameraCaptureConfig = cameraCaptureConfig
            )
            return
        }
        createPreviewSession(cameraCaptureConfig)
    }


    override fun resumeRecording(
        cameraCaptureConfig: CameraCaptureConfig,
        outputFile: File,
        orientation: Int,
        durationLimit: Int?,
        resolution: TruvideoSdkCameraResolution
    ) {
        recordingSurface = mediaRecorder.prepare(
            outputFile = outputFile,
            orientation = orientation,
            durationLimit = durationLimit,
            videoWidth = resolution.width,
            videoHeight = resolution.height
        ) {
            _recordingEvents.tryEmit(RecordingEvent.MaxDurationReached)
        }

        val videoSurface = recordingSurface ?: return
        val device = cameraDevice ?: return
        val surface = previewSurface ?: return
        val imageSurface = imageReader?.surface ?: return

        try {
            val previewRequest =
                cameraCaptureConfig
                    .toCaptureRequest(device, listOf(surface, videoSurface))
                    .build()

            device.createCameraSession(
                listOf(surface, videoSurface, imageSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        var firstTime = true
                        session.setRepeatingRequest(previewRequest, object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                result: TotalCaptureResult
                            ) {
                                super.onCaptureCompleted(session, request, result)
                                if (!firstTime) return
                                mediaRecorder.start{
                                    Log.d(TAG, "Media recording started successfully")
                                    _recordingEvents.tryEmit(RecordingEvent.Resumed)
                                    firstTime = false
                                }
                            }

                            override fun onCaptureStarted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                timestamp: Long,
                                frameNumber: Long
                            ) {
                                super.onCaptureStarted(session, request, timestamp, frameNumber)

                            }

                            override fun onCaptureSequenceAborted(
                                session: CameraCaptureSession,
                                sequenceId: Int
                            ) {
                                super.onCaptureSequenceAborted(session, sequenceId)
                            }

                            override fun onCaptureFailed(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                failure: CaptureFailure
                            ) {
                                super.onCaptureFailed(session, request, failure)
                                Log.d(TAG, "onCaptureFailed: Media recording failed with error ${failure.reason}")
                                _recordingEvents.tryEmit(RecordingEvent.Exception(TruvideoSdkCameraException("Media recording failed with error ${failure.reason}")))
                            }
                        }, cameraHandler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        _recordingEvents.tryEmit(RecordingEvent.Exception(TruvideoSdkCameraException("Preview session configuration failed")))
                    }
                }
            )

        } catch (e: CameraAccessException) {
            Log.d(TAG, "onCaptureException: Media recording failed with error ${e.reason}")

            _recordingEvents.tryEmit(RecordingEvent.Exception(e))
        }
    }

    override fun stopRecording(maxDurationReached: Boolean) {
        mediaRecorder.stop()
        _recordingEvents.tryEmit(RecordingEvent.Stopped(maxDurationReached))
    }

    private val isRecording : Boolean
        get() = recordingSurface != null

    private val requestTemplateType: Int
        get() = if (isRecording) CameraDevice.TEMPLATE_RECORD else CameraDevice.TEMPLATE_PREVIEW

    private val imageTemplateType: CaptureTemplate
        get() = if(isRecording) CaptureTemplate.Snapshot else CaptureTemplate.StillCapture

    private fun FocusState.asImageCaptureEvent() : ImageCaptureEvent =
        when(this) {
            FocusState.Failed -> ImageCaptureEvent.CaptureFocusFailed
            FocusState.FocusedLocked -> ImageCaptureEvent.CaptureFocusLocked
            FocusState.Started -> ImageCaptureEvent.CaptureFocusStarted
            FocusState.Idle -> ImageCaptureEvent.CaptureFocusIdle
        }

}

sealed interface ImageCaptureEvent {
    data class Captured(val image: Image): ImageCaptureEvent
    object CaptureStarted: ImageCaptureEvent
    object CaptureFailed: ImageCaptureEvent
    object CaptureFocusStarted: ImageCaptureEvent
    object CaptureFocusLocked: ImageCaptureEvent
    object CaptureFocusFailed: ImageCaptureEvent
    object CaptureFocusIdle: ImageCaptureEvent
    data class Exception(val throwable: Throwable): ImageCaptureEvent
}

fun List<TruvideoSdkCameraResolution>.sortResolutions() : List<TruvideoSdkCameraResolution> {
    return sortedWith(compareBy { it.height * it.width }).reversed().toList()
}

@Serializable
sealed class FocusState {
    @Serializable
    object Started : FocusState()
    @Serializable
    object FocusedLocked : FocusState()
    @Serializable
    object Failed : FocusState()
    @Serializable
    object Idle : FocusState()
}

sealed interface RecordingEvent {
    object Started : RecordingEvent
    object Resumed : RecordingEvent
    object MaxDurationReached : RecordingEvent
    object Paused: RecordingEvent
    data class Stopped(val maxDurationReached: Boolean = false): RecordingEvent
    data class Exception(val throwable: Throwable): RecordingEvent
}

sealed interface CameraEvent {
    data object Connected: CameraEvent
    data object Disconnected: CameraEvent
    data class Error(val throwable: Throwable): CameraEvent
}