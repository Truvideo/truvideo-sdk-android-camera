package com.truvideo.sdk.camera.ui.activities.scanner.components.animated_scanner_overlay

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme

@Composable
internal fun CustomAnimatedScannerOverlay(
    modifier: Modifier = Modifier,
    cornerLength: Dp = 50.dp,
    cornerWidth: Dp = 4.dp,
    cornerRadius: Dp = 16.dp,
    rectangleColor: Color = Color.White,
    scannerSize: Dp = 250.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Canvas(modifier = modifier.size(scannerSize)) {
        val cornerLengthPx = cornerLength.toPx()
        val cornerWidthPx = cornerWidth.toPx()
        val cornerRadiusPx = cornerRadius.toPx()

        val rectWidth = size.width * animatedProgress
        val rectHeight = size.height * animatedProgress

        val left = (size.width - rectWidth) / 2
        val top = (size.height - rectHeight) / 2

        // Draw the stroked rectangle
        drawRoundRect(
            color = rectangleColor,
            topLeft = Offset(left, top),
            size = Size(rectWidth, rectHeight),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = cornerWidthPx)
        )

        // Draw the corners
        val path = Path().apply {
            // Top-left corner
            moveTo(left, top + cornerLengthPx)
            lineTo(left, top + cornerRadiusPx)
            arcTo(
                Rect(left, top, left + cornerRadiusPx * 2, top + cornerRadiusPx * 2),
                180f,
                90f,
                false
            )
            lineTo(left + cornerLengthPx, top)

            // Top-right corner
            moveTo(left + rectWidth - cornerLengthPx, top)
            lineTo(left + rectWidth - cornerRadiusPx, top)
            arcTo(
                Rect(left + rectWidth - cornerRadiusPx * 2, top, left + rectWidth, top + cornerRadiusPx * 2),
                270f,
                90f,
                false
            )
            lineTo(left + rectWidth, top + cornerLengthPx)

            // Bottom-right corner
            moveTo(left + rectWidth, top + rectHeight - cornerLengthPx)
            lineTo(left + rectWidth, top + rectHeight - cornerRadiusPx)
            arcTo(
                Rect(left + rectWidth - cornerRadiusPx * 2, top + rectHeight - cornerRadiusPx * 2, left + rectWidth, top + rectHeight),
                0f,
                90f,
                false
            )
            lineTo(left + rectWidth - cornerLengthPx, top + rectHeight)

            // Bottom-left corner
            moveTo(left + cornerLengthPx, top + rectHeight)
            lineTo(left + cornerRadiusPx, top + rectHeight)
            arcTo(
                Rect(left, top + rectHeight - cornerRadiusPx * 2, left + cornerRadiusPx * 2, top + rectHeight),
                90f,
                90f,
                false
            )
            lineTo(left, top + rectHeight - cornerLengthPx)
        }

        drawPath(
            path = path,
            color = rectangleColor,
            style = Stroke(width = cornerWidthPx)
        )
    }
}

@Composable
@Preview
private fun Test() {
    TruVideoSdkCameraTheme {
        Column {
            Box(
                modifier = Modifier
                    .width(400.dp)
                    .height(800.dp)
            ) {
                CustomAnimatedScannerOverlay()
            }
        }
    }
}