package com.mahi.kr.mapup_androiddeveloperassessment.core.domain

/**
 * Usage Examples for Result and DataError
 *
 * Example 1: Basic Result usage
 * ```
 * fun fetchData(): Result<String, DataError> {
 *     return try {
 *         val data = // ... fetch data
 *         Result.Success(data)
 *     } catch (e: IOException) {
 *         Result.Error(DataError.Remote.NO_INTERNET)
 *     }
 * }
 * ```
 *
 * Example 2: Using map and onSuccess
 * ```
 * fetchData()
 *     .map { data -> data.toUpperCase() }
 *     .onSuccess { transformed ->
 *         println("Success: $transformed")
 *     }
 *     .onError { error ->
 *         showError(error.toUiText())
 *     }
 * ```
 *
 * Example 3: In Composable
 * ```
 * @Composable
 * fun ErrorDisplay(error: DataError) {
 *     val errorText = error.toUiText().asString()
 *     Text(text = errorText, color = Color.Red)
 * }
 * ```
 *
 * Example 4: EmptyResult (when you don't care about return data)
 * ```
 * fun saveData(data: String): EmptyResult<DataError> {
 *     return try {
 *         // ... save data
 *         Result.Success(Unit)
 *     } catch (e: IOException) {
 *         Result.Error(DataError.Local.DISK_FULL)
 *     }
 * }
 * ```
 */
