package com.uade.sensores.data.repository

import com.uade.sensores.model.AcelerometroMedicion
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del Repository.
 * El ViewModel depende de ESTA interface, no de la implementación concreta.
 * Trabaja con AcelerometroMedicion (dominio), nunca con Measurement (Entity).
 */
interface MeasurementRepository {
    fun observarMediciones(): Flow<List<AcelerometroMedicion>>
    fun observarBruscas(umbral: Float = 15f): Flow<List<AcelerometroMedicion>>
    fun contarMediciones(): Flow<Int>
    suspend fun guardar(medicion: AcelerometroMedicion): Long
    suspend fun eliminarTodas()
    suspend fun sincronizar()
}