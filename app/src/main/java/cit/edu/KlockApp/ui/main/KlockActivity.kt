package cit.edu.KlockApp.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.ActivityKlockBinding
import cit.edu.KlockApp.ui.main.alarm.AlarmFragment
import cit.edu.KlockApp.ui.main.worldClock.WorldClockFragment
import cit.edu.KlockApp.ui.settings.SettingsActivity

class KlockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKlockBinding
    private var shouldShowSettings: Boolean = true
    private var shouldShowAddButton: Boolean = true // Flag for Add button visibility

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            // Determine button visibility based on destination
            shouldShowSettings = destination.id == R.id.navigation_worldClock
            shouldShowAddButton = destination.id == R.id.navigation_worldClock || destination.id == R.id.navigation_alarm
            
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
                    is AlarmFragment -> currentFragment.showAddAlarmDialog()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_settings)?.isVisible = shouldShowSettings
        menu?.findItem(R.id.action_add)?.isVisible = shouldShowAddButton // Set Add button visibility
        return super.onPrepareOptionsMenu(menu)
    }
}
