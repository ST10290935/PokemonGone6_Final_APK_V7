//Code Attribution:
//For the code below these are the sources I have used to improve my knowledge and implement features:
//Android Developers, 2025. Authenticate users with Sign in with Google. [online] Available at: <https://developer.android.com/identity/sign-in/credential-manager-siwg> [Accessed 29 September 2025].
//Firebase, 2025. Get Started with Firebase Authentication on Android. [online] Available at: <https://firebase.google.com/docs/auth/android/start> [Accessed 29 September 2025].
//CodingSTUFF, 2022. Google Sign In using Firebase in Kotlin (Android Studio 2022). [video online] Available at: <https://youtu.be/318sOlkJBQ?si=jrFTWSEeZvbKk-nS> [Accessed 25 September 2025].
//Codes Easy, 2022. Login and Registration using Firebase in Android. [video online] Available at: <https://youtu.be/QAKq8UBv4GI?si=8ZBHqVJt5GyqiwNY> [Accessed 25 September 2025].
//Coding World, 2025. How To Generate SHA1 Key In Android Studio [2024] | 100% Working Method. [video online] Available at: <https://youtu.be/ClLZTrvsUSk?si=jiXRUUooIEsmJMiz> [Accessed 27 September 2025].


package com.example.pokemon

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pokemon.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // this configures Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // this is for the email login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Login failed: ${it.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        // this is for registering
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }


        binding.btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
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
    //this generates the token for firebases SSO login
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d("GOOGLE_SIGN_IN", "signInWithCredential:success -> ${user?.email}")
                    Toast.makeText(this, "Welcome ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Log.w("GOOGLE_SIGN_IN", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Firebase authentication failed.", Toast.LENGTH_LONG).show()
                }
            }
    }
}
