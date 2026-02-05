package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.dto

import androidx.room.Embedded
import androidx.room.Relation
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.entity.LocationEntity
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.entity.LocationSessionEntity

/**
 * DTO that combines LocationSession with its related Locations
 * Room will automatically fetch related locations using the @Relation annotation
 */
data class SessionWithLocations(
    @Embedded
    val session: LocationSessionEntity,

    @Relation(
        parentColumn = "sessionId",
        entityColumn = "sessionId"
    )
    val locations: List<LocationEntity>
)
