package com.twentythirty.guifenatransmitter.data


import com.google.gson.annotations.SerializedName

data class PayloadModel(
    @SerializedName("audio")
    val audio: String,
    @SerializedName("sensor_id")
    val sensor_id: Int
)