package com.truvideo.sdk.camera.domain.usecases

interface GetMediaRotationUseCase {
    operator fun invoke(
        sensorRotation: Int,
        isFront: Boolean,
        orientation: Int
    ) : Int
}


class GetMediaRotationUseCaseImpl: GetMediaRotationUseCase {

    override operator fun invoke(
        sensorRotation: Int,
        isFront: Boolean,
        orientation: Int
    ) : Int {
        val result = if (isFront) {
            sensorRotation + orientation
        } else {
            sensorRotation - orientation + 360
        }
        return (result % 360) % 360
    }
}