# Sensores UADE — App Android con Acelerómetro, Room y Sync a Supabase

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-757575?style=for-the-badge&logo=materialdesign&logoColor=white)
![Room](https://img.shields.io/badge/Room-2.6.1-4DB33D?style=for-the-badge&logo=sqlite&logoColor=white)
![Retrofit](https://img.shields.io/badge/Retrofit-2.11.0-48B983?style=for-the-badge)
![Supabase](https://img.shields.io/badge/Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)
![KSP](https://img.shields.io/badge/KSP-1.9.22--1.0.17-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Coroutines](https://img.shields.io/badge/Coroutines-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![StateFlow](https://img.shields.io/badge/StateFlow-FF6F00?style=for-the-badge&logo=kotlin&logoColor=white)

---

## 1. Objetivo de la Aplicación

Esta app Android lee el **acelerómetro** del dispositivo en tiempo real, calcula la **fuerza G** del movimiento, y persiste las mediciones bruscas (umbral 15G) tanto en una base local con **Room** como en un backend remoto con **Supabase**.

La idea es practicar el patrón **offline-first**: la UI siempre lee de Room (funciona sin internet), y cuando hay conectividad, el Repository sincroniza con Supabase en segundo plano.

### Funcionalidades

1. Lectura continua del acelerómetro (X, Y, Z) usando `SensorManager` y `SensorEventListener`.
2. Cálculo de la fuerza G en el dominio: `fuerzaG = √(x² + y² + z²)`.
3. Persistencia automática de mediciones bruscas (G > 15) en Room.
4. Historial visible en pantalla con `LazyColumn` reactivo (`Flow<List<…>>`).
5. Botón **"Sync with Supabase"** que dispara una sincronización bidireccional (PUSH local pendientes + PULL del backend).
6. Logs de ciclo de vida (`CYCLE`) para estudiar el comportamiento de la Activity.

El sensor es **lifecycle-aware**: la clase `AcelerometroReader` implementa `DefaultLifecycleObserver` y se registra/desregistra solo al ciclo de vida de la Activity (Inversión de Control).

---

## 2. Stack Técnico

- **Lenguaje:** Kotlin 1.9.22
- **UI:** Jetpack Compose + Material 3 (Compose puro, sin XML).
- **Arquitectura:** MVVM con `state down, events up` (UDF).
- **Persistencia local:** Room 2.6.1 sobre SQLite, con KSP para generación de código.
- **Cliente HTTP:** Retrofit 2.11 + OkHttp + interceptor de auth para Supabase.
- **Serialización:** kotlinx.serialization (JSON).
- **Backend remoto:** Supabase (PostgreSQL con API REST autogenerada).
- **Asincronía:** Corrutinas + StateFlow + Flow.
- **Seguridad de credenciales:** `local.properties` + `BuildConfig` (no hardcoded, no en Git).

---

## 3. Estructura del Proyecto

La estructura está organizada por **tipo de archivo dentro de cada capa** (siguiendo SOLID), de modo que escale a varias tablas sin perder claridad:

com.uade.sensores/

├── data/

│   ├── local/

│   │   ├── database/    AppDatabase.kt

│   │   ├── entities/    Measurement.kt

│   │   ├── daos/        MeasurementDao.kt

│   │   └── mappers/     MeasurementMapper.kt

│   ├── remote/

│   │   ├── api/         MeasurementApi.kt

│   │   ├── dto/         MeasurementDto.kt

│   │   ├── client/      RetrofitClient.kt

│   │   └── mappers/     MeasurementRemoteMapper.kt

│   └── repository/

│       ├── MeasurementRepository.kt        (interface)

│       └── MeasurementRepositoryImpl.kt    (impl híbrida offline-first)

├── model/

│   └── AcelerometroMedicion.kt             (modelo de dominio)

├── sensor/

│   └── AcelerometroReader.kt               (lifecycle-aware)

└── ui/

├── screen/   MainActivity.kt + ScreenSensor (Composable)

├── theme/    SensoresTheme

└── viewmodel/ MainViewModel.kt


### Responsabilidades por capa

| Capa | Responsabilidad |
|---|---|
| `data/local/` | Persistencia con Room: Entity, DAO, mappers, base. |
| `data/remote/` | Comunicación HTTP con Supabase: DTO, API, cliente, mappers. |
| `data/repository/` | Único punto de acceso a datos para el resto de la app. Combina local + remoto. |
| `model/` | Modelo de dominio puro. No conoce Room ni Retrofit. |
| `sensor/` | Acceso al acelerómetro físico. |
| `ui/` | Pantallas, ViewModels, tema visual. |

### Por qué esta estructura

Una alternativa habría sido juntar todo lo de `local/` en una sola carpeta. Funciona bien con 1 tabla, pero con 10+ tablas se vuelve inmanejable. Separar por tipo permite navegar rápido: si busco un DAO, voy a `daos/`. Si busco un Entity, voy a `entities/`. Cada archivo tiene un único motivo para cambiar (SRP).

---

## 4. Arquitectura: patrón offline-first

┌─────────────────────────┐
│    ScreenSensor (UI)    │
└────────────┬────────────┘

             │ observa StateFlow

┌────────────▼────────────┐

│     MainViewModel       │

└────────────┬────────────┘

             │ depende de la interface

┌────────────▼────────────────────┐

│  MeasurementRepository (iface)  │

└────────────┬────────────────────┘

             │ implementa

┌────────────▼─────────────────────────────┐

│ MeasurementRepositoryImpl (híbrido)      │

│                                          │

│  ┌──────────────┐    ┌──────────────┐   │

│  │ Room (local) │    │ Retrofit→API │   │

│  │ FUENTE DE    │    │ Supabase     │   │

│  │ VERDAD       │    │              │   │

│  └──────────────┘    └──────────────┘   │

└──────────────────────────────────────────┘

### Flujo de escritura (`guardar`)

1. La medición se inserta en Room con `pendingSync = true` (default).
2. Se intenta hacer POST al backend.
3. Si el POST es exitoso → `marcarSincronizada(id)` setea `pendingSync = false`.
4. Si falla la red → queda pendiente para la próxima sincronización. **La app no rompe.**

### Flujo de sincronización (`sincronizar`)

1. **PUSH:** se buscan las mediciones con `pending_sync = 1` y se reintentan subir.
2. **PULL:** se descarga el catálogo del backend y se inserta en Room con `@Upsert` (evita conflictos por id duplicado).

### Decisiones arquitectónicas claves

- **El Repository expone modelos de dominio**, nunca Entities ni DTOs. Los mappers traducen en la frontera de la capa de datos.
- **`Flow<List<…>>` para lecturas observables** → la UI se actualiza sola al cambiar la base.
- **`suspend` para escrituras puntuales** → Room ejecuta en hilo de IO, evita ANR.
- **`@Upsert` en el DAO** → insert-or-update atómico, ideal para sincronización idempotente.
- **`@Volatile` + Double-Checked Locking** en el Singleton de `AppDatabase`.
- **`SharingStarted.WhileSubscribed(5000)`** en los StateFlow del ViewModel → para de emitir cuando nadie observa, ahorra batería.

---

## 5. Dependencias

### `app/build.gradle.kts` — plugins

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}
```

### Dependencias principales

```kotlin
// Compose + Material 3 (BOM)
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.material3)
implementation(libs.androidx.activity.compose)

// Room (KSP para generación de código)
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)

// Retrofit + JSON serialization
implementation(libs.retrofit)
implementation(libs.retrofit.kotlinx.serialization)
implementation(libs.kotlinx.serialization.json)
implementation(libs.okhttp.logging)
```

### Por qué Room necesita KSP

Las anotaciones `@Entity`, `@Dao`, `@Database` son metadata vacía sin un procesador. **KSP las lee en tiempo de compilación** y genera las clases `…_Impl` con el SQL real. Si `ksp(...)` no está configurado, las anotaciones existen pero nadie las procesa → la app crashea en runtime al llamar `Room.databaseBuilder`.

La versión de KSP **debe coincidir con la versión de Kotlin** (`<kotlin>-<ksp>`): en este proyecto, `1.9.22 → 1.9.22-1.0.17`.

---

## 6. Configuración de Supabase

### Secrets en `local.properties`

```properties
SUPABASE_URL=https://<tu-proyecto>.supabase.co
SUPABASE_KEY=sb_publishable_xxxxxxxxxxxxxxxxxx
```

Este archivo **está en `.gitignore`**, nunca viaja a Git.

### Inyección en `BuildConfig` desde `build.gradle.kts`

```kotlin
defaultConfig {
    buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
    buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
}
```

Esto expone las credenciales como constantes en `BuildConfig.SUPABASE_URL` / `BuildConfig.SUPABASE_KEY`, consumibles desde `RetrofitClient`. **Las keys no aparecen en el código fuente versionado.**

### Tabla `measurements` en Supabase

```sql
CREATE TABLE measurements (
    id BIGSERIAL PRIMARY KEY,
    axis_x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_measurements_timestamp ON measurements(timestamp DESC);
```

Políticas RLS abiertas (inserción y lectura pública para roles `anon` y `authenticated`) — apropiadas para una app de estudio. En producción se restringen al usuario autenticado.

### Por qué `anon`/`publishable` key puede vivir en el cliente

La seguridad real de Supabase la dan **las políticas RLS**, no el secreto de la key (que en apps web cliente es siempre visible). Lo crítico es no exponer la `service_role` key, que sí bypassa RLS.

---

## 7. Logbook técnico

Problemas reales encontrados durante el desarrollo y cómo se resolvieron:

| Problema | Causa raíz | Solución |
|---|---|---|
| `Property delegate must have a getValue...` al usar `by viewModel.medicion` | El ViewModel usaba `LiveData`, incompatible con Compose. | Migrar a `MutableState<T>` (idiomático de Compose). |
| `Class referenced in the manifest... was not found` | `import kotlin.getValue` inválido inyectado por autocomplete; Activity mezclaba Views y Compose. | Reescribir `MainActivity` en Compose puro, eliminar el import roto. |
| `InternalSerializationApi` opt-in en MeasurementDto | El plugin de `kotlin-serialization` se aplica pero el IDE marca uso de API interna. | `@OptIn(InternalSerializationApi::class)` sobre la `data class` + `-opt-in=…` en `kotlinOptions.freeCompilerArgs`. |
| `Expected URL scheme but no scheme found for sb_pub…` | URL y KEY invertidas en `local.properties`. | Invertir las dos líneas + agregar `require(url.startsWith("https://"))` en `RetrofitClient` para fallar temprano con mensaje claro. |
| `404 / PGRST125: Invalid path specified` con URL duplicada `/rest/v1/rest/v1/…` | El endpoint en `MeasurementApi` y la `BASE_URL` ambos contenían `/rest/v1/`. | Dejar `/rest/v1/` solo en `BASE_URL` (con `trimEnd('/')`), endpoints relativos: `@GET("measurements")`. |
| Android Studio cancela el `Run` con `CancellationException` | Daemon de Gradle colgado entre sesiones. | `./gradlew --stop` + Invalidate Caches → relanzar. |
| `Could not resolve` al fijar versiones | Conflictos por `configurations.all { force(...) }`. | Mantener `force` solo para `androidx.core`, `lifecycle-runtime` y `startup-runtime`. |

---

## 8. Pendientes técnicos

Mejoras identificadas pero aún no implementadas:

1. **Calibración del sensor:** las mediciones registradas con el celular en reposo sobre la mesa marcan G≈10 (correcto: es la gravedad). Hay que restar el vector gravedad para que `fuerzaG` represente movimiento neto, no aceleración total.
2. **Duplicación en el PULL:** al sincronizar, las mediciones que ya existen localmente se vuelven a insertar como filas nuevas porque el id local y el id remoto son distintos. Solución pendiente: agregar columna `remote_id` separada del `id` local.
3. **Sincronización automática:** hoy es manual (botón). Puede dispararse al volver la conectividad usando `WorkManager` o un listener de `ConnectivityManager`.
4. **Estado de error visible en la UI:** hoy los errores de red solo van a Logcat. Convendría exponerlos al usuario como un Snackbar o ícono.

---

## 9. Cómo correr el proyecto

1. Clonar el repo.
2. Crear `local.properties` con las dos líneas de Supabase (ver sección 6).
3. Abrir en Android Studio → esperar a que Gradle Sync termine.
4. Conectar un celular físico o levantar un emulador con sensor virtual.
5. Build → Run.

Para test rápido: en el celu, mover bruscamente el dispositivo. Las mediciones con G > 15 aparecen en el historial. Tocar **"Sync with Supabase"** → revisar el dashboard de Supabase para confirmar.

---

## 10. Créditos y materiales

- Cátedra: Desarrollo de Aplicaciones I — UADE
- Apuntes y clase del 28/05/2026 como base conceptual.
- Construido con la guía de un tutor IA siguiendo el patrón _pregunta/analogía/bajo el capó/riesgo/snippet_ para fijar cada decisión técnica.
