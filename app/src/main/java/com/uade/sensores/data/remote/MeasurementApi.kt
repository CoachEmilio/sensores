package com.uade.sensores.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Contrato de la API REST.
 * Solo declaramos endpoints; Retrofit genera la implementación con KSP-like reflection.
 *
 * Todas las funciones son suspend → se ejecutan en una corrutina, no bloquean la UI.
 * Las llamadas a red pueden fallar (sin internet, timeout, 500): el llamador maneja errores.
 */
interface MeasurementApi {

    @GET("measurements")
    suspend fun obtenerTodas(): List<MeasurementDto>

    @GET("measurements/{id}")
    suspend fun obtenerPorId(@Path("id") id: Long): MeasurementDto

    @POST("measurements")
    suspend fun crear(@Body medicion: MeasurementDto): MeasurementDto
}