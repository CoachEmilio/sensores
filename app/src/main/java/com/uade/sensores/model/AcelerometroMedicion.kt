package com.uade.sensores.model

import kotlin.math.sqrt

data class AcelerometroMedicion(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    // Fuerza G = módulo del vector aceleración lineal.
    // Como ahora usamos TYPE_LINEAR_ACCELERATION (sin gravedad),
    // este valor representa el MOVIMIENTO REAL, no la fuerza total.
    val fuerzaG: Float get() = sqrt(x * x + y * y + z * z)

    // Umbral ajustado para el sensor lineal (sin gravedad de fondo).
    // Con TYPE_ACCELEROMETER usábamos 15 porque el baseline era ~10 G.
    // Ahora el baseline es ~0 G, entonces 5 G ya es un movimiento brusco real.
    val esBrusco: Boolean get() = fuerzaG > 15
}