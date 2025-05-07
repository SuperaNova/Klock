package cit.edu.KlockApp.ui.settings

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import cit.edu.KlockApp.databinding.ActivityLoginBinding
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme BEFORE super.onCreate()
        // applyAppTheme() // REMOVED
        applyAppTheme() // Restored and will be implemented

        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        val buttonLogin = binding.buttonLogin
        buttonLogin.setOnClickListener {
            loginUser()
        }
        val buttonSignup = binding.buttonSignup
        buttonSignup.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        val buttonBack = binding.buttonBack
        buttonBack.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is already signed in, navigate to main activity
//            showToast("Welcome back, ${'$'}{currentUser.email}!")
            startActivity(Intent(this, ProfileActivity::class.java))
            finishAffinity() // Finish all auth-related activities
        }
    }

    private fun applyAppTheme() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themeResId = sharedPreferences.getInt(
            ProfileActivity.PREF_KEY_THEME_ID,
            ProfileActivity.THEME_DEFAULT_ID
        )
        setTheme(themeResId)
    }

    private fun loginUser() {
        // Assuming your EditText for username is actually for email for Firebase Auth
        val email = binding.username.text.toString().trim()
        val password = binding.password.text.toString().trim()

        if (email.isEmpty()) {
            showToast("Email cannot be empty")
            return
        }
        if (password.isEmpty()) {
            showToast("Password cannot be empty")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("LoginActivity", "signInWithEmail:success")
                    val user = auth.currentUser
//                    showToast("Login successful. Welcome ${'$'}{user?.email}!")
                    // Navigate to KlockActivity or ProfileActivity
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finishAffinity() // Finish all auth-related activities
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                    showToast("Authentication failed: ${'$'}{task.exception?.message}")
                }
            }
    }

    // Utility function to show a toast
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
