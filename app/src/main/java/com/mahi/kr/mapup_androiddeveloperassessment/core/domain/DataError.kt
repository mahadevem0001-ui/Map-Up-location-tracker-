package com.mahi.kr.mapup_androiddeveloperassessment.core.domain

/**
 * Sealed interface for data-related errors
 * Categorizes errors into Remote (network) and Local (storage) errors
 */
sealed interface DataError : Error {
    /**
     * Remote/Network related errors
     */
    enum class Remote : DataError {
        REQUEST_TIMEOUT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        SERVER,
        SERIALIZATION,
        UNKNOWN
    }

    /**
     * Local/Storage related errors
     */
    enum class Local : DataError {
        DISK_FULL,
        UNKNOWN
    }
}
