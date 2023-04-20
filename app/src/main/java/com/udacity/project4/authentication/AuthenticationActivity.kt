package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
// Done
class AuthenticationActivity : AppCompatActivity() {

    private val loginLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onLoginResult(res)
    }

    private lateinit var binding: ActivityAuthenticationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { openLoginScreen() }

    }

    private fun onLoginResult(task: FirebaseAuthUIAuthenticationResult) {
        val response = task.idpResponse
        // Logged in successfully
        if (task.resultCode == RESULT_OK) {
            startActivity(Intent(this@AuthenticationActivity, RemindersActivity::class.java))
            finish()
        // Login failed
        } else {
            Log.e("rabbit", "onLoginResult: ${response?.error}")
        }
    }

    private fun openLoginScreen() {

        val sources = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        val loginIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(sources)
            .build()

        loginLauncher.launch(loginIntent)
    }
}
