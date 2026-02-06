package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.dao.LocationDao
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.dao.LocationSessionDao
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.entity.LocationEntity
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.entity.LocationSessionEntity

/**
 * Room Database for Location Tracking
 * Version 1: Initial schema with sessions and locations
 */
@Database(
    entities = [
        LocationSessionEntity::class,
        LocationEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class LocationDatabase : RoomDatabase() {

    abstract fun locationSessionDao(): LocationSessionDao
    abstract fun locationDao(): LocationDao

    companion object {
        const val DATABASE_NAME = "location_tracking.db"

        val MIGRATION_1_2 = androidx.room.migration.Migration(1, 2) { db ->
            db.execSQL("ALTER TABLE locations ADD COLUMN address TEXT")
        }
    }
}
