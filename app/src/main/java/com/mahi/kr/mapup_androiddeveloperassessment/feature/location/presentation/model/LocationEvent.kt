package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.model

/** One-off UI events for the location feature. */
sealed class LocationEvent {
    data class ShowMessage(val message: String) : LocationEvent()
    data class PermissionRevoked(val message: String) : LocationEvent()
    data class ProviderDisabled(val message: String) : LocationEvent()
}
