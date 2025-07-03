package com.truvideo.sdk.camera.domain.usecases

import com.truvideo.sdk.camera.model.SaveImageInput
import com.truvideo.sdk.camera.utils.save
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface SaveImageUseCase {
    suspend operator fun invoke(input: SaveImageInput): String
}

class SaveImageUseCaseImpl : SaveImageUseCase {
    override suspend operator fun invoke(input: SaveImageInput): String {
        val name = input.name ?:
            SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US).format(Date())

        return input.image.save(
            path = input.outputPath,
            name = name,
            extension = input.format.code,
            rotation = input.rotation
        ).also {
            input.image.close()
        }
    }
}