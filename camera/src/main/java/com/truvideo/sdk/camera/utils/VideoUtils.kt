package com.truvideo.sdk.camera.utils

import android.media.MediaMetadataRetriever
import java.io.File

internal fun getVideoDuration(file: File): Long {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(file.path)
    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    retriever.release()
    return durationStr?.toLong() ?: 0L
}