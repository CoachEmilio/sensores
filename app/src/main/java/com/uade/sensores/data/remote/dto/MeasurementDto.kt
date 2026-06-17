package com.uade.sensores.data.remote.dto

import com.uade.sensores.model.AcelerometroMedicion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi


@OptIn(InternalSerializationApi::class)
@Serializable
data class MeasurementDto(
    val id: Long? = null,
    @SerialName("axis_x")
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
)
