package com.uade.sensores.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Upsert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para la tabla "measurements".
 *
 * Es una INTERFACE: nosotros solo declaramos las operaciones; Room (vía KSP)
 * genera la implementación con el SQL real en tiempo de compilación.
 *
 * Reglas:
 *  - Escrituras (insert/delete/update) → suspend fun para no bloquear el hilo principal.
 *  - Lecturas observables → Flow<List<T>> para que la UI se actualice sola.
 *  - Parámetros en @Query → siempre con :nombre, nunca concatenando strings (SQL Injection).
 */
@Dao
interface MeasurementDao {

    /**
     * Inserta una medición.
     * suspend → Room la ejecuta en el hilo de IO, no bloquea la UI.
     * Devuelve el id autogenerado (Long) por si el llamador lo necesita.
     */
    @Upsert
    suspend fun guardarOActualizar(measurement: Measurement): Long

    /**
     * Borra una medición específica.
     * Room sabe cuál borrar usando el id de la entidad pasada.
     */
    @Delete
    suspend fun eliminar(measurement: Measurement)

    /**
     * Borra TODAS las mediciones (útil para "limpiar historial").
     * No usa @Delete porque @Delete necesita la entidad concreta;
     * para "borrar todo" usamos @Query con DELETE FROM.
     */
    @Query("DELETE FROM measurements")
    suspend fun eliminarTodas()

    /**
     * Lee TODAS las mediciones, ordenadas por timestamp descendente
     * (la más nueva primero).
     *
     * Devuelve Flow → cada vez que la tabla cambia (insert/delete),
     * Flow emite la lista actualizada y la UI se entera sola.
     *
     * NO lleva suspend porque Flow ya es asincrónico por diseño.
     */
    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    fun observarTodas(): Flow<List<Measurement>>

    /**
     * Cuenta cuántas mediciones hay registradas.
     * También como Flow → el contador en la UI se actualiza solo.
     *2. Flow<List<T>> para lecturas observables
     * Cuando leés datos, en lugar de devolver List<Measurement>, devolvés Flow<List<Measurement>>.
     * ¿Qué es un Flow? Un caño que emite valores nuevos cuando hay cambios. Si insertás una medición,
     * todos los Flow que estaban observando esa tabla emiten la lista actualizada automáticamente.
     * Tu UI se actualiza sola, sin que vos le digas "refrescá".
     * Comparalo con la alternativa fea:
     *
     * Sin Flow → cada vez que insertás, tenés que llamar a dao.obtenerTodos() manualmente y refrescar la UI a mano.
     * Con Flow → insertás una vez, todos los observadores reciben la nueva lista automáticamente.
     *
     * Por eso Flow es reactivo, encaja perfecto con Compose (que ya es reactivo) y con UDF (state down).
     * Detalle fino: Flow no necesita suspend en la firma.
     * Eso es porque Flow ya es asincrónico por diseño — suspend se usa para "ejecutar y devolver una vez";
     * Flow para "emitir muchas veces". Son dos modelos distintos.
     */

    @Query("SELECT COUNT(*) FROM measurements")
    fun contarTodas(): Flow<Int>

    /**
     * Lee solo las mediciones "bruscas" (fuerzaG > umbral).
     *
     * OJO: fuerzaG NO es una columna en la tabla (vive en el dominio).
     * Por eso calculamos sqrt(x*x + y*y + z*z) directamente en SQL.
     *
     * Parámetro con :umbral → seguro contra SQL Injection.
     */
    @Query("""
        SELECT * FROM measurements
        WHERE (axis_x * axis_x + y * y + z * z) > (:umbral * :umbral)
        ORDER BY timestamp DESC
    """)
    fun observarBruscas(umbral: Float = 15f): Flow<List<Measurement>>

    /**
     * Busca una medición específica por id.
     * Suspend porque es una lectura puntual (no observable).
     * Devuelve nullable: si no existe, devuelve null.
     */
    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Measurement?

    // suspend fun para operaciones de escritura
    // Cuando insertás, actualizás o borrás, la función se declara con suspend.
    // ¿Por qué?
    // Porque las operaciones de base de datos son lentas (no microsegundos,
    // milisegundos o más). Si las ejecutás en el hilo principal, bloqueás la UI.
    // El usuario ve la pantalla congelada y aparece el dialog "La aplicación no responde"
    // (ANR — Application Not Responding).
    // suspend le dice a Kotlin: "esta función se puede pausar y reanudar; ejecutala en una corrutina,
    // no en el hilo principal".
    // Room en runtime detecta suspend y automáticamente cambia al hilo de IO.
    // No tenés que pensar en threading manual.
}