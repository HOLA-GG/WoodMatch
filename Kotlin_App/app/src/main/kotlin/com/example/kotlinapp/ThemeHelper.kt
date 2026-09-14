package com.example.kotlinapp

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity

object ThemeHelper {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"

    const val THEME_SPRING = "spring"
    const val THEME_AUTUMN = "autumn"
    const val THEME_SNOW = "snow"

    fun getSelectedTheme(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME, THEME_SPRING) ?: THEME_SPRING
    }

    fun setSelectedTheme(context: Context, theme: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun applyTheme(activity: AppCompatActivity) {
        when (getSelectedTheme(activity)) {
            THEME_AUTUMN -> activity.setTheme(R.style.Theme_KotlinApp_Autumn)
            THEME_SNOW -> activity.setTheme(R.style.Theme_KotlinApp_Snow)
            else -> activity.setTheme(R.style.Theme_KotlinApp) // Default Spring
        }
    }
}
