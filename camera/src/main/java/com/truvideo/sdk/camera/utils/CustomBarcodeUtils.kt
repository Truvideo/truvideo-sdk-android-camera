package com.truvideo.sdk.camera.utils

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.Size
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerCode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerCodeFormat
import java.util.Hashtable

internal object CustomBarcodeUtils {

    /**
     * Generates a bitmap for barcode.
     *
     * @param code The code to be displayed.
     * @return A Bitmap representing the generated barcode, or null if an error occurred.
     */
    fun generateBarcode(code: TruvideoSdkCameraScannerCode): Bitmap? {
        val format = when (code.format) {
            TruvideoSdkCameraScannerCodeFormat.CODE_39 -> BarcodeFormat.CODE_39
            TruvideoSdkCameraScannerCodeFormat.CODE_QR -> BarcodeFormat.QR_CODE
            TruvideoSdkCameraScannerCodeFormat.CODE_93 -> BarcodeFormat.CODE_93
            TruvideoSdkCameraScannerCodeFormat.DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
        }

        val size = getBarcodeSize(code.format)

        if (format == BarcodeFormat.QR_CODE) {
            return generateQRCodeBitmap(code.data, size)
        }

        return generateBarcodeBitmap(code.data, format, size)
    }

    /**
     * Gets the barcode size given by the format of the barcode.
     *
     * @param format The barcode format that is to be displayed.
     * @return a Size class containing the width and height of the barcode
     */
    private fun getBarcodeSize(format: TruvideoSdkCameraScannerCodeFormat): Size {
        return when (format) {
            TruvideoSdkCameraScannerCodeFormat.CODE_QR -> Size(512, 512)
            TruvideoSdkCameraScannerCodeFormat.CODE_39 -> Size(500, 150)
            TruvideoSdkCameraScannerCodeFormat.CODE_93 -> Size(500, 150)
            TruvideoSdkCameraScannerCodeFormat.DATA_MATRIX -> Size(512, 512)
        }
    }

    /**
     * Generates a bitmap for a QR code.
     *
     * @param content The content to be encoded in the QR code.
     * @param size The dimensions of the resulting QR code bitmap.
     * @return A Bitmap representing the generated QR code, or null if an error occurred.
     */
    private fun generateQRCodeBitmap(content: String, size: Size): Bitmap? {
        return try {
            val hints = Hashtable<EncodeHintType, String>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size.width, size.height, hints)
            val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.RGB_565)
            for (x in 0 until size.width) {
                for (y in 0 until size.height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) -0x1000000 else -0x1)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a bitmap for a barcode.
     *
     * @param content The content to be encoded in the barcode.
     * @param format The barcode format to be used (e.g., Code 39, QR Code).
     * @param size The dimensions of the resulting barcode bitmap.
     * @return A Bitmap representing the generated barcode, or null if an error occurred.
     */
    private fun generateBarcodeBitmap(content: String, format: BarcodeFormat, size: Size): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, format, size.width, size.height)
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.createBitmap(bitMatrix)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}