package com.example.themepersistenceapp

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate
import android.widget.Toast

class MainActivity : ComponentActivity() {
    private lateinit var currentThemeTextView: TextView
    private lateinit var themeRadioGroup: RadioGroup
    private lateinit var saveButton: Button
    private lateinit var radioLight: RadioButton
    private lateinit var radioDark: RadioButton
    private lateinit var radioSystem: RadioButton

    private val PREFS_NAME = "ThemePrefs"
    private val PREF_THEME = "theme"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply saved theme before setting content view
        applySavedTheme()
        setContentView(R.layout.activity_main)

        // Initialize views
        currentThemeTextView = findViewById(R.id.currentThemeTextView)
        themeRadioGroup = findViewById(R.id.themeRadioGroup)
        saveButton = findViewById(R.id.saveButton)
        radioLight = findViewById(R.id.radioLight)
        radioDark = findViewById(R.id.radioDark)
        radioSystem = findViewById(R.id.radioSystem)

        // Load and display current theme
        updateThemeDisplay()

        // Set up save button click listener
        saveButton.setOnClickListener {
            saveThemePreference()
        }
    }

    private fun applySavedTheme() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val theme = sharedPreferences.getInt(PREF_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(theme)
    }

    private fun updateThemeDisplay() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val theme = sharedPreferences.getInt(PREF_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val themeText = when (theme) {
            AppCompatDelegate.MODE_NIGHT_NO -> "Light Theme"
            AppCompatDelegate.MODE_NIGHT_YES -> "Dark Theme"
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> "System Default"
            else -> "Not Set"
        }
        currentThemeTextView.text = "Current Theme: $themeText"

        // Update radio button selection
        when (theme) {
            AppCompatDelegate.MODE_NIGHT_NO -> radioLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> radioDark.isChecked = true
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> radioSystem.isChecked = true
        }
    }

    private fun saveThemePreference() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val selectedTheme = when (themeRadioGroup.checkedRadioButtonId) {
            R.id.radioLight -> AppCompatDelegate.MODE_NIGHT_NO
            R.id.radioDark -> AppCompatDelegate.MODE_NIGHT_YES
            R.id.radioSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> return // No selection, do nothing
        }

        editor.putInt(PREF_THEME, selectedTheme)
        editor.apply()

        // Apply the theme
        AppCompatDelegate.setDefaultNightMode(selectedTheme)
        updateThemeDisplay()
        Toast.makeText(this, "Theme saved and applied", Toast.LENGTH_SHORT).show()
    }
}