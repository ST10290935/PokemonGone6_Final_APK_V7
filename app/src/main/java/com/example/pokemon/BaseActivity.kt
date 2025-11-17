//Code Attribution
// For the below code I have used these sources to guide me:
//Codex Creator, 2023. How To Translate Apps In Android Studio. [video online] Available at: < https://youtu.be/v_ohTJnyNAA?si=-FVjgedLgWnnOQEq> [Accessed 16 November 2025].
//Mobile App Development, 2024. How to Translate Your App to Different Languages | Localization in Android | Android Studio. [video online] Available at: < https://youtu.be/qqBPnKCwLn8?si=iGzG6D9LtR6_x_E_> [Accessed 16 November 2025].
//Phillip Lackner, 2021. How to Translate Your Android App to Any Language (SO EASY!) - Android Studio Tutorial. [video online] Available at: < https://youtu.be/LXbpsBtIIeM?si=O4LH5rggEhoaGKXO> [Accessed 15 November 2025].


package com.example.pokemon

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply saved language preference before anything else
        applySavedLanguage()
    }

    private fun applySavedLanguage() {
        val prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val savedLanguage = prefs.getString("selected_language", "en") ?: "en"
        setLocale(this, savedLanguage)
    }

    private fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
