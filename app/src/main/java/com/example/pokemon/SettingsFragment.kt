package com.example.pokemon

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.pokemon.data.SettingsRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.example.pokemon.data.AppDatabase
import kotlinx.coroutines.launch
import java.util.Locale

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        Log.e(" TEST", "SettingsFragment onCreatePreferences called")


        // Load settings from Room when fragment opens
        lifecycleScope.launch {
            Log.e(" SETTINGS", " Loading local settings ")
            SettingsRepository.loadLocalSettings(requireContext())
        }

        // Listen for preference changes > save to Room > sync if online
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener { prefs, key ->
            Log.e(" SETTINGS", " Preference changed: $key ")
            lifecycleScope.launch {
                when (key) {
                    "pref_avatar_name" -> {
                        val value = prefs.getString(key, "You")
                        Log.e(" SETTINGS", "Saving avatar name: $value")

                        SettingsRepository.saveSettings(requireContext(), avatarName = value)
                        trySync()
                    }
                    "pref_avatar_size" -> {
                        val value = prefs.getString(key, "medium")
                        SettingsRepository.saveSettings(requireContext(), avatarSize = value)
                        trySync()
                    }
                    "pref_map_theme" -> {
                        val value = prefs.getString(key, "default")
                        SettingsRepository.saveSettings(requireContext(), mapTheme = value)
                        trySync()
                    }
                    "pref_map_rotation" -> {
                        val value = prefs.getBoolean(key, true)
                        SettingsRepository.saveSettings(requireContext(), mapRotation = value)
                        trySync()
                    }
                }

                // Sync immediately after each save
                if (isOnline()) {
                    Log.e(" SETTINGS", "Syncing to Firebase...")
                    val success = SettingsRepository.syncToFirebase(requireContext())
                    Log.e(" SETTINGS", "Sync result: $success")
                }
            }
        }

        // Handle logout click
        findPreference<Preference>("pref_logout")?.setOnPreferenceClickListener {
            performLogout()
            true
        }

        //  Handle language selection
        findPreference<Preference>("pref_language")?.setOnPreferenceClickListener {
            showLanguageDialog()
            true
        }
    }


    // Sync Helpers


    private suspend fun trySync() {
        if (isOnline()) {
            SettingsRepository.syncToFirebase(requireContext())
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    // Logout Logic


    private fun performLogout() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.logout_heading))
            .setMessage(getString(R.string.logout_ask))
            .setPositiveButton(getString(R.string.logout_heading)) { _, _ ->

                // Clean up map/overlays in MainActivity
                val mainActivity = activity
                if (mainActivity is MainActivity) {
                    mainActivity.cleanupOnLogout()
                }

                // Configure Google Sign-In client for logout
                val googleSignInClient = GoogleSignIn.getClient(
                    requireContext(),
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                )

                googleSignInClient.signOut().addOnCompleteListener {
                    FirebaseAuth.getInstance().signOut()

                    // Navigate back to Login
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity?.finish()
                }
            }
            .setNegativeButton(getString(R.string.cancel_option), null)
            .show()
    }


    // Language Selection Logic


    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Afrikaans", "Zulu")
        val languageCodes = arrayOf("en", "af", "zu")

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.lang_choose))
            .setItems(languages) { dialog, which ->
                val selectedLanguage = languageCodes[which]
                Log.e(" LANGUAGE", " User selected language: $selectedLanguage")

                setLocale(requireContext(), selectedLanguage)
                saveLanguagePreference(selectedLanguage)

                // Save and sync with coroutine scope
                lifecycleScope.launch {
                    try {
                        Log.e(" LANGUAGE", "Saving language: $selectedLanguage")
                        SettingsRepository.saveSettings(requireContext(), language = selectedLanguage)

                        if (isOnline()) {
                            Log.e(" LANGUAGE", "Syncing to Firebase...")
                            val success = SettingsRepository.syncToFirebase(requireContext())
                            Log.e(" LANGUAGE", "Sync complete: $success")
                        }

                        // Wait a moment to ensure sync completes
                        kotlinx.coroutines.delay(500)

                        Log.e(" LANGUAGE", "Recreating activity...")
                        withContext(Dispatchers.Main) {
                            requireActivity().recreate()
                        }
                    } catch (e: Exception) {
                        Log.e(" LANGUAGE", "Error: ${e.message}", e)
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel_option)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun saveLanguagePreference(languageCode: String) {
        val prefs = requireActivity().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_language", languageCode).apply()
    }

    fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
