package cit.edu.KlockApp

import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar

class ProfileActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        // Key for DayNight mode (if kept separate)
        // const val PREF_KEY_DAYNIGHT_MODE = "app_daynight_mode"

        // REMOVE Key and constants for color theme overlay selection
        // const val PREF_KEY_COLOR_THEME = "app_color_theme"
        // const val THEME_OVERLAY_DEFAULT = "default"
        // const val THEME_OVERLAY_OXFORD = "oxford"

        // RESTORE constants for Full Theme Resource IDs
        const val PREF_KEY_THEME_ID = "app_theme_id" // Use this key
        val THEME_DEFAULT_ID = R.style.Theme_Klock_Default // Use the actual resource ID
        val THEME_OXFORD_ID = R.style.Theme_Klock_Oxford   // Use the actual resource ID
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply FULL theme from prefs BEFORE super.onCreate()
        applyAppTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_profile)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        val buttonLogout = findViewById<Button>(R.id.button_logout)
        buttonLogout.setOnClickListener {
            showLogoutDialog()
        }

        setupThemePaletteSelection()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { dialog, which ->
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun setupThemePaletteSelection() {
        val themeRadioGroup = findViewById<RadioGroup>(R.id.radio_group_theme_palette)
        // Read the saved theme ID (Int)
        val currentThemeId = sharedPreferences.getInt(PREF_KEY_THEME_ID, THEME_DEFAULT_ID)

        // Set initial radio button state based on the saved ID
        when (currentThemeId) {
            THEME_OXFORD_ID -> themeRadioGroup.check(R.id.radio_theme_oxford)
            else -> themeRadioGroup.check(R.id.radio_theme_default) // Default case
        }

        // Listener for theme changes
        themeRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedThemeId = when (checkedId) {
                R.id.radio_theme_oxford -> THEME_OXFORD_ID
                else -> THEME_DEFAULT_ID // Default case
            }

            // Save the new theme ID (Int)
            if (selectedThemeId != currentThemeId) {
                sharedPreferences.edit().putInt(PREF_KEY_THEME_ID, selectedThemeId).apply()
                // Recreate the activity to apply the new theme immediately
                recreate()
            }
        }
    }

    // Function to apply FULL theme based on SharedPreferences
    private fun applyAppTheme() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this) // Ensure initialized
        val themeResId = sharedPreferences.getInt(PREF_KEY_THEME_ID, THEME_DEFAULT_ID)
        setTheme(themeResId) // Apply the chosen FULL theme
    }
}