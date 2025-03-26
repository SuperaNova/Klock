package cit.edu.KlockApp

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Button


class   ProfileActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val buttonLogin = findViewById<Button>(R.id.button_login)
        buttonLogin.setOnClickListener {
            AlertDialog.Builder(this)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle("Logging Out")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, which -> startActivity(
                    Intent(this, LandingActivity::class.java)
                ) })
                .setNegativeButton("No", null)
                .show()
        }

    }
}