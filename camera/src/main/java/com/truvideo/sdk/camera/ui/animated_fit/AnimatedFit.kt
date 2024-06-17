package com.truvideo.sdk.camera.ui.animated_fit

import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.components.animated_rotation.TruvideoAnimatedRotation
import com.truvideo.sdk.components.animated_scale.TruvideoAnimatedScale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedFit(
    modifier: Modifier = Modifier,
    aspectRatio: Float,
    rotation: Float = 0f,
    contentModifier: Modifier = Modifier,
    content: @Composable() (() -> Unit)? = null
) {

    var parentLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var parentAspectRatio: Float? = null
    var parentSize: Size? = null
    if (parentLayoutCoordinates != null) {
        parentSize = Size(parentLayoutCoordinates!!.size.width, parentLayoutCoordinates!!.size.height)
        parentAspectRatio = parentLayoutCoordinates!!.size.height.toFloat() / parentLayoutCoordinates!!.size.width
    }

    var contentLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val contentSize: Size?
    var rotatedContentSize: Size? = null
    var rotatedAspectRatio: Float? = null
    if (contentLayoutCoordinates != null) {
        contentSize = Size(contentLayoutCoordinates!!.size.width, contentLayoutCoordinates!!.size.height)
        rotatedContentSize = getRotatedDimensions(contentSize, rotation)
        rotatedAspectRatio = rotatedContentSize.height.toFloat() / rotatedContentSize.width
    }

    var scale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(aspectRatio, rotation, contentLayoutCoordinates) {
        scale = 1f

        val pAR = parentAspectRatio ?: return@LaunchedEffect
        val cAr = rotatedAspectRatio ?: return@LaunchedEffect

        val parentW = parentSize?.width ?: return@LaunchedEffect
        val parentH = parentSize.height
        val contentW = rotatedContentSize?.width ?: return@LaunchedEffect
        val contentH = rotatedContentSize.height

        scale = if (pAR > cAr) {
            parentW.toFloat() / contentW
        } else {
            parentH.toFloat() / contentH
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { parentLayoutCoordinates = it },
        contentAlignment = Alignment.Center
    ) {
        if (content != null)
            TruvideoAnimatedScale(scale = scale) {
                TruvideoAnimatedRotation(rotation = rotation) {
                    Box(
                        modifier = contentModifier
                            .aspectRatio(aspectRatio)
                            .onGloballyPositioned { contentLayoutCoordinates = it }
                    ) {
                        content()
                    }
                }
            }
    }
}

fun getRotatedDimensions(size: Size, rotation: Float): Size {
    val radians = Math.toRadians(rotation.toDouble())
    val rotatedWidth = (abs(size.width * cos(radians)) + abs(size.height * sin(radians))).toFloat()
    val rotatedHeight = (abs(size.width * sin(radians)) + abs(size.height * cos(radians))).toFloat()
    return Size(rotatedWidth.toInt(), rotatedHeight.toInt())
}

@Composable
@Preview
private fun Test() {

    var rotation by remember { mutableFloatStateOf(0f) }
    val contentAspectRatio = 6 / 4f

    Column {
        AnimatedFit(
            aspectRatio = contentAspectRatio,
            modifier = Modifier
                .background(color = Color.Red)
                .clip(RoundedCornerShape(0.dp))
                .weight(1f),
            rotation = rotation,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Black)
            ) {
                Column {
                    Text("Hola", color = Color.White)
                    Box(modifier = Modifier.weight(1f))
                    Text("Chau", color = Color.White)
                }
            }
        }

        Text("Rotation: $rotation", modifier = Modifier.clickable {
            rotation += 90f
        })
        Text("Clear rotation", modifier = Modifier.clickable {
            rotation = 0f
        })
    }

}