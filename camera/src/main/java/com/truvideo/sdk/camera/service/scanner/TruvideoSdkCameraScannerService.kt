package com.truvideo.sdk.camera.service.scanner

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerCode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerCodeFormat
import com.truvideo.sdk.camera.service.camera.TruvideoSdkCameraService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import truvideo.sdk.common.exceptions.TruvideoSdkException

class TruvideoSdkCameraScannerService(
    private val context: Context,
    val information: TruvideoSdkCameraInformation,
    private val textureView: TextureView,
    private val serviceCallback: TruvideoSdkCameraScannerCallback,
) {

    companion object {
        const val TAG = "[TruvideoSdkCamera][TruvideoSdkCameraScannerService]"
    }

    private var isBusy: Boolean = true

    private var backgroundHandlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    private lateinit var barcodeScanner: BarcodeScanner

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private var cameraDevice: CameraDevice? = null
    private var cameraModel: TruvideoSdkCameraDevice? = null

    private var previewCaptureRequest: CaptureRequest.Builder? = null
    private var previewCaptureSession: CameraCaptureSession? = null

    private lateinit var previewSize: TruvideoSdkCameraResolution
    private lateinit var imageSize: TruvideoSdkCameraResolution
    private lateinit var videoSize: TruvideoSdkCameraResolution
    private lateinit var scannerImageReader: ImageReader
    lateinit var flashMode: TruvideoSdkCameraFlashMode
    private var scanningEnabled = true

    private fun getCurrentCameraModel(): TruvideoSdkCameraDevice {
        return cameraModel ?: throw RuntimeException("Camera model not found")
    }

    private val onQRImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image: Image = reader.acquireLatestImage()
        val orientation = serviceCallback.getSensorRotation().getMediaRotation(cameraModel!!.sensorOrientation)
        val mediaImage = InputImage.fromMediaImage(image, orientation)
        processImage(mediaImage)
        image.close()
    }

    private var processingImage = false

    private fun processImage(image: InputImage, callback: (() -> Unit)? = null) {
        if (processingImage) {
            Log.d(TAG, "Cant process image. im busy with the previous one")
            return
        }

        processingImage = true
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                processingImage = false

                if (barcodes.isNotEmpty()) {
                    Log.d(TAG, "processImage: barcode list size ${barcodes.size}")
                    val code = barcodes.first()
                    val codeFormat = when (code.format) {
                        Barcode.FORMAT_CODE_39 -> TruvideoSdkCameraScannerCodeFormat.CODE_39
                        Barcode.FORMAT_CODE_93 -> TruvideoSdkCameraScannerCodeFormat.CODE_93
                        Barcode.FORMAT_QR_CODE -> TruvideoSdkCameraScannerCodeFormat.CODE_QR
                        Barcode.FORMAT_DATA_MATRIX -> TruvideoSdkCameraScannerCodeFormat.DATA_MATRIX
                        else -> null
                    }

                    if (codeFormat != null) {
                        val model = TruvideoSdkCameraScannerCode(
                            data = code.rawValue ?: "",
                            format = codeFormat
                        )
                        serviceCallback.onCodeScanned(model)
                    } else {
                        Log.d(TAG, "Unknown code scanned. Format: ${code.format}. Value: ${code.rawValue}")
                    }
                }
                callback?.invoke()
            }
            .addOnFailureListener {
                processingImage = false

                callback?.invoke()
            }
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

    private fun startBackgroundThread() {
        stopBackgroundThread()

        val name = "CameraServiceThread-${System.currentTimeMillis()}"
        Log.d(TAG, "Thread name $name")

        val thread = HandlerThread(name)
        thread.start()
        backgroundHandler = Handler(thread.looper)
        backgroundHandlerThread = thread
    }

    private fun updateIsBusy(value: Boolean) {
        isBusy = value
        serviceCallback.updateIsBusy(value)
    }

    private fun initCamera() {
        val camera = cameraModel ?: throw RuntimeException("Cannot init the camera. Info not found")

        var resolutions = information.backCamera?.resolutions ?: listOf()
        resolutions = resolutions.sortedByDescending { it.width * it.height }.toList()

        val resolution = resolutions.first()
        serviceCallback.updateResolution(resolution)

        Log.d(
            TAG,
            "Resolution for ${camera.lensFacing} resolution ${resolution.width} ${resolution.height}"
        )

        previewSize = resolution
        imageSize = resolution
        videoSize = resolution

        scannerImageReader = ImageReader.newInstance(
            resolution.width,
            resolution.height,
            ImageFormat.YUV_420_888,
            1
        )
        scannerImageReader.setOnImageAvailableListener(onQRImageAvailableListener, backgroundHandler)
    }

    @SuppressLint("MissingPermission")
    fun openCamera(callback: (() -> Unit)? = null) {
        startBackgroundThread()
        updateIsBusy(true)

        val newCamera = information.getDeviceFromFacing(TruvideoSdkCameraLensFacing.BACK)
            ?: throw RuntimeException("Cannot open the camera. Info for lens facing back not found")

        cameraDevice?.close()
        cameraDevice = null

        cameraModel = newCamera
        initCamera()

        cameraManager.openCamera(
            newCamera.id,
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    scope.launch {
                        startScanningPreview(callback = callback)
                    }

                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.d(TruvideoSdkCameraService.TAG, "Camera disconnected")
                    updateIsBusy(false)
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.d(TruvideoSdkCameraService.TAG, "Error opening the camera. Error $error")
                    updateIsBusy(false)
                }
            }, backgroundHandler
        )
    }

    private fun getCurrentCameraDevice(): CameraDevice {
        return cameraDevice ?: throw RuntimeException("Camera device not found")
    }

    private fun startScanningPreview(
        callback: (() -> Unit)? = null
    ) {
        val surfaceTexture = textureView.surfaceTexture ?: throw RuntimeException("Texture not found")
        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(surfaceTexture)

        previewCaptureRequest = getCurrentCameraDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(previewSurface)
            if (scanningEnabled) {
                addTarget(scannerImageReader.surface)
            }
            set(CaptureRequest.CONTROL_AF_MODE, CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
            set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
        }

        val items = if (scanningEnabled) {
            listOf(previewSurface, scannerImageReader.surface)
        } else {
            listOf(previewSurface)
        }

        @Suppress("DEPRECATION")
        getCurrentCameraDevice().createCaptureSession(
            items,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.d(TruvideoSdkCameraService.TAG, "Failed to configure camera preview")
                    updateIsBusy(false)
                    if (callback != null) callback()
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    Log.d(TruvideoSdkCameraService.TAG, "Camera preview configured")
                    previewCaptureSession = session

                    var firstTime = true
                    try {
                        session.setRepeatingRequest(
                            getCurrentRequest().build(), object : CameraCaptureSession.CaptureCallback() {
                                override fun onCaptureStarted(
                                    session: CameraCaptureSession,
                                    request: CaptureRequest,
                                    timestamp: Long,
                                    frameNumber: Long
                                ) {
                                    if (firstTime) {
                                        firstTime = false
                                        Log.d(TruvideoSdkCameraService.TAG, "Camera preview started")
                                        updateIsBusy(false)
                                        if (callback != null) callback()
                                    }
                                }

                            }, backgroundHandler
                        )
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                        Log.e(
                            TruvideoSdkCameraService.TAG,
                            "Failed to start camera preview because it couldn't access the camera"
                        )
                        updateIsBusy(false)
                        if (callback != null) callback()
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                        Log.e(TruvideoSdkCameraService.TAG, "Failed to start camera preview")
                        updateIsBusy(false)
                        if (callback != null) callback()
                    }
                }
            },
            backgroundHandler
        )
    }

    fun enableScanning() {
        if (scanningEnabled) return
        scanningEnabled = true
        startScanningPreview()
    }

    fun disableScanning() {
        if (!scanningEnabled) return
        scanningEnabled = false
        startScanningPreview()
    }

    private fun getCurrentRequest(): CaptureRequest.Builder {
        return previewCaptureRequest ?: throw RuntimeException("Request not found")
    }

    fun disconnect(
        callback: (() -> Unit)? = null
    ) {
        Log.d(TruvideoSdkCameraService.TAG, "Disconnect camera")

        updateIsBusy(true)

        scope.launch {

            previewCaptureSession?.close()
            previewCaptureSession = null

            cameraDevice?.close()
            cameraDevice = null

            updateIsBusy(false)

            stopBackgroundThread()
            callback?.invoke()
        }
    }

    private fun updateFlashMode(value: TruvideoSdkCameraFlashMode) {
        Log.d(TAG, "Update flash mode $value")
        flashMode = value
        serviceCallback.updateFlashMode(value)
    }

    private fun getCameraFlashMode(): TruvideoSdkCameraFlashMode {
        val model = getCurrentCameraModel()

        val currentValue = flashMode

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
            if (newFlashMode.isOn) {
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            } else {
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            }
        }

        try {
            previewCaptureSession?.apply {
                setRepeatingRequest(getCurrentRequest().build(), null, backgroundHandler)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun buildCodeScanner(formats: List<TruvideoSdkCameraScannerCodeFormat>) {
        if (formats.isEmpty()) throw TruvideoSdkException("Scanner formats list can not be empty")

        val scanningFormats = formats.map { fmt ->
            when (fmt) {
                TruvideoSdkCameraScannerCodeFormat.CODE_39 -> Barcode.FORMAT_CODE_39
                TruvideoSdkCameraScannerCodeFormat.CODE_93 -> Barcode.FORMAT_CODE_93
                TruvideoSdkCameraScannerCodeFormat.CODE_QR -> Barcode.FORMAT_QR_CODE
                TruvideoSdkCameraScannerCodeFormat.DATA_MATRIX -> Barcode.FORMAT_DATA_MATRIX
            }
        }

        val firstFormat = scanningFormats.first()
        val remainingFormats = if (scanningFormats.size > 1) {
            scanningFormats.subList(1, scanningFormats.size).toIntArray()
        } else {
            listOf<Int>().toIntArray()
        }

        val scanningOptions: BarcodeScannerOptions =
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(firstFormat, *remainingFormats)
                .build()

        barcodeScanner = BarcodeScanning.getClient(scanningOptions)
    }
}