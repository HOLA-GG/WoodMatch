package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinapp.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupThemeSelection()
        setupBottomNavigation()
    }

    private fun setupThemeSelection() {
        val currentTheme = ThemeHelper.getSelectedTheme(this)
        when (currentTheme) {
            ThemeHelper.THEME_SPRING -> binding.radioSpring.isChecked = true
            ThemeHelper.THEME_AUTUMN -> binding.radioAutumn.isChecked = true
            ThemeHelper.THEME_SNOW -> binding.radioSnow.isChecked = true
        }

        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = when (checkedId) {
                R.id.radioSpring -> ThemeHelper.THEME_SPRING
                R.id.radioAutumn -> ThemeHelper.THEME_AUTUMN
                R.id.radioSnow -> ThemeHelper.THEME_SNOW
                else -> ThemeHelper.THEME_SPRING
            }
            ThemeHelper.setSelectedTheme(this, newTheme)
            recreate()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.navigation_settings
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, SecondActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_settings -> true
                else -> false
            }
        }
    }
}
