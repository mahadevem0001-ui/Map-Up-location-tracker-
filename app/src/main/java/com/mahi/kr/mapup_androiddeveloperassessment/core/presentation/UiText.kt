package com.mahi.kr.mapup_androiddeveloperassessment.core.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Sealed interface for UI text representation
 * Supports both dynamic strings and string resources with arguments
 */
sealed interface UiText {
    /**
     * Dynamic string that doesn't require localization
     */
    data class DynamicString(val value: String) : UiText

    /**
     * Localized string resource with optional formatting arguments
     */
    class StringResourceId(
        @StringRes val id: Int,
        val args: Array<Any> = arrayOf()
    ) : UiText {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StringResourceId) return false

            if (id != other.id) return false
            if (!args.contentEquals(other.args)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

    /**
     * Convert UiText to String based on the type
     */
    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResourceId -> stringResource(id = id, formatArgs = args)
        }
    }
}
