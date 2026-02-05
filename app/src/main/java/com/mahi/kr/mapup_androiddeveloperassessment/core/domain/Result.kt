package com.mahi.kr.mapup_androiddeveloperassessment.core.domain

/**
 * A generic Result type for functional error handling
 *
 * @param D The type of data on success
 * @param E The type of error that implements Error interface
 */
sealed interface Result<out D, out E : Error> {
    /**
     * Represents a successful result containing data
     */
    data class Success<out D>(val data: D) : Result<D, Nothing>

    /**
     * Represents a failed result containing an error
     */
    data class Error<out E : com.mahi.kr.mapup_androiddeveloperassessment.core.domain.Error>(
        val error: E
    ) : Result<Nothing, E>
}

/**
 * Maps the success data to a new type while preserving errors
 */
inline fun <T, E : Error, R> Result<T, E>.map(map: (T) -> R): Result<R, E> {
    return when (this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(map(data))
    }
}

/**
 * Converts a Result<T, E> to EmptyResult<E> (Result<Unit, E>)
 * Useful when you only care about success/failure, not the data
 */
fun <T, E : Error> Result<T, E>.asEmptyDataResult(): EmptyResult<E> {
    return map { }
}

/**
 * Executes the given action if the result is Success
 * Returns the original result for chaining
 */
inline fun <T, E : Error> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    return when (this) {
        is Result.Error -> this
        is Result.Success -> {
            action(data)
            this
        }
    }
}

/**
 * Executes the given action if the result is Error
 * Returns the original result for chaining
 */
inline fun <T, E : Error> Result<T, E>.onError(action: (E) -> Unit): Result<T, E> {
    return when (this) {
        is Result.Error -> {
            action(error)
            this
        }
        is Result.Success -> this
    }
}

/**
 * Type alias for results that don't return data, only success/failure
 */
typealias EmptyResult<E> = Result<Unit, E>
