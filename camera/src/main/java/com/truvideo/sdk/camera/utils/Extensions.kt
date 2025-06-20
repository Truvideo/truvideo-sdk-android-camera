package com.truvideo.sdk.camera.utils

import android.content.Context

internal fun Context.dpToPx(dp: Float): Float {
    val density = resources.displayMetrics.density
    return dp * density
}

internal fun Context.pxToDp(px: Float): Float {
    val density = resources.displayMetrics.density
    return px / density
}