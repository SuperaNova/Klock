package cit.edu.KlockApp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import cit.edu.KlockApp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme BEFORE super.onCreate()
        // applyAppTheme() // REMOVED

        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val buttonLogin = binding.buttonLogin
        buttonLogin.setOnClickListener {
            validateInput()
        }
        val buttonSignup = binding.buttonSignup
        buttonSignup.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        val buttonBack = binding.buttonBack
        buttonBack.setOnClickListener {
            finish()
        }

    }

    private fun validateInput() {
        val username = binding.username.text.toString().trim()
        val password = binding.password.text.toString().trim()

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
