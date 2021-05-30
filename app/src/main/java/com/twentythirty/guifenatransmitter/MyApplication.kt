package com.twentythirty.guifenatransmitter

import android.app.Application
import com.twentythirty.guifenatransmitter.di.Koin.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApp : Application() {
    override fun onCreate() {

        // Start Koin
        startKoin {
            modules(appModule)
        }
        super.onCreate()
    }
}