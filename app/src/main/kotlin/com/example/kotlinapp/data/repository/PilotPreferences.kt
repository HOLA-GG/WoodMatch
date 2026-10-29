package com.example.kotlinapp.data.repository

import android.content.Context
import com.example.kotlinapp.data.model.SeasonalTheme
import java.util.Calendar

object PilotPreferences {
    const val TEXT_SCALE_KEY = "text_scale"
    const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"

    fun hasCompletedOnboarding(context: Context): Boolean =
        store(context).getBoolean(ONBOARDING_COMPLETED_KEY, false)

    fun completeOnboarding(context: Context) {
        store(context).edit().putBoolean(ONBOARDING_COMPLETED_KEY, true).apply()
    }

    /** App scaling is additive to Android accessibility settings, never a replacement. */
    fun normalizedTextScale(value: Float): Float =
        if (value.isFinite()) value.coerceIn(1f, 1.3f) else 1f

    fun seasonForMonth(month: Int): SeasonalTheme =
        when (month) {
            in 3..5 -> SeasonalTheme.SPRING
            in 6..8 -> SeasonalTheme.SUMMER
            in 9..11 -> SeasonalTheme.AUTUMN
            else -> SeasonalTheme.SNOW
        }

    fun resolvedSeason(
        mode: String,
        month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    ): SeasonalTheme =
        if (mode == "AUTO") seasonForMonth(month)
        else runCatching { SeasonalTheme.valueOf(mode) }.getOrDefault(SeasonalTheme.SUMMER)

    fun store(context: Context) =
        context.getSharedPreferences("pilot_preferences", Context.MODE_PRIVATE)
}
