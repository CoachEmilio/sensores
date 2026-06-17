package com.uade.sensores.data.local.mappers

import com.uade.sensores.data.local.entities.Measurement
import com.uade.sensores.model.AcelerometroMedicion

/**
 * Mappers Entity ⇄ Dominio.
 *
 * Notar que el campo `pendingSync` NO viaja al dominio:
 * es un detalle de la capa de datos, la UI no tiene que conocerlo.
 *
 * Cuando convertimos de dominio a Entity, dejamos pendingSync = true
 * (el default), porque cualquier medición nueva arranca como pendiente
 * hasta que el Repository confirme el sync con el backend.
 */
fun Measurement.toDomain(): AcelerometroMedicion =
    AcelerometroMedicion(
        x = x,
        y = y,
        z = z,
        timestamp = timestamp
    )

fun AcelerometroMedicion.toEntity(): Measurement =
    Measurement(
        x = x,
        y = y,
        z = z,
        timestamp = timestamp
        // pendingSync = true por default → toda medición nueva nace pendiente
    )