package com.truvideo.sdk.camera.ui.activities.scanner.components.scanner_code_preview_panel

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerCode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerCodeFormat
import com.truvideo.sdk.camera.ui.components.panel.Panel
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.camera.utils.CustomBarcodeUtils
import com.truvideo.sdk.components.button.TruvideoButton

@Composable
internal fun CustomScannerCodePreviewPanel(
    visible: Boolean = true,
    onConfirm: (() -> Unit) = {},
    close: (() -> Unit) = {},
    orientation: TruvideoSdkCameraOrientation,
    enabled: Boolean = true,
    code: TruvideoSdkCameraScannerCode?
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
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(0.1f))
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = code?.data ?: "",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                    )
                }

                if (code != null) {
                    Box(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .size(56.dp)
                    ) {
                        BarcodeView(code = code)
                    }
                }
            }

            Box(modifier = Modifier.height(48.dp))

            Box(modifier = Modifier.widthIn(max = 300.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TruvideoButton(
                        text = "CONFIRM",
                        selected = true,
                        selectedColor = Color(0xFFFFC107),
                        selectedTextColor = Color.White,
                        enabled = enabled,
                        onPressed = { onConfirm() }
                    )

                    TruvideoButton(
                        text = "CANCEL",
                        enabled = enabled,
                        onPressed = { close() }
                    )
                }
            }

        }
    }
}

@Composable
private fun BarcodeView(code: TruvideoSdkCameraScannerCode) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(code) {
        isLoading = true
        bitmap = CustomBarcodeUtils.generateBarcode(code)
        isLoading = false
    }

    Box(
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(targetState = bitmap, label = "code-bitmap") { bitmapTarget ->
            if (bitmapTarget == null) {
                Box(modifier = Modifier.fillMaxSize())
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val image = remember(bitmapTarget) { bitmapTarget.asImageBitmap() }
                    Image(
                        bitmap = image,
                        contentDescription = "Barcode",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                }
            }

        }
    }
}

@Composable
@Preview(showBackground = true)
private fun Test() {
    var visible by remember { mutableStateOf(true) }
    var orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }

    TruVideoSdkCameraTheme {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CustomScannerCodePreviewPanel(
                    visible = visible,
                    orientation = orientation,
                    code = TruvideoSdkCameraScannerCode(
                        data = "Hola",
                        format = TruvideoSdkCameraScannerCodeFormat.CODE_QR
                    )
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
}