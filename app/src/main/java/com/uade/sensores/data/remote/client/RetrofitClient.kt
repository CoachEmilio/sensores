package com.uade.sensores.data.remote.client

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.uade.sensores.BuildConfig
import com.uade.sensores.data.remote.api.MeasurementApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * Cliente Retrofit configurado para Supabase REST API.
 *
 * Carga la URL y la key desde BuildConfig (que las lee de local.properties).
 * Las keys NUNCA están hardcodeadas en el código.
 *
 * Cada request lleva 2 headers obligatorios:
 *  - apikey: identifica el proyecto Supabase.
 *  - Authorization: Bearer <key>: identifica el rol (publishable = público).
 *
 * El interceptor agrega los headers automáticamente a TODAS las llamadas.
 */
object RetrofitClient {

    /**
     * BASE_URL = https://<proyecto>.supabase.co/rest/v1/
     *
     * - trimEnd('/') por si SUPABASE_URL viene con barra al final (evita dobles barras).
     * - require() valida que sea https en runtime para detectar errores de config temprano.
     */
    private val BASE_URL: String = run {
        val url = BuildConfig.SUPABASE_URL.trimEnd('/')
        require(url.startsWith("https://")) {
            "SUPABASE_URL debe empezar con https:// — revisá local.properties. Valor actual: '$url'"
        }
        "$url/rest/v1/"
    }

    private val supabaseAuthInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("apikey", BuildConfig.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_KEY}")
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(supabaseAuthInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
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