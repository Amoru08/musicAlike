package com.example.musicalike

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnRegis: Button
    private lateinit var btnIni: Button

    override fun onCreate(savedInstanceState: Bundle?) {
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


}
