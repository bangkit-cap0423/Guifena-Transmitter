package com.twentythirty.guifenatransmitter.data

import com.twentythirty.guifenatransmitter.network.GuifenaAPI
import retrofit2.Response


class MainRepository(private val guifenaAPI: GuifenaAPI) {
    suspend fun pushPost(audio: String?, sensorId: Int): Response<PayloadModel> =
        guifenaAPI.pushPost(audio, sensorId)

    suspend fun addSensor(name: String, location: String): Response<SensorModel> =
        guifenaAPI.addSensor(name, location)
}