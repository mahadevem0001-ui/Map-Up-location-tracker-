package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.dao

import androidx.room.*
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.dto.SessionWithLocations
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.entity.LocationEntity
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.entity.LocationSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Location Session operations
 */
@Dao
interface LocationSessionDao {

    /**
     * Insert a new session
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: LocationSessionEntity)

    /**
     * Update an existing session
     */
    @Update
    suspend fun updateSession(session: LocationSessionEntity)

    /**
     * Delete a session (will cascade delete all related locations)
     */
    @Delete
    suspend fun deleteSession(session: LocationSessionEntity)

    /**
     * Get a session by ID with all its locations
     */
    @Transaction
    @Query("SELECT * FROM location_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionWithLocations(sessionId: Long): SessionWithLocations?

    /**
     * Get all sessions with their locations, ordered by start time (newest first)
     */
    @Transaction
    @Query("SELECT * FROM location_sessions ORDER BY startTime DESC")
    fun getAllSessionsWithLocations(): Flow<List<SessionWithLocations>>

    /**
     * Get all sessions with their locations (non-Flow version for one-time fetch)
     */
    @Transaction
    @Query("SELECT * FROM location_sessions ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentSessionsWithLocations(limit: Int): List<SessionWithLocations>

    /**
     * Get the active session (endTime is null)
     */
    @Transaction
    @Query("SELECT * FROM location_sessions WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveSession(): SessionWithLocations?

    /**
     * Delete all sessions
     */
    @Query("DELETE FROM location_sessions")
    suspend fun deleteAllSessions()

    /**
     * Get session count
     */
    @Query("SELECT COUNT(*) FROM location_sessions")
    suspend fun getSessionCount(): Int
}
