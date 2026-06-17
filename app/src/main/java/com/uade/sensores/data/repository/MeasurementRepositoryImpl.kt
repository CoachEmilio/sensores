package com.uade.sensores.data.repository

import android.util.Log
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
 * Repository híbrido (offline-first) con manejo de pendientes:
 *   - La UI SIEMPRE observa Room. Funciona sin internet.
 *   - guardar() inserta en Room como "pendiente"; intenta subir al backend;
 *     si lo logra, marca la medición como sincronizada.
 *   - sincronizar() hace 2 cosas:
 *       1) Sube los pendientes locales que quedaron del modo offline.
 *       2) Baja del backend lo que el servidor tenga y lo persiste como ya sincronizado.
 */
class MeasurementRepositoryImpl(
    private val dao: MeasurementDao,
    private val api: MeasurementApi
) : MeasurementRepository {

    override fun observarMediciones(): Flow<List<AcelerometroMedicion>> =
        dao.observarTodas().map { lista -> lista.map { it.toDomain() } }

    override fun observarBruscas(umbral: Float): Flow<List<AcelerometroMedicion>> =
        dao.observarBruscas(umbral).map { lista -> lista.map { it.toDomain() } }

    override fun contarMediciones(): Flow<Int> = dao.contarTodas()

    /**
     * Flujo:
     *   1. Insertar en local con pendingSync = true (default).
     *   2. Intentar POST al backend.
     *   3a. Si OK → marcar como sincronizada (pendingSync = false).
     *   3b. Si falla → queda pendiente para el próximo sincronizar().
     */
    override suspend fun guardar(medicion: AcelerometroMedicion): Long {
        val idLocal = dao.guardarOActualizar(medicion.toEntity())
        try {
            api.crear(medicion.toDto())
            dao.marcarSincronizada(idLocal)
        } catch (e: Exception) {
            Log.w("Repository", "Pendiente de sync: id=$idLocal — ${e.message}")
        }
        return idLocal
    }

    override suspend fun eliminarTodas() = dao.eliminarTodas()

    /**
     * Sincronización bidireccional:
     *   - PUSH: sube todas las mediciones locales pendientes.
     *   - PULL: baja las que el backend tenga y las persiste como ya sincronizadas.
     *
     * Si una llamada individual falla, NO se cancela el resto: la siguiente
     * sincronización reintentará lo que quedó pendiente.
     */
    override suspend fun sincronizar() {

        // 1) PUSH: subir pendientes locales
        val pendientes = dao.obtenerPendientes()
        Log.d("Repository", "Sincronizando ${pendientes.size} pendientes")

        pendientes.forEach { entity ->
            try {
                api.crear(entity.toDomain().toDto())
                dao.marcarSincronizada(entity.id)
            } catch (e: Exception) {
                Log.w("Repository", "No pude subir id=${entity.id}: ${e.message}")
                // El forEach continúa con el siguiente, no cancela todo.
            }
        }

        // 2) PULL: bajar nuevas del backend
        try {
            val remotas = api.obtenerTodas()
            remotas.forEach { dto ->
                // Las que vienen del backend ya están "sincronizadas" por definición.
                val entity = dto.toDomain().toEntity().copy(pendingSync = false)
                dao.guardarOActualizar(entity)
            }
        } catch (e: Exception) {
            Log.w("Repository", "Pull falló: ${e.message}")
        }
    }
}