package com.example.musicalike

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedTheme()
    }

    private fun applySavedTheme() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val theme = sharedPreferences.getString("selectedTheme", "default")
        when (theme) {
            "blue_light" -> setTheme(R.style.Theme_BlueLight)
            "green_light" -> setTheme(R.style.Theme_GreenLight)
            "pink_light" -> setTheme(R.style.Theme_PinkLight)
            else -> setTheme(R.style.AppTheme)
        }
    }
}
