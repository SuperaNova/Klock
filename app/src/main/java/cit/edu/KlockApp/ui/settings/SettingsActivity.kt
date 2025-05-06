package cit.edu.KlockApp.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import cit.edu.KlockApp.AboutUsActivity
import cit.edu.KlockApp.LoginActivity
import cit.edu.KlockApp.ProfileActivity
import cit.edu.KlockApp.R
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import com.google.android.material.appbar.MaterialToolbar

class SettingsActivity : AppCompatActivity() {

    // Define preference key constant
    companion object {
        const val PREF_KEY_24_HOUR = "use_24_hour_format"
    }

    private lateinit var sharedPreferences: SharedPreferences
    private var checkbox24Hour: CheckBox? = null
    private var buttonBack: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply FULL theme from prefs BEFORE super.onCreate()
        applyAppTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Find Views safely
        buttonBack = findViewById(R.id.back_button)
        checkbox24Hour = findViewById(R.id.checkbox_24_hour)
        val buttonProfile = findViewById<Button>(R.id.profile_view)
        val buttonDeveloper = findViewById<Button>(R.id.about_us)

        // Basic null checks after finding views (can be more robust)
        if (buttonBack == null || checkbox24Hour == null || buttonProfile == null || buttonDeveloper == null) {
            // Log an error or show a message, then return or throw to prevent further NPEs
            android.util.Log.e("SettingsActivity", "Error finding essential views in layout!")
            Toast.makeText(this, "Error loading settings layout.", Toast.LENGTH_LONG).show()
            finish() // Exit activity if layout is broken
            return
        }

        // Setup Listeners
        buttonBack?.setOnClickListener { finish() }
        buttonProfile.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
        buttonDeveloper.setOnClickListener { startActivity(Intent(this, AboutUsActivity::class.java)) }

        // Setup UI components only if views were found
        setup24HourToggle()
    }

    // Function to apply FULL theme based on SharedPreferences
    private fun applyAppTheme() {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // Read the saved theme *Resource ID*
        val themeResId = sharedPreferences.getInt(ProfileActivity.PREF_KEY_THEME_ID, ProfileActivity.THEME_DEFAULT_ID)
        setTheme(themeResId) // Apply the chosen FULL theme
    }

    private fun setup24HourToggle() {
        checkbox24Hour?.let { checkBox ->
            // Load initial state
            val use24Hour = sharedPreferences.getBoolean(PREF_KEY_24_HOUR, false) // Default to false (12-hour)
            checkBox.isChecked = use24Hour

            // Save state on change
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit().putBoolean(PREF_KEY_24_HOUR, isChecked).apply()
            }
        }
    }
}