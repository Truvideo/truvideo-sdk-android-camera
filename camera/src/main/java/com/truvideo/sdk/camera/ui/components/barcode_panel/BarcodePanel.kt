package com.truvideo.sdk.camera.ui.components.barcode_panel

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.common.internal.BarcodeSource
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.ui.components.panel.Panel
import com.truvideo.sdk.camera.utils.BarcodeUtils
import com.truvideo.sdk.components.button.TruvideoButton


@Composable
fun BarcodePanel(
    visible: Boolean = true,
    onConfirm: (() -> Unit) = {},
    close: (() -> Unit) = {},
    orientation: TruvideoSdkCameraOrientation,
    enabled: Boolean = true,
    barcode: Barcode?
) {

    BackHandler(visible) {
        if (enabled) {
            close()
        }
    }

    Panel(
        visible = visible,
        orientation = orientation,
        close = { close() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "SCAN SUCCESSFUL", color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Box(modifier = Modifier.height(32.dp))

            Text(
                "Would you like to confirm the scan for the code with value \"${barcode?.rawValue}\"", color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Box(modifier = Modifier.height(16.dp))

            if (barcode != null) {
                BarcodeView(code = barcode)
            }

            Box(modifier = Modifier.height(8.dp))

            Text(
                "Barcode may differ but will contain the same information as the original one", color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(horizontal = 48.dp),
                textAlign = TextAlign.Center
            )

            Box(modifier = Modifier.height(48.dp))

            Box(modifier = Modifier.widthIn(max = 300.dp)) {
                TruvideoButton(
                    text = "CONFIRM",
                    selected = true,
                    selectedColor = Color(0xFFFFC107),
                    selectedTextColor = Color.White,
                    enabled = enabled,
                    onPressed = { onConfirm() }
                )
            }

            Box(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.widthIn(max = 300.dp)) {
                TruvideoButton(
                    text = "CANCEL",
                    enabled = enabled,
                    onPressed = { close() }
                )
            }
        }
    }
}

@Composable
private fun BarcodeView(code: Barcode) {
    var barcodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        barcodeBitmap = BarcodeUtils.generateBarcode(code)
        isLoading = false
    }

    Box(
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            barcodeBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Barcode",
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
@Preview
private fun Test() {
    var visible by remember { mutableStateOf(true) }
    var orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }

    Column {
        Box(
            modifier = Modifier
                .width(400.dp)
                .height(800.dp)
        ) {
            BarcodePanel(
                visible = visible,
                orientation = orientation,
                barcode = Barcode(object : BarcodeSource {
                    override fun getFormat(): Int {
                        TODO("Not yet implemented")
                    }

                    override fun getValueType(): Int {
                        TODO("Not yet implemented")
                    }

                    override fun getBoundingBox(): Rect? {
                        TODO("Not yet implemented")
                    }

                    override fun getCalendarEvent(): Barcode.CalendarEvent? {
                        TODO("Not yet implemented")
                    }

                    override fun getContactInfo(): Barcode.ContactInfo? {
                        TODO("Not yet implemented")
                    }

                    override fun getDriverLicense(): Barcode.DriverLicense? {
                        TODO("Not yet implemented")
                    }

                    override fun getEmail(): Barcode.Email? {
                        TODO("Not yet implemented")
                    }

                    override fun getGeoPoint(): Barcode.GeoPoint? {
                        TODO("Not yet implemented")
                    }

                    override fun getPhone(): Barcode.Phone? {
                        TODO("Not yet implemented")
                    }

                    override fun getSms(): Barcode.Sms? {
                        TODO("Not yet implemented")
                    }

                    override fun getUrl(): Barcode.UrlBookmark? {
                        TODO("Not yet implemented")
                    }

                    override fun getWifi(): Barcode.WiFi? {
                        TODO("Not yet implemented")
                    }

                    override fun getDisplayValue(): String? {
                        TODO("Not yet implemented")
                    }

                    override fun getRawValue(): String? {
                        return "jose"
                    }

                    override fun getRawBytes(): ByteArray? {
                        TODO("Not yet implemented")
                    }

                    override fun getCornerPoints(): Array<Point>? {
                        TODO("Not yet implemented")
                    }
                })
            )
        }
        Text("Visible: $visible", modifier = Modifier.clickable {
            visible = !visible
        })
        Text("Orientation: $orientation", modifier = Modifier.clickable {
            val index = (orientation.ordinal + 1) % TruvideoSdkCameraOrientation.entries.size
            orientation = TruvideoSdkCameraOrientation.entries[index]
        })
    }
}