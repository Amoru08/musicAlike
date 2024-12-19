package com.example.musicalike

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class si_registrado : AppCompatActivity() {
    private lateinit var atras: Button
    private lateinit var elEmail: EditText
    private lateinit var laContra: EditText
    private lateinit var ir: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_si_registrado)

        // Inicializar los elementos de la interfaz
        atras = findViewById(R.id.atras2)
        elEmail = findViewById(R.id.elEmail)
        laContra = findViewById(R.id.laContra)
        ir = findViewById(R.id.irHome)

        // Obtener los extras del Intent
        val bundle = intent.extras
        val email = bundle?.getString("email")
        val provider = bundle?.getString("provider")

        // Configurar la vista
        setup(email ?: "", provider ?: "")
    }

    private fun setup(emails: String, providers: String) {
        title = "Acceder"
        elEmail.setText(emails)
        laContra.setText(providers)

        // Botón de "Atrás" para cerrar sesión y volver a la pantalla principal
        atras.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            goToPrincipio()
        }

        // Botón de "Ir a Home" para iniciar sesión con Firebase y redirigir a Spotify
        ir.setOnClickListener {
            val emailInput = elEmail.text.toString()
            val passwordInput = laContra.text.toString()

            // Verificar si los campos están vacíos
            if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese email y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Aquí es donde se hace la autenticación con Firebase
            FirebaseAuth.getInstance().signInWithEmailAndPassword(emailInput, passwordInput)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Si la autenticación con Firebase es exitosa, redirigimos a Spotify
                        goToSpotifyAuth()
                    } else {
                        // Muestra un mensaje de error si falla la autenticación
                        Toast.makeText(this, "Error al iniciar sesión: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun goToPrincipio() {
        // Redirigir a la pantalla principal
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }

    private fun goToSpotifyAuth() {
        // Aquí ya tienes un usuario autenticado en Firebase, ahora redirigimos a Spotify
        val clientId = "1d1c94387942461b8bd890e34b4ab6c7"
        val clientSecret = "785cb1c64f1044b0b0597a035e7c8cd8"
        val redirectUri = "musicalike://callback"
        val state = "some_random_state"

        val authUrl = "https://accounts.spotify.com/authorize" +
                "?client_id=$clientId" +
                "&response_type=code" +
                "&redirect_uri=$redirectUri" +
                "&state=$state" +
                "&scope=user-library-read playlist-read-private user-read-email"

        // Abrir el navegador para que el usuario autorice la aplicación
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
        startActivity(intent)
    }
}
