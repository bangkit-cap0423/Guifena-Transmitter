package com.twentythirty.guifenatransmitter.di

import com.twentythirty.guifenatransmitter.data.MainRepository
import com.twentythirty.guifenatransmitter.network.RetroBuilder
import com.twentythirty.guifenatransmitter.services.RecordService
import org.koin.dsl.module

object Koin {
    val appModule = module {
        single { RetroBuilder.guifenaApi }
        single { MainRepository(get()) }
    }
}