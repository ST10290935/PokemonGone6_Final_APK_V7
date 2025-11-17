package com.example.pokemon.biometric


import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class BiometricAuthHelper(private val activity: AppCompatActivity) {
    private val executor: Executor = ContextCompat.getMainExecutor(activity)

    /**
     * Authenticate using device biometric (fingerprint)
     * callback(success, errorMessage)
     */
    fun authenticate(
        title: String = "Biometric login",
        subtitle: String = "Use your fingerprint to sign in",
        callback: (Boolean, String?) -> Unit
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use password")
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                callback(true, null)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                callback(false, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                callback(false, "Authentication failed")
            }
        })

        biometricPrompt.authenticate(promptInfo)
    }
}
