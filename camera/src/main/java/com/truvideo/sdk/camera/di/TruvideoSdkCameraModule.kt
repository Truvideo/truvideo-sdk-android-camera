package com.truvideo.sdk.camera.di

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.lifecycle.SavedStateHandle
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraAdapter
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraAuthAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraFFmpegAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraLogAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraRecorderAdapter
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraRecorderAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraVersionPropertiesAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkFileManager
import com.truvideo.sdk.camera.adapters.TruvideoSdkFileManagerImpl
import com.truvideo.sdk.camera.data.repository.TruvideoSdkCameraRepositoryImpl
import com.truvideo.sdk.camera.domain.usecases.ChangeCameraUseCase
import com.truvideo.sdk.camera.domain.usecases.ChangeCameraUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.ConcatVideoListUseCase
import com.truvideo.sdk.camera.domain.usecases.ConcatVideoListUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.GetCameraInformationUseCase
import com.truvideo.sdk.camera.domain.usecases.GetCameraInformationUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.ObserveRecordingEventsUseCase
import com.truvideo.sdk.camera.domain.usecases.ObserveRecordingEventsUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.PauseRecordingUseCase
import com.truvideo.sdk.camera.domain.usecases.PauseRecordingUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.RequestFocusOnPositionUseCase
import com.truvideo.sdk.camera.domain.usecases.RequestFocusOnPositionUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.RestartPreviewUseCase
import com.truvideo.sdk.camera.domain.usecases.RestartPreviewUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.SetFlashUseCase
import com.truvideo.sdk.camera.domain.usecases.SetFlashUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.SetZoomLevelUseCase
import com.truvideo.sdk.camera.domain.usecases.SetZoomLevelUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.StartPreviewUseCase
import com.truvideo.sdk.camera.domain.usecases.StartPreviewUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.StartRecordingUseCase
import com.truvideo.sdk.camera.domain.usecases.StartRecordingUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.StopPreviewUseCase
import com.truvideo.sdk.camera.domain.usecases.StopPreviewUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.StopRecordingUseCase
import com.truvideo.sdk.camera.domain.usecases.StopRecordingUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.TakeImageUseCase
import com.truvideo.sdk.camera.domain.usecases.TakeImageUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.TakeVideoSnapshotUseCase
import com.truvideo.sdk.camera.domain.usecases.TakeVideoSnapshotUseCaseImpl
import com.truvideo.sdk.camera.domain.repository.TruvideoSdkCameraRepository
import com.truvideo.sdk.camera.domain.usecases.GetMediaFileUseCase
import com.truvideo.sdk.camera.domain.usecases.GetMediaFileUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.GetMediaRotationUseCase
import com.truvideo.sdk.camera.domain.usecases.GetMediaRotationUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.ObserveCameraEventsUseCase
import com.truvideo.sdk.camera.domain.usecases.ObserveCameraEventsUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.ResumeRecordingUseCase
import com.truvideo.sdk.camera.domain.usecases.ResumeRecordingUseCaseImpl
import com.truvideo.sdk.camera.domain.usecases.SaveImageUseCase
import com.truvideo.sdk.camera.domain.usecases.SaveImageUseCaseImpl
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraFFmpegAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraLogAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraVersionPropertiesAdapter
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraConfig
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.CameraPreviewViewModel
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.reducers.CameraConfigReducer
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.reducers.CameraControlsReducer
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.reducers.CameraMediaReducer
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.reducers.CameraUIReducer
import com.truvideo.sdk.camera.usecase.ManipulateResolutionsUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val cameraSdkModule = module {

    // Adapters
    single<TruvideoSdkCameraRecorderAdapter> { TruvideoSdkCameraRecorderAdapterImpl(androidContext()) }
    single<TruvideoSdkCameraFFmpegAdapter> { TruvideoSdkCameraFFmpegAdapterImpl() }
    single<TruvideoSdkCameraAdapter> {
        val cameraManager = androidContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        TruvideoSdkCameraAdapterImpl(cameraManager, get())
    }
    single<TruvideoSdkCameraVersionPropertiesAdapter> { TruvideoSdkCameraVersionPropertiesAdapterImpl(androidContext()) }
    single<TruvideoSdkCameraLogAdapter> { TruvideoSdkCameraLogAdapterImpl(get()) }
    single<TruvideoSdkCameraAuthAdapter> { TruvideoSdkCameraAuthAdapterImpl(get(), get()) }

    // Repository
    single<TruvideoSdkCameraRepository> { TruvideoSdkCameraRepositoryImpl(get()) }

    // Use Cases
    factory { ManipulateResolutionsUseCase() }
    factory<GetCameraInformationUseCase> { GetCameraInformationUseCaseImpl(get()) }
    factory<StartPreviewUseCase> { StartPreviewUseCaseImpl(get()) }
    factory<StopPreviewUseCase> { StopPreviewUseCaseImpl(get()) }
    factory<TakeImageUseCase> { TakeImageUseCaseImpl(get()) }
    factory<TakeVideoSnapshotUseCase> { TakeVideoSnapshotUseCaseImpl(get()) }
    factory<RequestFocusOnPositionUseCase> { RequestFocusOnPositionUseCaseImpl(get()) }
    factory<TruvideoSdkFileManager> { TruvideoSdkFileManagerImpl(androidContext()) }
    factory<SetFlashUseCase> { SetFlashUseCaseImpl(get()) }
    factory<SetZoomLevelUseCase> { SetZoomLevelUseCaseImpl(get()) }
    factory<StartRecordingUseCase> { StartRecordingUseCaseImpl(get()) }
    factory<StopRecordingUseCase> { StopRecordingUseCaseImpl(get()) }
    factory<PauseRecordingUseCase> { PauseRecordingUseCaseImpl(get()) }
    factory<ResumeRecordingUseCase> { ResumeRecordingUseCaseImpl(get()) }
    factory<ObserveRecordingEventsUseCase> { ObserveRecordingEventsUseCaseImpl(get()) }
    factory<ObserveCameraEventsUseCase> { ObserveCameraEventsUseCaseImpl(get()) }
    factory<ChangeCameraUseCase>{ ChangeCameraUseCaseImpl(get()) }
    factory<ConcatVideoListUseCase>{ ConcatVideoListUseCaseImpl(get(), get()) }
    factory<RestartPreviewUseCase>{ RestartPreviewUseCaseImpl(get()) }
    factory<CameraControlsReducer>{ CameraControlsReducer(get(), get(), get(), get(), get(), get() , get(), get()) }
    factory<CameraUIReducer>{ CameraUIReducer(get()) }
    factory<GetMediaFileUseCase> { GetMediaFileUseCaseImpl() }
    factory<GetMediaRotationUseCase> { GetMediaRotationUseCaseImpl() }
    factory<SaveImageUseCase> { SaveImageUseCaseImpl() }
    factory<CameraMediaReducer>{ CameraMediaReducer(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory<CameraConfigReducer>{ CameraConfigReducer(get(), get()) }

    // ViewModel
    viewModel {  (state: SavedStateHandle) ->
        CameraPreviewViewModel(
            logAdapter = get(),
            getCameraInformationUseCase = get(),
            savedStateHandle = state,
            observeRecordingEventsUseCase = get(),
            observeCameraEventsUseCase = get(),
            controlsReducer = get(),
            uiReducer = get(),
            mediaReducer = get(),
            configReducer = get()
        )
    }
}