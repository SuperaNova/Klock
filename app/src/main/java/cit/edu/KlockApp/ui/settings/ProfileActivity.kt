package cit.edu.KlockApp.ui.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import cit.edu.KlockApp.BuildConfig
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.time.LocalTime

// SharedPreferences Constants (ideally in a separate file or respective ViewModels)
private const val WORLD_CLOCKS_PREFS_NAME = "world_clocks"
private const val WORLD_CLOCKS_KEY = "saved_clocks_ordered"

private const val TIMER_PREFS_NAME = "timer_prefs"
private const val TIMER_PRESETS_KEY = "timer_presets_json"
private const val TIMER_SOUND_PREFS_NAME = "timer_sound_prefs"
private const val TIMER_SOUND_URI_KEY = "pref_timer_sound_uri"

private const val ALARMS_PREFS_NAME = "alarms_prefs"
private const val ALARMS_KEY = "alarms_key"

// Data classes for Firebase
@Serializable
data class FirebaseTimerPreset(
    val id: String = "",
    val emojiIcon: String = "",
    val durationMillis: Long = 0L
)

@Serializable
data class FirebaseTimerSettings(
    val presets: List<FirebaseTimerPreset>? = null,
    val sound_uri: String? = null
)

@Serializable
data class FirebaseAlarm(
    val id: Int = 0,
    val label: String = "",
    val time_hour: Int = 0,
    val time_minute: Int = 0,
    val repeatDays: List<String>? = null,
    var isEnabled: Boolean = false,
    var snoozeMinutes: Int = 0,
    var vibrateOnAlarm: Boolean = false,
    var alarmSound: String? = null
)

