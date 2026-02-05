package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.repository

import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.DataError
import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.EmptyResult
import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.Result
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.dao.LocationDao
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.dao.LocationSessionDao
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.mapper.toDomain
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.mapper.toEntity
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationData
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationSession
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.repository.LocationSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Implementation of LocationSessionRepository using Room database
 */
class LocationSessionRepositoryImpl(
    private val sessionDao: LocationSessionDao,
    private val locationDao: LocationDao
) : LocationSessionRepository {

    override suspend fun createSession(session: LocationSession): EmptyResult<DataError> {
        return try {
            sessionDao.insertSession(session.toEntity())

            // Insert all locations if any
            if (session.locations.isNotEmpty()) {
                val locationEntities = session.locations.map { it.toEntity(session.sessionId) }
                locationDao.insertLocations(locationEntities)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun updateSession(session: LocationSession): EmptyResult<DataError> {
        return try {
            sessionDao.updateSession(session.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun addLocationToSession(
        sessionId: Long,
        location: LocationData
    ): EmptyResult<DataError> {
        return try {
            locationDao.insertLocation(location.toEntity(sessionId))

            // Update session location count
            val session = sessionDao.getSessionWithLocations(sessionId)
            session?.let {
                val updatedSession = it.session.copy(
                    locationCount = it.locations.size + 1
                )
                sessionDao.updateSession(updatedSession)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun getSessionById(sessionId: Long): Result<LocationSession?, DataError> {
        return try {
            val sessionWithLocations = sessionDao.getSessionWithLocations(sessionId)
            Result.Success(sessionWithLocations?.toDomain())
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override fun getAllSessions(): Flow<Result<List<LocationSession>, DataError>> {
        return sessionDao.getAllSessionsWithLocations()
            .map<List<com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.dto.SessionWithLocations>, Result<List<LocationSession>, DataError>> { sessionsList ->
                Result.Success(sessionsList.map { it.toDomain() })
            }
            .catch { e ->
                emit(Result.Error(DataError.Local.UNKNOWN))
            }
    }

    override suspend fun getRecentSessions(limit: Int): Result<List<LocationSession>, DataError> {
        return try {
            val sessions = sessionDao.getRecentSessionsWithLocations(limit)
            Result.Success(sessions.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun getActiveSession(): Result<LocationSession?, DataError> {
        return try {
            val activeSession = sessionDao.getActiveSession()
            Result.Success(activeSession?.toDomain())
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun deleteSession(sessionId: Long): EmptyResult<DataError> {
        return try {
            val session = sessionDao.getSessionWithLocations(sessionId)
            session?.let {
                sessionDao.deleteSession(it.session)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun deleteAllSessions(): EmptyResult<DataError> {
        return try {
            sessionDao.deleteAllSessions()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }
}
