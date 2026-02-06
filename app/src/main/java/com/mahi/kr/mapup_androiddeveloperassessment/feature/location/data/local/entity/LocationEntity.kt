package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for Location Data
 * Each location belongs to a session (foreign key relationship)
 */
@Entity(
    tableName = "locations",
    foreignKeys = [
        ForeignKey(
            entity = LocationSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE // Delete locations when session is deleted
        )
    ],
    indices = [Index(value = ["sessionId"])] // Index for faster queries
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float?,
    val altitude: Double?,
    val speed: Float?,
    val bearing: Float?, // Direction of travel in degrees (0-360), null if stationary
    val address: String? = null
)
