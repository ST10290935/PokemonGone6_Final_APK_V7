//Code Attribution
// For the below code I have used these sources to guide me:
//Atif Perviaz, 2020. Biometric Authentication | Android Studio | Kotlin. [video online] Available at: < https://youtu.be/n5TRI1RB1Mc?si=e9y6897l129HrnVS> [Accessed 12 November 2025].
//Mullatoez, 2020. Android Fingerprint Authentication in Kotlin | Biometric Authentication | Android Studio Tutorial. [video online] Available at: < https://youtu.be/sU9p6Pt6I2k?si=9lrt51KQb9aQY2v_> [Accessed 12 November 2025].
//Phillip Lackner, 2024. How to Implement Biometric Auth in Your Android App. [video online] Available at: < https://youtu.be/_dCRQ9wta-I?si=KRuKHRS_UqQLD9vD> [Accessed 12 November 2025].

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
