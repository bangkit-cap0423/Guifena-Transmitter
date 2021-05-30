package com.twentythirty.guifenatransmitter.data

import com.google.gson.annotations.SerializedName

data class SensorModel(
    @SerializedName("sensor_id")
    val sensor_id: Int,
    @SerializedName("nama")
    val nama: String,
    @SerializedName("location")
    val location: String
)
