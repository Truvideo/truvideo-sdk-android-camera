package com.truvideo.sdk.camera.utils

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.common.Barcode.BarcodeValueType
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.Size
import java.util.Hashtable

object BarcodeUtils {

    /**
     * Generates a bitmap for barcode.
     *
     * @param barcode The barcode to be displayed.
     * @return A Bitmap representing the generated barcode, or null if an error occurred.
     */
    fun generateBarcode(barcode: Barcode) : Bitmap? {
        val value = barcode.displayValue ?: return null
        val format = when (barcode.format) {
            Barcode.FORMAT_CODE_39 -> BarcodeFormat.CODE_39
            Barcode.FORMAT_QR_CODE -> BarcodeFormat.QR_CODE
            else -> { return null }
        }
        val size = getBarcodeSize(format)

        if (format == BarcodeFormat.QR_CODE) {
            return generateQRCodeBitmap(value, size)
        }

        return generateBarcodeBitmap(value, format, size)
    }

    /**
     * Gets the barcode size given by the format of the barcode.
     *
     * @param format The barcode format that is to be displayed.
     * @return a Size class containing the width and height of the barcode
     */
    private fun getBarcodeSize(format: BarcodeFormat) : Size {
        return when(format) {
            BarcodeFormat.QR_CODE -> Size(512, 512)
            BarcodeFormat.CODE_39 -> Size(500, 150)
            else -> { return  Size(0,0)
            }
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