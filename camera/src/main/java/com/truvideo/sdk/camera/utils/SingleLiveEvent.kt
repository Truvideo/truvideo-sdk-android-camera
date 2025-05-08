package com.truvideo.sdk.camera.utils

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

internal class SingleLiveEvent<T> : MutableLiveData<T>() {

    private val pending = AtomicBoolean(false)
    override fun observeForever(observer: Observer<in T>) {
        super.observeForever { t ->
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        }
    }
    override fun setValue(value: T?) {
        pending.set(true)
        super.setValue(value)
    }
}