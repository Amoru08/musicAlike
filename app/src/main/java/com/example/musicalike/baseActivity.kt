package com.example.musicalike

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedTheme()  // Aplica el tema guardado al inicio
    }

    private fun applySavedTheme() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val theme = sharedPreferences.getString("selectedTheme", "default")  // Obtiene el tema seleccionado
        when (theme) {
            "blue_light" -> setTheme(R.style.Theme_BlueLight)  // Aplica el tema azul
            "green_light" -> setTheme(R.style.Theme_GreenLight)  // Aplica el tema verde
            "pink_light" -> setTheme(R.style.Theme_PinkLight)  // Aplica el tema rosa
            else -> setTheme(R.style.AppTheme)  // Aplica el tema por defecto
        }
    }
}
