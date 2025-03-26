package cit.edu.KlockApp.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import cit.edu.KlockApp.AboutUsActivity
import cit.edu.KlockApp.LoginActivity
import cit.edu.KlockApp.ProfileActivity
import cit.edu.KlockApp.R

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val buttonBack = findViewById<ImageButton>(R.id.back_button)
        buttonBack.setOnClickListener {
            finish()
        }

        // TODO: add local storage for account information (login is least priority)
        val buttonProfile = findViewById<Button>(R.id.profile_view)
        buttonProfile.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        val buttonDeveloper = findViewById<Button>(R.id.about_us)
        buttonDeveloper.setOnClickListener {
            startActivity(Intent(this, AboutUsActivity::class.java))
        }

    }
}