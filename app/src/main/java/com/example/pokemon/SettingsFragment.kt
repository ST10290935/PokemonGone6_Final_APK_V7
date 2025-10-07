//Code Attribution:
//For the code below these are the sources I have used to improve my knowledge and implement features:
//GeeksForGeeks, 2025. How to implement preferences settings screen in Android?. [online] Available at: <https://www.geeksforgeeks.org/android/how-to-implement-preferences-settings-screen-in-android/> [Accessed 29 September 2025].




package com.example.pokemon

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.Preference
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Add Logout preference listener
        val logoutPref = findPreference<Preference>("pref_logout")
        logoutPref?.setOnPreferenceClickListener {
            performLogout()  // shows confirmation dialog
            true
        }
    }

    private fun performLogout() {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                // Cleanup MainActivity map and overlays safely if present
                val mainActivity = activity
                if (mainActivity is MainActivity) {
                    mainActivity.cleanupOnLogout()
                }

                // Configure GoogleSignInClient for logout
                val googleSignInClient = GoogleSignIn.getClient(
                    requireContext(),
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                )

                // Sign out from Google first, then Firebase
                googleSignInClient.signOut().addOnCompleteListener {
                    // Firebase sign out
                    FirebaseAuth.getInstance().signOut()

                    // Go back to LoginActivity
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity?.finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}
