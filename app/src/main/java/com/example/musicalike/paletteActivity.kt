package com.example.musicalike

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button

class PaletteActivity : BaseActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private var selectedTheme: String = "default"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_palette)

        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        val blueButton: Button = findViewById(R.id.themeBlueButton)
        val greenButton: Button = findViewById(R.id.themeGreenButton)
        val pinkButton: Button = findViewById(R.id.themePinkButton)
        val saveButton: Button = findViewById(R.id.saveButton)
        val resetButton: Button = findViewById(R.id.resetThemeButton)

        blueButton.setOnClickListener {
            selectedTheme = "blue_light"
            previewTheme(selectedTheme)
        }
        greenButton.setOnClickListener {
            selectedTheme = "green_light"
            previewTheme(selectedTheme)
        }
        pinkButton.setOnClickListener {
            selectedTheme = "pink_light"
            previewTheme(selectedTheme)
        }
        saveButton.setOnClickListener {
            saveTheme(selectedTheme)
        }
        resetButton.setOnClickListener {
            selectedTheme = "default"
            previewTheme(selectedTheme)
        }
    }

    private fun previewTheme(theme: String) {
        val editor = sharedPreferences.edit()
        editor.putString("selectedTheme", theme)
        editor.apply()
        recreate()
    }

    private fun saveTheme(theme: String) {
        val editor = sharedPreferences.edit()
        editor.putString("theme", theme)
        editor.apply()
        val resultIntent = Intent()
        resultIntent.putExtra("selectedTheme", theme)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
