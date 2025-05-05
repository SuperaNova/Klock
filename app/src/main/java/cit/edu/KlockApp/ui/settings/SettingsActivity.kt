package cit.edu.KlockApp.ui.settings

import android.app.Activity
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
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    // Define preference key constant
    companion object {
        const val PREF_KEY_24_HOUR = "use_24_hour_format"
    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var switch24Hour: MaterialSwitch
    private lateinit var themeLayout: android.view.View
    private lateinit var themeCurrentValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Find Views
        val buttonBack = findViewById<ImageButton>(R.id.back_button)
        switch24Hour = findViewById(R.id.switch_24_hour)
        themeLayout = findViewById(R.id.setting_theme)
        themeCurrentValue = findViewById(R.id.theme_current_value)
        val buttonProfile = findViewById<Button>(R.id.profile_view)
        val buttonDeveloper = findViewById<Button>(R.id.about_us)

        // Setup Listeners
        buttonBack.setOnClickListener { finish() }
        buttonProfile.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
        buttonDeveloper.setOnClickListener { startActivity(Intent(this, AboutUsActivity::class.java)) }

        setup24HourSwitch()
        setupThemeSelector()
    }

    private fun setup24HourSwitch() {
        // Load initial state
        val use24Hour = sharedPreferences.getBoolean(PREF_KEY_24_HOUR, false) // Default to false (12-hour)
        switch24Hour.isChecked = use24Hour

        // Save state on change
        switch24Hour.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(PREF_KEY_24_HOUR, isChecked).apply()
            // TODO: Optionally broadcast change or notify relevant components if needed immediately
        }
    }

    private fun setupThemeSelector() {
        // TODO: Load and display current theme setting
        themeCurrentValue.text = "System default" // Placeholder

        themeLayout.setOnClickListener {
            // TODO: Show theme selection dialog
            android.widget.Toast.makeText(this, "Theme selection clicked (TODO)", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}