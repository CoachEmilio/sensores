package com.uade.sensores.data.remote

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
// Mappers DTO ⇄ Domain
fun MeasurementDto.toDomain(): AcelerometroMedicion =
    AcelerometroMedicion(x = x, y = y, z = z, timestamp = timestamp)

fun AcelerometroMedicion.toDto(): MeasurementDto =
    MeasurementDto(x = x, y = y, z = z, timestamp = timestamp)

/*
    DTO (Data Transfer Object): how travel the data in the network
    No sale de la capa de data. Se Traduce a AcelerometroMedicion con mappers.
*/