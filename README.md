# App Android de Control de Calidad Industrial con Sensores, Room y Jetpack Compose

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-757575?style=for-the-badge&logo=materialdesign&logoColor=white)
![Room](https://img.shields.io/badge/Room-4DB33D?style=for-the-badge&logo=sqlite&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![KSP](https://img.shields.io/badge/KSP-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Coroutines](https://img.shields.io/badge/Coroutines-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![StateFlow](https://img.shields.io/badge/StateFlow-FF6F00?style=for-the-badge&logo=kotlin&logoColor=white)

---

## 1. Objetivo de la Aplicación

Esta aplicación Android modela un caso simple de control de calidad industrial usando sensores del dispositivo. La idea central es usar el teléfono como **inclinómetro**: se apoya sobre una superficie, se calibra una posición base y luego se registran mediciones para determinar si una pieza, molde, soporte o estación de trabajo se encuentra dentro de una tolerancia.

La app usa el **acelerómetro** para estimar ángulos:

- **Pitch**: inclinación hacia adelante y hacia atrás.
- **Roll**: inclinación lateral.

Con esos valores, la aplicación permite:

1. Leer la inclinación actual del dispositivo.
2. Calibrar una posición base.
3. Definir una tolerancia de aceptación.
4. Guardar mediciones localmente con Room.
5. Mostrar un dashboard con estadísticas básicas.
6. Ejecutar un análisis local sobre los datos registrados.

El punto importante no es solamente leer un sensor. La app convierte esa lectura física en una **decisión de negocio**: determinar si una medición está dentro o fuera de la tolerancia.

---

## 2. Componentes Principales

La aplicación combina varias piezas habituales en una app Android moderna:

- Kotlin
- Jetpack Compose
- Material 3 Design
- Sensores Android
    - `SensorManager`
    - `SensorEventListener`
- Room sobre SQLite
- DAO
- Repository
- ViewModel
- StateFlow
- Corrutinas
- Separación entre UI, lógica, sensor y datos
- Cálculo local sobre registros históricos

La arquitectura mantiene separadas las responsabilidades. La UI renderiza estado y envía eventos. El ViewModel coordina el estado de pantalla y las operaciones. La lógica de negocio queda aislada. El sensor se encapsula en una clase propia. Room queda detrás de un DAO y un Repository.

---

## 3. Estructura del Proyecto

La estructura sugerida es la siguiente:

app/
└── kotlin+java
└── com.uade.sensores
├── data/
│   ├── AppDatabase.kt
│   ├── Measurement.kt
│   ├── MeasurementDao.kt
│   └── MeasurementRepository.kt
│
├── logic/
│   ├── QualityCalculator.kt
│   └── QualityViewModel.kt
│
├── sensor/
│   └── InclinometerSensor.kt
│
└── ui/
└── QualityScreen.kt

| Carpeta | Responsabilidad |
|---------|-----------------|
| `data/` | Entidad, DAO, base de datos Room y Repository. |
| `logic/` | Reglas de negocio, cálculo de tolerancia y ViewModel. |
| `sensor/` | Acceso al acelerómetro y conversión de lecturas en ángulos. |
| `ui/` | Pantallas y componentes visuales con Jetpack Compose. |
| raíz | `MainActivity`, inicialización de dependencias y carga de la UI. |

Esta estructura evita que la aplicación termine concentrada en una única `Activity` o en un único `@Composable`. También permite cambiar una parte sin arrastrar a las demás: así podemos reemplazar Room, ajustar el cálculo de aceptación o mejorar la UI sin reescribir toda la app.

---

## 4. Dependencias del Proyecto

En el `build.gradle.kts` del módulo `app` se declaran los plugins y dependencias necesarios para Compose, Room, ViewModel y KSP:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}
```

Los plugins principales son:

- **`android.application`**: permite compilar una app Android.
- **`kotlin.android`**: habilita Kotlin sobre Android.
- **`kotlin.compose`**: habilita Jetpack Compose.
- **`com.google.devtools.ksp`**: permite que Room genere código a partir de anotaciones.

Room necesita procesamiento de símbolos para interpretar anotaciones como `@Entity`, `@Dao` y `@Database`. Ese trabajo lo hace **KSP**.

<!-- TODO: completar con el resto de la explicación del profe sobre qué pasa si KSP no está configurado -->

La configuración base del módulo puede quedar así:

```kotlin
android {
    namespace = "com.uade.sensores"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.uade.sensores"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
```

<!-- TODO: completar con el bloque dependencies { } del profe (librerías de Room, Compose, ViewModel, etc.) -->

---

## 5. Capa Data

La capa **data** contiene todo lo relacionado con la persistencia local. En esta app se compone de cuatro archivos:

data/
├── Measurement.kt
├── MeasurementDao.kt
├── AppDatabase.kt
└── MeasurementRepository.kt

La idea es que Room y SQLite **no queden mezclados con la UI ni con el cálculo de calidad**. El ViewModel no debería armar SQL, abrir bases, ni conocer detalles internos.

<!-- TODO: completar con el detalle de Measurement.kt, MeasurementDao.kt, AppDatabase.kt y MeasurementRepository.kt del profe -->

## 6. Logbook de Arreglos Técnicos (Examen Prep)

Este apartado detalla los problemas críticos encontrados en la configuración de Gradle y el entorno, y cómo se resolvieron para que la app compile y corra. **Guía de supervivencia para el examen.**

### 🛠️ Problemas de Gradle y Dependencias

| Qué estaba roto | Por qué fallaba | Cambio realizado |
|:---|:---|:---|
| **Versiones "Fantasma"** | Librerías como `androidx.core` subían solas a la v1.19.0 (requieren AGP 9.1+). | Se usó `resolutionStrategy.force` para clavar versiones en **1.13.1**. |
| **Crash al iniciar (Startup)** | `NoClassDefFoundError` en `androidx.startup`. Versiones incompatibles entre sí. | Se forzó `androidx.startup:startup-runtime:1.1.1` en el build.gradle. |
| **Room + KSP** | Incompatibilidad entre el plugin de Kotlin y la versión de KSP. | Se sincronizó **Kotlin 2.1.0** con **KSP 2.1.0-1.0.29**. |
| **Aviso de BuildConfig** | `buildConfig=true` está depreciado en `gradle.properties`. | Se movió a `build.gradle.kts` dentro de `android { buildFeatures { buildConfig = true } }`. |
| **Theme Error** | Error `Theme.AppCompat... not found` al compilar recursos. | Se agregó la dependencia `androidx.appcompat:appcompat` (necesaria para temas XML). |

### 💾 Gestión de Memoria y Disco

Si el disco está casi lleno (>95%), Android Studio y Gradle **rompen** porque no pueden escribir archivos temporales.

**Comandos de limpieza (Terminal):**
1. **Limpiar build del proyecto:** `./gradlew clean` (borra la carpeta `app/build`).
2. **Parar Daemons trabados:** `./gradlew --stop` (libera RAM de procesos Gradle colgados).
3. **Limpieza profunda de Caches (Cuidado):**
   - `rm -rf ~/.gradle/caches` (Borra todas las librerías bajadas, las vuelve a bajar al compilar).
   - `rm -rf ~/Library/Caches/Google/AndroidStudio*` (Borra archivos temporales del IDE).

### 💡 Tips para el Examen
* **MinSdk:** Asegurate que sea 24 o 26 según pida la cátedra.
* **Namespace:** Debe coincidir exactamente con tu package (`com.uade.sensores`).
* **KSP:** Si Room no genera el código de la DB, revisá que el plugin de KSP coincida **exactamente** con la versión de Kotlin.
* **Sync:** Si el Sync falla por versiones de Android APIs (v37), bajá el `compileSdk` y `targetSdk` a **35** y forzá las dependencias de `core` a **1.13.1**.

---