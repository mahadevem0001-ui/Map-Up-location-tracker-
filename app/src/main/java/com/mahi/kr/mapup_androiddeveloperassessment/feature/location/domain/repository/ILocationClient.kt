package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.repository

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface ILocationClient {

    fun getLocationUpdates(interval:Long): Flow<Location>

    suspend fun getLastKnownLocation(): Location?

    class LocationException(message:String): Exception()

}