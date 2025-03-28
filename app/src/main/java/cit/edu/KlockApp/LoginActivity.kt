package cit.edu.KlockApp

import android.app.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast

class LoginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val buttonLogin = findViewById<Button>(R.id.button_login)
        buttonLogin.setOnClickListener {
            validateInput();
        }
        val buttonSignup = findViewById<Button>(R.id.button_signup)
        buttonSignup.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        val buttonBack = findViewById<ImageButton>(R.id.button_back)
        buttonBack.setOnClickListener {
            finish()
        }

    }

    private fun validateInput() {
        val username = findViewById<EditText>(R.id.username).text.toString().trim()
        val password = findViewById<EditText>(R.id.password).text.toString().trim()

        // Username validation
        when {
            username.isEmpty() -> {
                showToast("Username cannot be empty")
            }
            username.length < 4 -> {
                showToast("Username must be at least 4 characters long")
            }
            username.length > 20 -> {
                showToast("Username must not exceed 20 characters")
            }
            // !username.matches(Regex("^[a-zA-Z0-9_.-]+$")) -> {
            //     showToast("Username can only contain letters, numbers, '_', '-', and '.'")
            // }

            // Password validation
            password.isEmpty() -> {
                showToast("Password cannot be empty")
            }
            password.length < 8 -> {
                showToast("Password must be at least 8 characters long")
            }
            // !password.matches(Regex(".*[A-Z].*")) -> {
            //     showToast("Password must contain at least one uppercase letter")
            // }
            // !password.matches(Regex(".*[a-z].*")) -> {
            //     showToast("Password must contain at least one lowercase letter")
            // }
            // !password.matches(Regex(".*\\d.*")) -> {
            //     showToast("Password must contain at least one number")
            // }
            // !password.matches(Regex(".*[!@#\$%^&*()-+=].*")) -> {
            //     showToast("Password must contain at least one special character (!@#\$%^&*()-+=)")
            // }

            // If all checks pass
            else -> {
                startActivity(Intent(this, ProfileActivity::class.java))
            }
        }
    }

    // Utility function to show a toast
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
