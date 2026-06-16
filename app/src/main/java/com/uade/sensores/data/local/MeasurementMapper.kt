package com.uade.sensores.data.local

import com.uade.sensores.model.AcelerometroMedicion

/**
 * Mappers entre la Entity de Room (Measurement) y el modelo de dominio
 * (AcelerometroMedicion).
 *
 * ¿Por qué dos clases casi iguales?
 *  - Measurement es un detalle de cómo persistimos (Room/SQLite).
 *  - AcelerometroMedicion es el concepto del negocio: "una medición del sensor".
 *
 * Si mañana cambiamos Room por DataStore, Firebase, o archivos, solo cambian
 * Measurement y este mapper. AcelerometroMedicion, el ViewModel y la UI
 * NO se enteran.
 *
 * Estas funciones son extension functions: se invocan como
 *   medicion.toEntity() / entity.toDomain()
 * en lugar de
 *   toEntity(medicion) / toDomain(entity)
 * Es más idiomático en Kotlin y se lee mejor.
 */

/**
 * Entity -> Dominio.
 * Se usa al LEER de la base: el DAO devuelve Measurement, lo convertimos
 * a AcelerometroMedicion antes de que salga de la capa de datos.
 *
 * Notar que NO mapeamos el id: el dominio no necesita saber el id de SQLite.
 */
fun Measurement.toDomain(): AcelerometroMedicion {
    return AcelerometroMedicion(
        x = x,
        y = y,
        z = z,
        timestamp = timestamp
    )
}

/**
 * Dominio -> Entity.
 * Se usa al GUARDAR: el ViewModel nos pasa un AcelerometroMedicion,
 * lo convertimos a Measurement para que Room lo entienda.
 *
 * id NO se setea: dejamos el default (0) para que autoGenerate haga su trabajo.
 */
fun AcelerometroMedicion.toEntity(): Measurement {
    return Measurement(
        x = x,
        y = y,
        z = z,
        timestamp = timestamp
    )
}