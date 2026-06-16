package com.uade.sensores.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * Configuración del cliente HTTP. Singleton manual (Retrofit ya internamente
 * comparte conexiones, pero igual conviene una sola instancia).
 *
 * BASE_URL: cambiala por la URL real de tu backend.
 *   - Emulador Android Studio → "http://10.0.2.2:8080/" (apunta al localhost del host)
 *   - Celular físico en la misma red → "http://IP_DE_TU_PC:8080/"
 *   - Backend en internet → "https://api.tuservidor.com/"
 */
object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    // Logger: imprime cada request/response en Logcat. Útil para debug,
    // SACAR en release o filtrar info sensible (tokens, contraseñas).
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true   // si el server manda campos extra, los ignora
        coerceInputValues = true   // si manda null donde no debe, usa el default
    }

    val api: MeasurementApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MeasurementApi::class.java)
    }
}