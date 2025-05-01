package cit.edu.KlockApp.ui.main

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.ActivityKlockBinding
import cit.edu.KlockApp.ui.main.worldClock.WorldClockFragment
import cit.edu.KlockApp.ui.main.alarm.AlarmAddActivity
import cit.edu.KlockApp.ui.main.alarm.AlarmFragment
import cit.edu.KlockApp.ui.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class KlockActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val FIRST_RUN_KEY = "first_run_done"
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }

    private lateinit var binding: ActivityKlockBinding
    private var shouldShowSettings: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestFirstRunPermissions()

        binding = ActivityKlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_klock)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_worldClock,
                R.id.navigation_alarm,
                R.id.navigation_stopwatch,
                R.id.navigation_timer
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            supportActionBar?.title = when (destination.id) {
                R.id.navigation_worldClock -> "World Clock"
                R.id.navigation_alarm -> "Alarm"
                R.id.navigation_stopwatch -> "Stopwatch"
                R.id.navigation_timer -> "Timer"
                else -> getString(R.string.app_name)
            }

            shouldShowSettings = destination.id == R.id.navigation_worldClock // used in world clock
            invalidateOptionsMenu() // Refresh menu visibility
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_add -> {
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_klock)
                val currentFragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment

                when (currentFragment) {
                    is WorldClockFragment -> currentFragment.showTimeZoneSelectionDialog()
                    is AlarmFragment -> currentFragment.launchAddAlarm()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_settings)?.isVisible = shouldShowSettings
        return super.onPrepareOptionsMenu(menu)
    }
    private fun requestFirstRunPermissions() {
        val prefs: SharedPreferences =
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(FIRST_RUN_KEY, false)) {
            // Ask for notification permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_NOTIFICATION_PERMISSION
                    )
                }
            }

            // Ask user to allow exact alarms on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager =
                    getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            }

            // Remember we asked already
            prefs.edit().putBoolean(FIRST_RUN_KEY, true).apply()
        }
    }

}
