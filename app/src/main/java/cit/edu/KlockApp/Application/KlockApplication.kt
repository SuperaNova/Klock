package cit.edu.KlockApp.Application

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

class KlockApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // REMOVE registration
        // registerActivityLifecycleCallbacks(this)

        // Apply DayNight mode (leave this separate - might still be useful)
        applyDayNightMode()
    }

    // Keep applyDayNightMode as it was (optional, could be removed if themes handle it)
    private fun applyDayNightMode() {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val currentDayNightMode = sharedPreferences.getInt(
            "app_daynight_mode", // Using a placeholder key - adjust if needed
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        AppCompatDelegate.setDefaultNightMode(currentDayNightMode)
    }

    // REMOVE applyColorThemeOverlay function
    /*
    private fun applyColorThemeOverlay(activity: Activity) {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themeKey = sharedPreferences.getString(ProfileActivity.PREF_KEY_COLOR_THEME, ProfileActivity.THEME_OVERLAY_DEFAULT)

        val themeResId = when (themeKey) {
            ProfileActivity.THEME_OVERLAY_OXFORD -> R.style.ThemeOverlay_App_Oxford
            else -> 0
        }

        if (themeResId != 0) {
            activity.setTheme(themeResId)
        }
    }
    */

    // REMOVE ActivityLifecycleCallbacks Methods
    /*
    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
    */

} 