@Serializable
data class FirebaseUserSettings(
    val world_clocks: List<String>? = null,
    val timer_settings: FirebaseTimerSettings? = null,
    val alarms: List<FirebaseAlarm>? = null
)

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var database: FirebaseDatabase
    private lateinit var userSettingsRef: DatabaseReference

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        serializersModule = SerializersModule {
            // Add contextual serializers if needed for specific types not directly supported by Firebase
            // For LocalTime in original Alarm data, we handle it during conversion
        }
    }

    companion object {
        const val PREF_KEY_THEME_ID = "theme_resource_id"
        val THEME_DEFAULT_ID = R.style.Theme_Klock_Default // Default theme
        val THEME_OXFORD_ID = R.style.Theme_Klock_Oxford // Oxford theme
        // Removed PREF_KEY_NIGHT_MODE as DayNight theme handles it
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme() // Apply theme before super.onCreate()
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        currentUser = firebaseAuth.currentUser
        database = FirebaseDatabase.getInstance(BuildConfig.DATABASE_URL)

        if (currentUser == null) {
            // Redirect to LoginActivity if no user is logged in
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        userSettingsRef = database.reference.child("storage").child(currentUser!!.uid)

        setupToolbar()
        loadUserProfile()
        setupThemeRadioButtons()
        setupLogoutButton()
        setupSyncButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarProfile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarProfile.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun loadUserProfile() {
        currentUser?.let {
            binding.textProfileName.text = "Hi, ${it.email ?: "User"}!"
            // Load profile picture if available - not implemented here
        }
    }

    private fun setupThemeRadioButtons() {
        val sharedPreferences = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val currentThemeId = sharedPreferences.getInt(PREF_KEY_THEME_ID, THEME_DEFAULT_ID)

        when (currentThemeId) {
            THEME_DEFAULT_ID -> binding.radioGroupThemePalette.check(R.id.radio_theme_default)
            THEME_OXFORD_ID -> binding.radioGroupThemePalette.check(R.id.radio_theme_oxford)
        }

        binding.radioGroupThemePalette.setOnCheckedChangeListener { group, checkedId ->
            val selectedThemeId = when (checkedId) {
                R.id.radio_theme_default -> THEME_DEFAULT_ID
                R.id.radio_theme_oxford -> THEME_OXFORD_ID
                else -> THEME_DEFAULT_ID
            }
            saveThemePreference(selectedThemeId)
            applyAndRecreate()
        }
    }

    private fun saveThemePreference(themeResId: Int) {
        val sharedPreferences = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(PREF_KEY_THEME_ID, themeResId).apply()
        Log.d("ProfileActivity", "Theme saved: $themeResId")
    }

    private fun applyAppTheme() {
        val sharedPreferences = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val themeResId = sharedPreferences.getInt(PREF_KEY_THEME_ID, THEME_DEFAULT_ID)
        setTheme(themeResId)
        Log.d("ProfileActivity", "Theme applied: $themeResId")

        // Apply DayNight mode based on system/user preference if applicable
        // This part is now handled by Theme.MaterialComponents.DayNight parent
    }

    private fun applyAndRecreate() {
        // Set a flag or use some mechanism if you want to show a specific tab after recreation
        recreate()
    }

    private fun setupLogoutButton() {
        binding.buttonLogout.setOnClickListener {
            firebaseAuth.signOut()
            // Clear any local user-specific preferences if necessary
            val themePrefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            themePrefs.edit().remove(PREF_KEY_THEME_ID).apply() // Example: reset theme choice on logout

            // Optionally clear other SharedPreferences related to user data if they shouldn't persist after logout
            // clearUserSharedPreferences(WORLD_CLOCKS_PREFS_NAME)
            // clearUserSharedPreferences(TIMER_PREFS_NAME)
            // clearUserSharedPreferences(TIMER_SOUND_PREFS_NAME)
            // clearUserSharedPreferences(ALARMS_PREFS_NAME)

            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun clearUserSharedPreferences(prefsName: String) {
        getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun setupSyncButtons() {
        binding.buttonBackupSettings.setOnClickListener {
            showBackupConfirmationDialog()
        }
        binding.buttonRestoreSettings.setOnClickListener {
            showRestoreConfirmationDialog()
        }
    }

    private fun showBackupConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Backup Settings")
            .setMessage("Are you sure you want to back up your current settings to the cloud?")
            .setPositiveButton("Backup") { _, _ ->
                backupSettingsToFirebase()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun backupSettingsToFirebase() {
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to backup settings.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBarSync.visibility = View.VISIBLE
        binding.buttonBackupSettings.isEnabled = false
        binding.buttonRestoreSettings.isEnabled = false

        val worldClocksPrefs = getSharedPreferences(WORLD_CLOCKS_PREFS_NAME, Context.MODE_PRIVATE)
        val timerPrefs = getSharedPreferences(TIMER_PREFS_NAME, Context.MODE_PRIVATE)
        val timerSoundPrefs = getSharedPreferences(TIMER_SOUND_PREFS_NAME, Context.MODE_PRIVATE)
        val alarmsPrefs = getSharedPreferences(ALARMS_PREFS_NAME, Context.MODE_PRIVATE)

        val worldClocksJsonString = worldClocksPrefs.getString(WORLD_CLOCKS_KEY, null)
        val worldClocksList = worldClocksJsonString?.let { json.decodeFromString<List<String>>(it) }

        val timerPresetsJsonString = timerPrefs.getString(TIMER_PRESETS_KEY, null)
        val timerPresetsList = timerPresetsJsonString?.let { json.decodeFromString<List<FirebaseTimerPreset>>(it) }
        val timerSoundUri = timerSoundPrefs.getString(TIMER_SOUND_URI_KEY, null)
        val firebaseTimerSettings = FirebaseTimerSettings(timerPresetsList, timerSoundUri)

        val alarmsJsonString = alarmsPrefs.getString(ALARMS_KEY, null)
        val originalAlarmsList = alarmsJsonString?.let { Json {ignoreUnknownKeys = true; serializersModule = SerializersModule { contextual(cit.edu.KlockApp.ui.main.alarm.LocalTimeSerializer) }}.decodeFromString<List<cit.edu.KlockApp.ui.main.alarm.Alarm>>(it) }
        val firebaseAlarmsList = originalAlarmsList?.map {
            FirebaseAlarm(
                id = it.id,
                label = it.label,
                time_hour = it.time.hour,
                time_minute = it.time.minute,
                repeatDays = it.repeatDays,
                isEnabled = it.isEnabled,
                snoozeMinutes = it.snoozeMinutes,
                vibrateOnAlarm = it.vibrateOnAlarm,
                alarmSound = it.alarmSound
            )
        }

        val userSettings = FirebaseUserSettings(
            world_clocks = worldClocksList,
            timer_settings = firebaseTimerSettings,
            alarms = firebaseAlarmsList
        )

        userSettingsRef.setValue(userSettings)
            .addOnSuccessListener {
                binding.progressBarSync.visibility = View.GONE
                binding.buttonBackupSettings.isEnabled = true
                binding.buttonRestoreSettings.isEnabled = true
                Toast.makeText(this, "Settings backed up successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                binding.progressBarSync.visibility = View.GONE
                binding.buttonBackupSettings.isEnabled = true
                binding.buttonRestoreSettings.isEnabled = true
                Toast.makeText(this, "Backup failed: ${it.message}", Toast.LENGTH_LONG).show()
                Log.e("ProfileActivity", "Backup failed", it)
            }
    }

    private fun showRestoreConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Restore Settings")
            .setMessage("This will overwrite your current local settings with the ones from the cloud. Are you sure?")
            .setPositiveButton("Restore") { _, _ ->
                restoreSettingsFromFirebase()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun restoreSettingsFromFirebase() {
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to restore settings.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBarSync.visibility = View.VISIBLE
        binding.buttonBackupSettings.isEnabled = false
        binding.buttonRestoreSettings.isEnabled = false

        userSettingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBarSync.visibility = View.GONE
                binding.buttonBackupSettings.isEnabled = true
                binding.buttonRestoreSettings.isEnabled = true

                if (!snapshot.exists()) {
                    Toast.makeText(applicationContext, "No backup found in the cloud.", Toast.LENGTH_SHORT).show()
                    return
                }
                val firebaseSettings = snapshot.getValue(FirebaseUserSettings::class.java)

                if (firebaseSettings == null) {
                    Toast.makeText(applicationContext, "Failed to parse backup data.", Toast.LENGTH_SHORT).show()
                    return
                }

                firebaseSettings.world_clocks?.let {
                    val worldClocksJson = json.encodeToString(it)
                    getSharedPreferences(WORLD_CLOCKS_PREFS_NAME, Context.MODE_PRIVATE).edit().putString(WORLD_CLOCKS_KEY, worldClocksJson).apply()
                }

                firebaseSettings.timer_settings?.let { ts ->
                    ts.presets?.let {
                        val timerPresetsJson = json.encodeToString(it)
                        getSharedPreferences(TIMER_PREFS_NAME, Context.MODE_PRIVATE).edit().putString(TIMER_PRESETS_KEY, timerPresetsJson).apply()
                    }
                    getSharedPreferences(TIMER_SOUND_PREFS_NAME, Context.MODE_PRIVATE).edit().putString(TIMER_SOUND_URI_KEY, ts.sound_uri).apply()
                }

                firebaseSettings.alarms?.let { faList ->
                    val originalAlarmsList = faList.map {
                        cit.edu.KlockApp.ui.main.alarm.Alarm(
                            id = it.id,
                            label = it.label,
                            time = LocalTime.of(it.time_hour, it.time_minute),
                            repeatDays = it.repeatDays ?: emptyList(),
                            isEnabled = it.isEnabled,
                            snoozeMinutes = it.snoozeMinutes,
                            vibrateOnAlarm = it.vibrateOnAlarm,
                            alarmSound = it.alarmSound ?: "",
                            isExpanded = false
                        )
                    }
                    val alarmsJson = Json {serializersModule = SerializersModule { contextual(cit.edu.KlockApp.ui.main.alarm.LocalTimeSerializer) }}.encodeToString(originalAlarmsList)
                    getSharedPreferences(ALARMS_PREFS_NAME, Context.MODE_PRIVATE).edit().putString(ALARMS_KEY, alarmsJson).apply()
                }

                Toast.makeText(applicationContext, "Settings restored! Please restart the app for changes to take full effect.", Toast.LENGTH_LONG).show()
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBarSync.visibility = View.GONE
                binding.buttonBackupSettings.isEnabled = true
                binding.buttonRestoreSettings.isEnabled = true
                Toast.makeText(applicationContext, "Failed to restore settings: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("ProfileActivity", "Firebase restore cancelled", error.toException())
            }
        })
    }
}