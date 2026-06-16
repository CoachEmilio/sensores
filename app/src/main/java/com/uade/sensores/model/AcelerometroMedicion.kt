package com.uade.sensores.model

//import math.sqrt para
import kotlin.math.sqrt

data class AcelerometroMedicion(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    // La fuerza G = módulo del vector (física del secundario)
    val fuerzaG: Float get() = sqrt(x * x + y * y + z * z)
    val esBrusco: Boolean get() = fuerzaG > 15
}