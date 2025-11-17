//Code Attribution:
//For the code below these are the sources I have used to improve my knowledge and implement features:
//GeeksForGeeks, 2025. How to implement preferences settings screen in Android?. [online] Available at: <https://www.geeksforgeeks.org/android/how-to-implement-preferences-settings-screen-in-android/> [Accessed 29 September 2025].


package com.example.pokemon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent recreating if already has fragment
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }
}
