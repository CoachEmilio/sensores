package com.uade.sensores.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity de Room: representa la tabla "measurements" en SQLite.
 *
 * Reglas de arquitectura:
 *  - Esta clase vive SOLO en la capa de datos (data/local).
 *  - La UI y el ViewModel NUNCA deben recibir un Measurement directamente.
 *  - Para salir de esta capa, se traduce a AcelerometroMedicion vía mappers.
 *
 * indices:
 *  - timestamp: acelera queries que ordenan por fecha (todas las nuestras).
 *  - pending_sync: acelera la query que busca mediciones sin sincronizar.
 */
@Entity(
    tableName = "measurements",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["pending_sync"])
    ]
)
data class Measurement(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "axis_x")
    val x: Float,

    val y: Float,

    val z: Float,

    val timestamp: Long,

    /**
     * Flag de sincronización con el backend.
     *
     *  - true  → la medición está en local pero NO se subió al servidor todavía.
     *  - false → la medición ya está sincronizada con el backend.
     *
     * Por defecto se inserta como pendiente (true). El Repository lo cambia
     * a false cuando confirma el POST al backend.
     *
     * Este campo NO existe en el dominio (AcelerometroMedicion) porque es
     * un detalle interno de la persistencia: a la UI no le importa.
     */
    @ColumnInfo(name = "pending_sync")
    val pendingSync: Boolean = true
)