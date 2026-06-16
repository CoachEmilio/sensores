package com.uade.sensores.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity de Room: representa la tabla "measurements" en SQLite.
 *
 * Reglas de arquitectura:
 *  - Esta clase vive SOLO en la capa de datos (data/local).
 *  - La UI y el ViewModel NUNCA deben recibir un Measurement directamente.
 *  - Para salir de esta capa, se traduce a AcelerometroMedicion vía mappers.
 *
 * KSP lee estas anotaciones en tiempo de compilación y genera el SQL
 * (CREATE TABLE, INSERT, SELECT...) automáticamente. Sin KSP configurado
 * en build.gradle.kts, estas anotaciones no hacen nada.
 */
@Entity(
    tableName = "measurements",
    indices = [Index(value = ["timestamp"])]
)
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "axis_x")
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
)
    /**
     * Primary key autogenerada por Room.
     * Se le pasa 0 al insertar y Room le asigna el siguiente número disponible.
     * Tipo Long porque SQLite usa INTEGER (64 bits) — es lo idiomático.
     */
    /**
     * Lecturas crudas del acelerómetro en m/s².
     * @ColumnInfo es opcional: solo se usa cuando el nombre del campo Kotlin
     * y el nombre de la columna SQL deben diferir. Lo dejo en X como ejemplo;
     * en Y y Z dejo que Room use el nombre del campo tal cual.
     */
    /**
     * Marca temporal en milisegundos desde epoch (1/1/1970).
     * System.currentTimeMillis() devuelve Long, por eso este campo es Long.
     */