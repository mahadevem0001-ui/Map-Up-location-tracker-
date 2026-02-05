package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.repository

import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.DataError
import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.EmptyResult
import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.Result
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationData
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationSession
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Location Session operations
 * Defines the contract for data operations
 */
interface LocationSessionRepository {

    /**
     * Create a new tracking session
     */
    suspend fun createSession(session: LocationSession): EmptyResult<DataError>

    /**
     * Update an existing session (e.g., set end time)
     */
    suspend fun updateSession(session: LocationSession): EmptyResult<DataError>

    /**
     * Add a location to a session
     */
    suspend fun addLocationToSession(sessionId: Long, location: LocationData): EmptyResult<DataError>

    /**
     * Get a session by ID with all its locations
     */
    suspend fun getSessionById(sessionId: Long): Result<LocationSession?, DataError>

    /**
     * Get all sessions with locations as Flow (reactive)
     */
    fun getAllSessions(): Flow<Result<List<LocationSession>, DataError>>

    /**
     * Get recent sessions (limit)
     */
    suspend fun getRecentSessions(limit: Int): Result<List<LocationSession>, DataError>

    /**
     * Get the currently active session (endTime is null)
     */
    suspend fun getActiveSession(): Result<LocationSession?, DataError>

    /**
     * Delete a session
     */
    suspend fun deleteSession(sessionId: Long): EmptyResult<DataError>

    /**
     * Delete all sessions
     */
    suspend fun deleteAllSessions(): EmptyResult<DataError>
}
