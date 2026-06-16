package com.uade.sensores.data.repository

import com.uade.sensores.data.local.MeasurementDao
import com.uade.sensores.data.local.toDomain
import com.uade.sensores.data.local.toEntity
import com.uade.sensores.data.remote.MeasurementApi
import com.uade.sensores.data.remote.toDomain
import com.uade.sensores.data.remote.toDto
import com.uade.sensores.model.AcelerometroMedicion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository híbrido (offline-first):
 *   - La UI SIEMPRE observa Room (Flow). Funciona sin internet.
 *   - sincronizar() trae datos del backend y los guarda en Room.
 *   - guardar() inserta en Room Y, si hay red, también en backend.
 *
 * Si la red falla, NO rompe la app: Room sigue siendo la fuente de verdad para la UI.
 */
class MeasurementRepositoryImpl(
    private val dao: MeasurementDao,
    private val api: MeasurementApi
) : MeasurementRepository {

    // Lecturas: Room siempre. La UI no espera red.
    override fun observarMediciones(): Flow<List<AcelerometroMedicion>> =
        dao.observarTodas().map { lista -> lista.map { it.toDomain() } }

    override fun observarBruscas(umbral: Float): Flow<List<AcelerometroMedicion>> =
        dao.observarBruscas(umbral).map { lista -> lista.map { it.toDomain() } }

    override fun contarMediciones(): Flow<Int> = dao.contarTodas()

    /**
     * Guarda en local Y remoto. Si el remoto falla (sin internet, server caído),
     * la medición queda guardada localmente. Una sincronización futura puede
     * reenviarla al backend.
     */
    override suspend fun guardar(medicion: AcelerometroMedicion): Long {
        val idLocal = dao.insertar(medicion.toEntity())
        try {
            api.crear(medicion.toDto())
        } catch (e: Exception) {
            // Log o marcado como "pendiente de sincronizar". No relanzamos.
            // La UI no se rompe porque el dato local ya está.
        }
        return idLocal
    }

    override suspend fun eliminarTodas() = dao.eliminarTodas()

    /**
     * Trae mediciones del backend y las guarda en Room.
     * Llamar manualmente (por ejemplo, desde un botón "Sincronizar").
     * Lanza excepción si la red falla → el ViewModel decide cómo reaccionar.
     */
    suspend fun sincronizar() {
        val remotas = api.obtenerTodas()
        remotas.forEach { dto -> dao.insertar(dto.toDomain().toEntity()) }
    }
}