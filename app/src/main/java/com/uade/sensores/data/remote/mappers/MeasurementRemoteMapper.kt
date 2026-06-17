package com.uade.sensores.data.remote.mappers

import com.uade.sensores.data.remote.dto.MeasurementDto
import com.uade.sensores.model.AcelerometroMedicion

/**
 * Mappers entre el DTO (capa remota) y el modelo de dominio.
 *
 * ¿Por qué un archivo separado del DTO?
 *  - SRP (Single Responsibility): MeasurementDto define cómo viaja el JSON;
 *    este archivo define cómo se traduce entre el mundo de la red y el dominio.
 *  - Si cambia el modelo de dominio, solo se toca este archivo, no el DTO.
 *  - Es coherente con MeasurementMapper en data/local: la misma idea, otra capa.
 *
 * Como ambos mappers (local y remote) trabajan con AcelerometroMedicion, los
 * mantenemos en archivos separados con nombres distintos para que no haya
 * ambigüedad al importar (toDomain del Entity vs toDomain del DTO).
 */

/**
 * DTO → Dominio.
 * Se usa cuando RECIBIMOS datos del backend: el response trae MeasurementDto,
 * lo convertimos a AcelerometroMedicion antes de entregárselo al Repository.
 *
 * Notar que NO mapeamos id ni created_at: son detalles de persistencia remota
 * que al dominio no le importan.
 */
fun MeasurementDto.toDomain(): AcelerometroMedicion =
    AcelerometroMedicion(
        x = x,
        y = y,
        z = z,
        timestamp = timestamp
    )

/**
 * Dominio → DTO.
 * Se usa cuando ENVIAMOS al backend: el Repository tiene un AcelerometroMedicion,
 * lo convertimos a MeasurementDto para que Retrofit lo serialice a JSON.
 *
 * id NO se setea: Supabase lo autogenera del lado del servidor (BIGSERIAL).
 */
fun AcelerometroMedicion.toDto(): MeasurementDto =
    MeasurementDto(
        x = x,
        y = y,
        z = z,
        timestamp = timestamp
    )