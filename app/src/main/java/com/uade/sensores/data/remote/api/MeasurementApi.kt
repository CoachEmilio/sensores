package com.uade.sensores.data.remote.api

import com.uade.sensores.data.remote.dto.MeasurementDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Contrato de la API REST de Supabase para la tabla `measurements`.
 *
 * IMPORTANTE: los endpoints son RELATIVOS a la BASE_URL del RetrofitClient.
 * La BASE_URL ya incluye "/rest/v1/" → acá solo va "measurements", NUNCA "rest/v1/measurements".
 *
 * Todas las funciones son suspend → se ejecutan en una corrutina, no bloquean la UI.
 *
 * Header "Prefer: return=representation" en el POST → pide que el server devuelva
 * la fila creada con su id autogenerado. Sin esto, Supabase devuelve body vacío.
 *
 * Supabase SIEMPRE devuelve arrays JSON, incluso para crear UN solo elemento.
 * Por eso `crear()` devuelve List<MeasurementDto>, no MeasurementDto.
 */
interface MeasurementApi {

    /**
     * GET https://<proyecto>.supabase.co/rest/v1/measurements?select=*
     * Devuelve TODAS las mediciones del backend.
     */
    @GET("measurements")
    suspend fun obtenerTodas(
        @Query("select") select: String = "*"
    ): List<MeasurementDto>

    /**
     * POST https://<proyecto>.supabase.co/rest/v1/measurements
     * Crea una medición nueva en el backend.
     */
    @POST("measurements")
    suspend fun crear(
        @Body medicion: MeasurementDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<MeasurementDto>
}