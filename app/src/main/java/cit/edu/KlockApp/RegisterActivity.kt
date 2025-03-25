package cit.edu.KlockApp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton

class RegisterActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val buttonRegister = findViewById<Button>(R.id.button_register)
        buttonRegister.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        val buttonBack = findViewById<ImageButton>(R.id.button_back)
        buttonBack.setOnClickListener {
            finish()
        }
    }
}