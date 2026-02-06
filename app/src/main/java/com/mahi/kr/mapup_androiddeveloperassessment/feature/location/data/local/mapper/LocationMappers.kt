package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.mapper

import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.dto.SessionWithLocations
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.entity.LocationEntity
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.entity.LocationSessionEntity
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationData
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationSession

/**
 * Convert LocationSession domain model to LocationSessionEntity
 */
fun LocationSession.toEntity(): LocationSessionEntity {
    return LocationSessionEntity(
        sessionId = sessionId,
        startTime = startTime,
        endTime = endTime,
        locationCount = locations.size
    )
}

/**
 * Convert LocationData domain model to LocationEntity
 */
fun LocationData.toEntity(sessionId: Long): LocationEntity {
    return LocationEntity(
        sessionId = sessionId,
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp,
        accuracy = accuracy,
        altitude = altitude,
        speed = speed,
        bearing = bearing,
        address = address
    )
}

/**
 * Convert LocationEntity to LocationData domain model
 */
fun LocationEntity.toDomain(): LocationData {
    return LocationData(
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp,
        accuracy = accuracy,
        altitude = altitude,
        speed = speed,
        bearing = bearing,
        address = address
    )
}

/**
 * Convert SessionWithLocations DTO to LocationSession domain model
 */
fun SessionWithLocations.toDomain(): LocationSession {
    return LocationSession(
        sessionId = session.sessionId,
        startTime = session.startTime,
        endTime = session.endTime,
        locations = locations.map { it.toDomain() }
    )
}
