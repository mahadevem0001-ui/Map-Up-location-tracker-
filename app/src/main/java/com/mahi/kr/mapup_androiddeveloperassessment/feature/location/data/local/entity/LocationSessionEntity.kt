package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for Location Session
 * Represents a tracking session with start/end times
 */
@Entity(tableName = "location_sessions")
data class LocationSessionEntity(
    @PrimaryKey
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long?,
    val locationCount: Int = 0
)
