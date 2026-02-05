package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.mahi.kr.mapup_androiddeveloperassessment.core.util.extensions.hasPermission
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.ILocationClient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FusedLocationClientImpl(
    private val context: Context,
    private val client: FusedLocationProviderClient
) : ILocationClient {

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow {

            if (!context.hasPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) throw ILocationClient.LocationException("Location permissions not granted")

            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (!isGpsEnabled && !isNetworkEnabled) {
                throw ILocationClient.LocationException("Location providers are disabled")
            }

            val request = LocationRequest.Builder(interval)
                .setIntervalMillis(interval)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    super.onLocationResult(result)
                    result.locations.lastOrNull()?.let { location ->
                        trySend(location)
                    }
                }
            }

            client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}