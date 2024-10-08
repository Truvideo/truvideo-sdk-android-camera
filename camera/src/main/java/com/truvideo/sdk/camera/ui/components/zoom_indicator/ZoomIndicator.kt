package com.truvideo.sdk.camera.ui.components.zoom_indicator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.utils.dpToPx
import com.truvideo.sdk.camera.utils.pxToDp
import com.truvideo.sdk.components.animated_fade_visibility.TruvideoAnimatedFadeVisibility
import com.truvideo.sdk.components.animated_rotation.TruvideoAnimatedRotation
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

enum class ZoomIndicatorMode {
    Indicator, Full;

    val isIndicator: Boolean
        get() {
            return this == Indicator
        }

    val isFull: Boolean
        get() {
            return this == Full
        }
}


@Composable
fun ZoomIndicator(
    zoom: Float,
    mode: ZoomIndicatorMode = ZoomIndicatorMode.Indicator,
    onModeChange: ((mode: ZoomIndicatorMode) -> Unit)? = null,
    onZoomChange: ((zoom: Float) -> Unit)? = null,
    orientation: TruvideoSdkCameraOrientation = TruvideoSdkCameraOrientation.PORTRAIT
) {
    val context = LocalContext.current
    var size by remember { mutableStateOf(Size(0f, 0f)) }
    var offset by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var initialZoom by remember { mutableFloatStateOf(zoom) }

    val totalPoints = 10
    val angleOffset = (360f * 0.5) / (totalPoints - 1)
    val circleAngle = remember(zoom) { (zoom - 1f) / 9f * 180f }

    LaunchedEffect(dragging, initialZoom, offset) {
        if (dragging) {
            val p = offset / size.width
            val delta = 10 * p
            val newZoom = initialZoom - delta
            if (onZoomChange != null) {
                onZoomChange(newZoom.coerceIn(1f, 10f))
            }
        }
    }


    Box(modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(0.dp))
        .onGloballyPositioned {
            size = Size(it.size.width.toFloat(), it.size.height.toFloat())
        }) {
        // Indicator
        TruvideoAnimatedFadeVisibility(
            visible = mode.isIndicator,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    TruvideoAnimatedRotation(orientation.uiRotation) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(40.dp)
                                .background(color = Color.Black.copy(0.7f))
                                .clickable(
                                    enabled = mode == ZoomIndicatorMode.Indicator,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = Color.White)
                                ) {
                                    initialZoom = zoom
                                    if (onModeChange != null) {
                                        onModeChange(ZoomIndicatorMode.Full)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                String.format("%.1fX", zoom),
                                color = Color(0xFFFFC107),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }


        // Full
        AnimatedVisibility(
            visible = mode == ZoomIndicatorMode.Full,
            enter = fadeIn() + scaleIn(
                transformOrigin = TransformOrigin(0.5f, 1.0f),
                initialScale = 0.7f,
            ),
            exit = fadeOut() + scaleOut(
                transformOrigin = TransformOrigin(0.5f, 1.0f),
                targetScale = 0.7f,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (context.pxToDp(size.width * 0.8f)).dp)
                        .scale(1.15f)
                ) {

                    val itemSize = context.dpToPx(35f)
                    val padding = context.dpToPx(32f)


                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    dragging = true
                                },
                                onDragEnd = {
                                    dragging = false
                                    val p = offset / size.width
                                    val delta = 10 * p
                                    val newZoom = initialZoom - delta
                                    initialZoom = newZoom.coerceIn(1f, 10f)
                                    offset = 0f
                                },
                                onDragCancel = {
                                    dragging = false
                                    val p = offset / size.width
                                    val delta = 10 * p
                                    val newZoom = initialZoom - delta
                                    initialZoom = newZoom.coerceIn(1f, 10f)
                                    offset = 0f
                                },
                            ) { change, dragAmount ->
                                offset += dragAmount
                                change.consume()
                            }
                        }
                        .clip(CircleShape)
                        .rotate(-90f - circleAngle)
                        .background(color = Color.Black.copy(0.7f))
                        .drawBehind {
                            val radius = size.width * 0.5f

                            // Middle lines
                            for (degree in 0..180 step 4) {
                                val radians = Math.toRadians(degree.toDouble())
                                val startX = (center.x + (radius - padding * 0.2) * cos(radians)).toFloat()
                                val endX = (center.x + (radius - padding * 0.8) * cos(radians)).toFloat()
                                val startY = (center.y + (radius - padding * 0.2) * sin(radians)).toFloat()
                                val endY = (center.y + (radius - padding * 0.8) * sin(radians)).toFloat()

                                drawLine(
                                    color = Color.White.copy(alpha = 0.4f),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }

                            // Whole numbers lines
                            for (degree in 0..180 step 20) {
                                val radians = Math.toRadians(degree.toDouble())
                                val startX = (center.x + (radius - padding * 0.2) * cos(radians)).toFloat()
                                val endX = (center.x + (radius - padding * 0.9) * cos(radians)).toFloat()
                                val startY = (center.y + (radius - padding * 0.2) * sin(radians)).toFloat()
                                val endY = (center.y + (radius - padding * 0.9) * sin(radians)).toFloat()

                                drawLine(
                                    color = Color.White,
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }

                        }
                    ) {
                        for (i in 0 until totalPoints) {
                            val angle = Math.toRadians((i * angleOffset))
                            val angleDistance = circleAngle - (i * angleOffset)
                            val alpha = interpolateLinear(
                                angleDistance.absoluteValue,
                                angleOffset / 2,
                                angleOffset,
                                0.0,
                                1.0
                            ).coerceIn(0.0, 1.0)
                            val distance = (size.width * 0.5f) - (itemSize * 0.5f) - padding
                            val x = (cos(angle) * distance + size.width * 0.5).toFloat()
                            val y = (sin(angle) * distance + size.width * 0.5).toFloat()
                            val textRotation = angle + Math.PI / 2

                            Box(
                                modifier = Modifier
                                    .size(context.pxToDp(itemSize).dp)
                                    .offset(
                                        x = context.pxToDp(x - itemSize * 0.5f).dp,
                                        y = context.pxToDp(y - itemSize * 0.5f).dp
                                    )
                                    .graphicsLayer(
                                        alpha = alpha.toFloat()
                                    )
                                    .rotate(
                                        Math
                                            .toDegrees(textRotation)
                                            .toFloat()
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                TruvideoAnimatedRotation(orientation.uiRotation) {
                                    Text(
                                        text = "${i + 1}x",
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }

                            }
                        }
                    }

                    // Current zoom arrow
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .width(10.dp)
                            .height(10.dp)
                            .drawBehind {
                                val w = 8.dp.toPx()
                                val h = 8.dp.toPx()
                                val trianglePath = Path().apply {
                                    moveTo(center.x - w * 0.5f, 0f) // Punto superior
                                    lineTo(center.x + w * 0.5f, 0f) // Punto inferior izquierdo
                                    lineTo(center.x, center.y + h) // Punto inferior derecho
                                    close()
                                }

                                drawPath(trianglePath, color = Color(0xFFFFC107))
                            }
                    )
                    // Current zoom
                    Box(
                        modifier = Modifier
                            .padding(context.pxToDp(padding).dp)
                            .align(Alignment.TopCenter)
                    ) {
                        TruvideoAnimatedRotation(orientation.uiRotation) {
                            Box(
                                modifier = Modifier
                                    .size(context.pxToDp(itemSize).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    String.format("%.1fX", zoom),
                                    color = Color(0xFFFFC107),
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

        }

//        val p = offset / size.width
//        val delta = 10 * p
//
//        Column {
//            Text("Dragging: $dragging")
//            Text("Delta: $delta")
//            Text("new zoom: ${(initialZoom - delta).coerceIn(1f, 10f)}")
//            Text("initialZoom: $initialZoom")
//            Text("zoom: $zoom")
//            Text("Size: ${size.width}")
//            Text("Count: ${count}")
//
//        }
    }
}

fun interpolateLinear(x: Double, x0: Double, x1: Double, y0: Double, y1: Double): Double {
    return y0 + (y1 - y0) * ((x - x0) / (x1 - x0))
}

@Composable
@Preview
private fun Test() {
    var zoom by remember { mutableFloatStateOf(8f) }
    var mode by remember { mutableStateOf(ZoomIndicatorMode.Indicator) }
    var orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }
    var reverse by remember { mutableStateOf(false) }

    Column {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            TruvideoAnimatedRotation(rotation = if (reverse) 180f else 0f) {
                ZoomIndicator(
                    zoom = zoom,
                    mode = mode,
                    onModeChange = { mode = it },
                    onZoomChange = { zoom = it },
                    orientation = orientation
                )
            }

        }

        Text("Orientation: $orientation", modifier = Modifier.clickable {
            val index = (orientation.ordinal + 1) % TruvideoSdkCameraOrientation.entries.size
            orientation = TruvideoSdkCameraOrientation.entries[index]
        })
        Text("Reverse: $reverse", modifier = Modifier.clickable {
            reverse = !reverse
        })
    }

}