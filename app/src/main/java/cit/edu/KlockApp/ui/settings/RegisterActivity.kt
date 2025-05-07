package cit.edu.KlockApp.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import cit.edu.KlockApp.databinding.ActivityRegisterBinding
import cit.edu.KlockApp.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import cit.edu.KlockApp.ui.main.KlockActivity
import android.content.Context

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.buttonRegister.setOnClickListener {
            registerUser()
        }

        binding.buttonBack.setOnClickListener {
            finish()
        }
    }

    private fun applyAppTheme() {
        sharedPreferences = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val themeResId = sharedPreferences.getInt(
            ProfileActivity.PREF_KEY_THEME_ID,
            ProfileActivity.THEME_DEFAULT_ID
        )
        setTheme(themeResId)
        Log.d("RegisterActivity", "Theme applied: $themeResId")
    }

    private fun registerUser() {
        val email = binding.email.text.toString().trim()
        val username = binding.username.text.toString().trim()
        val password = binding.password.text.toString().trim()
        val confirmPassword = binding.confirmPassword.text.toString().trim()

        binding.email.error = null
        binding.username.error = null
        binding.password.error = null
        binding.confirmPassword.error = null

        if (email.isEmpty()) {
            binding.email.error = "Email cannot be empty"
            binding.email.requestFocus()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.error = "Please enter a valid email address"
            binding.email.requestFocus()
            return
        }
        if (username.isEmpty()) {
            binding.username.error = "Username cannot be empty"
            binding.username.requestFocus()
            return
        }
        if (username.length < 3) {
            binding.username.error = "Username must be at least 3 characters long"
            binding.username.requestFocus()
            return
        }
        if (password.isEmpty()) {
            binding.password.error = "Password cannot be empty"
            binding.password.requestFocus()
            return
        }
        if (password.length < 6) {
            binding.password.error = "Password must be at least 6 characters long"
            binding.password.requestFocus()
            return
        }
        if (password != confirmPassword) {
            binding.confirmPassword.error = "Passwords do not match"
            binding.confirmPassword.requestFocus()
            return
        }

        binding.buttonRegister.isEnabled = false
        binding.progressBarRegister.visibility = View.VISIBLE

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBarRegister.visibility = View.GONE
                binding.buttonRegister.isEnabled = true

                if (task.isSuccessful) {
                    Log.d("RegisterActivity", "createUserWithEmail:SUCCESS")
                    val firebaseUser = auth.currentUser

                    if (firebaseUser == null) {
                        Log.e("RegisterActivity", "CRITICAL: firebaseUser is null even after successful account creation!")
                        showToast("Registration error. Please try again.")
                        return@addOnCompleteListener
                    }

                    Log.d("RegisterActivity", "Attempting to update Firebase Auth displayName for $username")
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()
                    firebaseUser.updateProfile(profileUpdates).addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            Log.d("RegisterActivity", "Firebase Auth displayName updated successfully for $username.")
                        } else {
                            Log.w("RegisterActivity", "Failed to update Firebase Auth displayName.", profileTask.exception)
                        }
                        // DisplayName update is complete (or failed), NOW try RTDB write and then navigate based on RTDB outcome

                        Log.d("RegisterActivity", "Proceeding to write username ('$username') to Realtime Database for UID: ${'$'}{firebaseUser.uid}.")
                        val uid = firebaseUser.uid
                        val database = Firebase.database(BuildConfig.DATABASE_URL)
                        val userRef = database.getReference("usernames").child(uid)

                        userRef.setValue(username)
                            .addOnSuccessListener {
                                Log.d("RegisterActivity", "RTDB_WRITE_SUCCESS: Username '$username' saved for UID: $uid. Navigating now...")
                                showToast("Registration successful. Welcome $username!")
                                // Navigate to KlockActivity then ProfileActivity
                                val klockIntent = Intent(this, KlockActivity::class.java)
                                klockIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                startActivity(klockIntent)

                                val profileIntent = Intent(this, ProfileActivity::class.java)
                                startActivity(profileIntent)
                                finishAffinity()
                            }
                            .addOnFailureListener { e ->
                                Log.e("RegisterActivity", "RTDB_WRITE_FAILURE: Failed to save username for UID: $uid. Error: ${'$'}{e.message}. Navigating anyway...", e)
                                showToast("Registration successful (DB username save error). Welcome $username!")
                                // Navigate to KlockActivity then ProfileActivity
                                val klockIntent = Intent(this, KlockActivity::class.java)
                                klockIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                startActivity(klockIntent)

                                val profileIntent = Intent(this, ProfileActivity::class.java)
                                startActivity(profileIntent)
                                finishAffinity()
                            }
                    }
                } else {
                    Log.w("RegisterActivity", "createUserWithEmail:FAILURE", task.exception)
                    var errorMessage = task.exception?.localizedMessage ?: "Unknown registration error. Please try again."
                    when (task.exception) {
                        is FirebaseAuthUserCollisionException -> {
                            errorMessage = "This email address is already in use."
                            binding.email.error = errorMessage
                            binding.email.requestFocus()
                        }
                        is FirebaseAuthWeakPasswordException -> {
                            errorMessage = "Password is too weak. Please choose a stronger one (at least 6 characters)."
                            binding.password.error = errorMessage
                            binding.password.requestFocus()
                        }
                    }
                    showToast(errorMessage)
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}