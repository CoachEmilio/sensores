package com.uade.sensores.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos Room de la app.
 *
 * Es una abstract class porque Room (vía KSP) genera la implementación
 * concreta en tiempo de compilación. Nosotros nunca instanciamos esta clase
 * directamente — siempre se accede vía AppDatabase.getInstance(context).
 *
 * @Database declara:
 *  - entities: qué tablas viven en esta base (cada Entity = una tabla).
 *  - version: número de versión del esquema. Subir cuando se cambia la estructura.
 *  - exportSchema: si Room exporta el JSON del schema (útil para migraciones reales).
 */
@Database(
    entities = [Measurement::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Room genera la implementación de este método: devuelve el DAO que
     * podemos usar para leer/escribir en la tabla "measurements".
     *
     * Si en el futuro agregamos más DAOs (UserDao, SettingsDao, etc.),
     * los exponemos acá con más métodos abstractos.
     */
    abstract fun measurementDao(): MeasurementDao

    companion object {
        /**
         * @Volatile garantiza que la escritura de INSTANCE sea inmediatamente
         * visible a todos los threads. Sin esto, dos threads podrían ver
         * valores distintos y crear dos instancias en paralelo.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Patrón Singleton con Double-Checked Locking.
         *
         * Primer ?:    → si INSTANCE ya existe, salimos rápido sin entrar al lock.
         * synchronized → solo un thread por vez entra a construir la base.
         * Segundo ?:   → por si dos threads pasaron el primer check al mismo tiempo,
         *                el segundo ve que el primero ya creó la instancia y no duplica.
         *
         * applicationContext → NUNCA pasamos un Activity Context al Singleton;
         * provocaría un memory leak (la Activity quedaría retenida toda la vida del proceso).
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sensores.db"   // nombre del archivo SQLite en el dispositivo
                )
                    // En estudios: si el esquema cambia, borrar y recrear la base.
                    // En producción: REEMPLAZAR esto por una migración real.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}