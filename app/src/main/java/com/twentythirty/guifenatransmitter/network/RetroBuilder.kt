package com.twentythirty.guifenatransmitter.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by taufan-mft on 5/19/2021.
 */
object RetroBuilder {
    private const val BASE_URL = "https://guifena.topanlabs.com"

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val guifenaApi: GuifenaAPI = getRetrofit().create(GuifenaAPI::class.java)

}