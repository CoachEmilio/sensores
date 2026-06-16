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

// Se diseña con SensorEventListener porque los recursos en móviles son limitados (batería y CPU)
// El modelo push permite que el sistema notifique los cambios mediante callbacks (Sensor → App)
// evitando que la app bloquee el hilo principal con consultas constantes e inútiles al hardware

class AcelerometroReader(
    context: Context,
    private val onMedicion: (AcelerometroMedicion) -> Unit
) : DefaultLifecycleObserver, SensorEventListener {

    // applicationContext: NUNCA guardamos el Context de la Activity (evita leaks)
    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val acelerometro: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // El control se INVIERTE: el componente se suscribe y libera SOLO
    override fun onResume(owner: LifecycleOwner) {
        val sensor = acelerometro ?: return  // Elvis: sin acelerómetro, no hago nada
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        Log.d("SENSOR", "onResume ► registerListener (auto)")
    }

    override fun onPause(owner: LifecycleOwner) {
        sensorManager.unregisterListener(this)
        Log.d("SENSOR", "onPause ► unregisterListener (auto)")
    }
    // (b) Dónde desuscribir y justificación por Simetría
    // Debe desuscribirse utilizando el metodo unregisterListener
    // Si registró el listener en onCreate, siguiendo la Regla de Simetría,
    // el lugar técnico para hacerlo es el onDestroy (su contraparte de existencia)
    // Sin embargo, la recomendación arquitectónica para sensores es usar el par onStart / onStop o onResume / onPause

    // El sensor nos habla a NOSOTROS (push)
    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
        onMedicion(AcelerometroMedicion(e.values[0], e.values[1], e.values[2]))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}