package cit.edu.KlockApp

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar

class AboutUsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the selected theme
        applyAppTheme()
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        // Setup Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_about)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Set App Version
        try {
            val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
            val version = pInfo.versionName
            findViewById<TextView>(R.id.text_app_version).text = "Version $version"
        } catch (e: PackageManager.NameNotFoundException) {
            // Handle exception - perhaps hide the version text or show default
            findViewById<TextView>(R.id.text_app_version).text = "Version N/A"
            e.printStackTrace()
        }

        // Setup Link Buttons (REMOVED)
        /*
        findViewById<Button>(R.id.button_privacy).setOnClickListener {
            openUrl("https://example.com/privacy") // Replace with actual URL
        }

        findViewById<Button>(R.id.button_licenses).setOnClickListener {
            openUrl("https://example.com/licenses") // Replace with actual URL or library
        }
        */
    }

    // Function to apply the currently selected theme
    private fun applyAppTheme() {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themeResId = sharedPreferences.getInt(ProfileActivity.PREF_KEY_THEME_ID, ProfileActivity.THEME_DEFAULT_ID)
        setTheme(themeResId)
    }

    // Handle Toolbar back button press
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}