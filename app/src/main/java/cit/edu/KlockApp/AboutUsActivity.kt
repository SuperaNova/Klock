package cit.edu.KlockApp

import android.app.Activity
import android.os.Bundle
import android.widget.ImageButton
import cit.edu.KlockApp.R

class AboutUsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        val buttonBack = findViewById<ImageButton>(R.id.back_button)
        buttonBack.setOnClickListener {
            finish()
        }
    }
}