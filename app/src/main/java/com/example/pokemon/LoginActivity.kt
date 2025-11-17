//Code Attribution:
//For the code below these are the sources I have used to improve my knowledge and implement features:
//Android Developers, 2025. Authenticate users with Sign in with Google. [online] Available at: <https://developer.android.com/identity/sign-in/credential-manager-siwg> [Accessed 29 September 2025].
//Firebase, 2025. Get Started with Firebase Authentication on Android. [online] Available at: <https://firebase.google.com/docs/auth/android/start> [Accessed 29 September 2025].
//CodingSTUFF, 2022. Google Sign In using Firebase in Kotlin (Android Studio 2022). [video online] Available at: <https://youtu.be/318sOlkJBQ?si=jrFTWSEeZvbKk-nS> [Accessed 25 September 2025].
//Codes Easy, 2022. Login and Registration using Firebase in Android. [video online] Available at: <https://youtu.be/QAKq8UBv4GI?si=8ZBHqVJt5GyqiwNY> [Accessed 25 September 2025].
//Coding World, 2025. How To Generate SHA1 Key In Android Studio [2024] | 100% Working Method. [video online] Available at: <https://youtu.be/ClLZTrvsUSk?si=jiXRUUooIEsmJMiz> [Accessed 27 September 2025].
//Atif Perviaz, 2020. Biometric Authentication | Android Studio | Kotlin. [video online] Available at: < https://youtu.be/n5TRI1RB1Mc?si=e9y6897l129HrnVS> [Accessed 12 November 2025].
//Mullatoez, 2020. Android Fingerprint Authentication in Kotlin | Biometric Authentication | Android Studio Tutorial. [video online] Available at: < https://youtu.be/sU9p6Pt6I2k?si=9lrt51KQb9aQY2v_> [Accessed 12 November 2025].
//Phillip Lackner, 2024. How to Implement Biometric Auth in Your Android App. [video online] Available at: < https://youtu.be/_dCRQ9wta-I?si=KRuKHRS_UqQLD9vD> [Accessed 12 November 2025].



package com.example.pokemon

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.lifecycleScope
import com.example.pokemon.biometric.BiometricAuthHelper
import com.example.pokemon.data.SettingsRepository
import com.example.pokemon.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        //  Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //  Regular email login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            //  Save user session after successful login
                            val userEmail = auth.currentUser?.email ?: email
                            saveUserSession(email, password) // Save both email and password

                            Toast.makeText(this, getString(R.string.email_login_success), Toast.LENGTH_SHORT).show()

                            //Download settings from Firebase on first login
                            lifecycleScope.launch {
                                if (isOnline()) {
                                    SettingsRepository.downloadFromFirebase(this@LoginActivity)
                                }
                            }

                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                getString(R.string.login_failed_error, it.exception?.message ?: getString(R.string.unknown_error)),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, getString(R.string.enter_login_detail_toast), Toast.LENGTH_SHORT).show()
            }
        }


        //  Register redirect
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        //  Google Sign-In
        binding.btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        //  Fingerprint Biometric Authentication
        binding.btnBiometric.setOnClickListener {
            val helper = BiometricAuthHelper(this)
            helper.authenticate(
                title = "Biometric Login",
                subtitle = "Use your fingerprint to sign in"
            ) { success, error ->
                if (success) {
                    //  Retrieve the saved session
                    val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
                    val savedEmail = prefs.getString("email", null)
                    val savedPassword = prefs.getString("password", null)
                    val loginType = prefs.getString("login_type", "email")

                    if (savedEmail != null) {
                        when (loginType) {
                            "email" -> {
                                // Re-authenticate with email/password
                                if (savedPassword != null) {
                                    auth.signInWithEmailAndPassword(savedEmail, savedPassword)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                runOnUiThread {
                                                    Toast.makeText(this, getString(R.string.welcome_back), Toast.LENGTH_SHORT).show()

                                                    // Download settings from Firebase on first login
                                                    lifecycleScope.launch {
                                                        if (isOnline()) {
                                                            SettingsRepository.downloadFromFirebase(this@LoginActivity)
                                                        }
                                                    }

                                                    startActivity(Intent(this, MainActivity::class.java))
                                                    finish()
                                                }
                                            } else {
                                                runOnUiThread {
                                                    Toast.makeText(this, getString(R.string.session_expire_toast), Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                } else {
                                    Toast.makeText(this, getString(R.string.no_saved_password_toast), Toast.LENGTH_LONG).show()
                                }
                            }
                            "google" -> {
                                // For Google sign-in, just redirect to Google sign-in
                                runOnUiThread {
                                    Toast.makeText(this, getString(R.string.use_gooogle_toast), Toast.LENGTH_SHORT).show()
                                    binding.btnGoogleSignIn.performClick()
                                }
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                getString(R.string.no_saved_session_toast),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Biometric failed: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }



    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("GOOGLE_SIGN_IN", "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("GOOGLE_SIGN_IN", "Google sign in failed", e)
                Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    //  Save the user session after successful Google Sign-In
                    val userEmail = user?.email ?: ""
                    saveUserSession(userEmail, password = null, loginType = "google")


                    //  Download settings from Firebase on first login
                    lifecycleScope.launch {
                        if (isOnline()) {
                            SettingsRepository.downloadFromFirebase(this@LoginActivity)
                        }
                    }

                    Log.d("GOOGLE_SIGN_IN", "signInWithCredential:success -> ${user?.email}")
                    Toast.makeText(this, getString(R.string.welcome_user, user?.displayName ?: ""), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Log.w("GOOGLE_SIGN_IN", "signInWithCredential:failure", task.exception)
                    Toast.makeText(
                        this,
                        "Firebase authentication failed.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }


    private fun saveUserSession(email: String, password: String? = null, loginType: String = "email") {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        prefs.edit().apply {
            putString("email", email)
            putString("password", password) // Will be null for Google sign-in
            putString("login_type", loginType) // "email" or "google"
            apply()
        }
    }

}
