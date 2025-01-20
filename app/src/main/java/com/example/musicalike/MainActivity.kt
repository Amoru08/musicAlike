package com.example.musicalike

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : BaseActivity() {

    private lateinit var btnRegis: Button
    private lateinit var btnIni: Button
    private val REQUEST_CODE_PALETTE = 1002
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Inicializar SharedPreferences antes de usarlo
        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        // Aplicar el tema guardado
        applySavedTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicialización de vistas
        btnRegis = findViewById(R.id.regis)
        btnIni = findViewById(R.id.inicio)

        // Configuración de listeners
        btnRegis.setOnClickListener {
            goToRegister()
        }

        btnIni.setOnClickListener {
            Log.d("MainActivity", "Botón 'Iniciar Sesión' presionado")
            goToInicioSesion()
        }
    }

    private fun goToRegister() {
        val i = Intent(this, no_registrado::class.java)
        startActivity(i)
    }

    private fun goToInicioSesion() {
        Log.d("MainActivity", "Navegando a si_registrado")
        val i = Intent(this, si_registrado::class.java)
        startActivity(i)
    }

    private fun applySavedTheme() {
        val theme = sharedPreferences.getString("theme", "default")
        when (theme) {
            "pink_light" -> setTheme(R.style.Theme_PinkLight)
            "green_light" -> setTheme(R.style.Theme_GreenLight)
            "blue_light" -> setTheme(R.style.Theme_BlueLight)
            else -> setTheme(R.style.AppTheme)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PALETTE && resultCode == RESULT_OK) {
            val selectedTheme = data?.getStringExtra("selectedTheme")
            if (selectedTheme != null) {
                // Aplica el tema seleccionado
                applySelectedTheme(selectedTheme)
            }
        }
    }

    private fun applySelectedTheme(selectedTheme: String) {
        val editor = sharedPreferences.edit()
        editor.putString("theme", selectedTheme)
        editor.apply()

        // Reinicia la actividad para aplicar el tema seleccionado
        recreate()
    }
}