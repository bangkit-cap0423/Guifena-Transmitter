package com.twentythirty.guifenatransmitter.network

import com.twentythirty.guifenatransmitter.data.PayloadModel
import com.twentythirty.guifenatransmitter.data.SensorModel
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Created by taufan-mft on 5/19/2021.
 */
interface GuifenaAPI{
    
    @FormUrlEncoded
    @POST("sensors/upload/")
    suspend fun pushPost(
        @Field("audio") audio: String?,
        @Field("sensor_id") sensor_id: Int
    ): Response<PayloadModel>

    @FormUrlEncoded
    @POST("sensors/add/")
    suspend fun addSensor(
        @Field("nama") nama: String,
        @Field("location") location: String
    ): Response<SensorModel>
}