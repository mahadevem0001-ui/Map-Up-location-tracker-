package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.dao

import androidx.room.*
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.entity.LocationEntity

/**
 * DAO for Location operations
 */
@Dao
interface LocationDao {

    /**
     * Insert a single location
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    /**
     * Insert multiple locations
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationEntity>)

    /**
     * Delete a location
     */
    @Delete
    suspend fun deleteLocation(location: LocationEntity)

    /**
     * Get all locations for a session
     */
    @Query("SELECT * FROM locations WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getLocationsForSession(sessionId: Long): List<LocationEntity>

    /**
     * Delete all locations for a session
     */
    @Query("DELETE FROM locations WHERE sessionId = :sessionId")
    suspend fun deleteLocationsForSession(sessionId: Long)

    /**
     * Get location count for a session
     */
    @Query("SELECT COUNT(*) FROM locations WHERE sessionId = :sessionId")
    suspend fun getLocationCountForSession(sessionId: Long): Int

    /**
     * Delete all locations
     */
    @Query("DELETE FROM locations")
    suspend fun deleteAllLocations()
}
