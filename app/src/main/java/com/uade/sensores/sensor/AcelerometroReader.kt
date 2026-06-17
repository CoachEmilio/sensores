package com.uade.sensores.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.uade.sensores.model.AcelerometroMedicion

/**
 * Lector del acelerómetro con calibración por software (filtro pasa-altos).
 *
 * Como no todos los dispositivos exponen TYPE_LINEAR_ACCELERATION (requiere giroscopio),
 * usamos TYPE_ACCELEROMETER y le restamos la gravedad nosotros mismos con un filtro
 * pasa-altos clásico: estimamos la gravedad como un promedio de baja frecuencia
 * y la restamos al valor crudo. Lo que queda es la aceleración lineal.
 *
 * Esta es la forma estándar de implementar el LINEAR_ACCELERATION cuando el sistema
 * no lo provee.
 */
class AcelerometroReader(
    context: Context,
    private val onMedicion: (AcelerometroMedicion) -> Unit
) : DefaultLifecycleObserver, SensorEventListener {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Usamos el acelerómetro crudo y filtramos la gravedad por software.
    private val acelerometro: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).also {
            Log.d("SENSOR", "Sensor inicializado: ${it?.name ?: "ninguno"}")
        }

    /**
     * Gravedad estimada (componente lenta del acelerómetro).
     * Se actualiza con un filtro pasa-bajos: arranca en 0 y converge al valor real
     * en pocos eventos (~50ms con SENSOR_DELAY_UI).
     */
    private val gravedad = FloatArray(3)

    /**
     * Constante del filtro: cuánto del valor anterior conservar.
     *  - α alto (0.9) → gravedad muy lenta, captura solo cambios muy graduales.
     *  - α bajo (0.5) → gravedad reactiva, deja pasar menos movimiento.
     *  0.8 es el valor que recomienda Google en la documentación oficial.
     */
    private val alpha = 0.8f

    override fun onResume(owner: LifecycleOwner) {
        val sensor = acelerometro ?: return
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        Log.d("SENSOR", "onResume ► registerListener (auto)")
    }

    override fun onPause(owner: LifecycleOwner) {
        sensorManager.unregisterListener(this)
        Log.d("SENSOR", "onPause ► unregisterListener (auto)")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return

        // 1) Actualizamos la estimación de gravedad (filtro pasa-bajos).
        gravedad[0] = alpha * gravedad[0] + (1 - alpha) * e.values[0]
        gravedad[1] = alpha * gravedad[1] + (1 - alpha) * e.values[1]
        gravedad[2] = alpha * gravedad[2] + (1 - alpha) * e.values[2]

        // 2) Restamos la gravedad al valor crudo → nos queda la aceleración lineal.
        val linealX = e.values[0] - gravedad[0]
        val linealY = e.values[1] - gravedad[1]
        val linealZ = e.values[2] - gravedad[2]

        onMedicion(AcelerometroMedicion(linealX, linealY, linealZ))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}