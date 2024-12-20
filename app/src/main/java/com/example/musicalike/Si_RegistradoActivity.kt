package com.example.musicalike

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
        Log.d("si_registrado", "Entrando en si_registrado")
        setContentView(R.layout.activity_si_registrado)

        // Inicializar elementos
        atras = findViewById(R.id.atras2)
        elEmail = findViewById(R.id.elEmail)
        laContra = findViewById(R.id.laContra)
        ir = findViewById(R.id.irHome)

        // Obtener extras del Intent
        val bundle = intent.extras
        Log.d("si_registrado", "Extras recibidos: $bundle")

        // Configurar vistas
        setup()
    }

    private fun setup() {
        title = "Acceder"

        // Botón de "Atrás" para cerrar sesión y volver a la pantalla principal
        atras.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            goToPrincipio()
        }

        // Botón de "Ir a Home" para iniciar sesión con Firebase
        ir.setOnClickListener {
            val emailInput = elEmail.text.toString()
            val passwordInput = laContra.text.toString()

            // Verificar si los campos están vacíos
            if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese email y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Intentar iniciar sesión con Firebase
            FirebaseAuth.getInstance().signInWithEmailAndPassword(emailInput, passwordInput)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Si la autenticación es exitosa, ir a HomeActivity
                        goToHome()
                    } else {
                        // Si falla la autenticación, mostrar el error
                        val errorMessage = task.exception?.message ?: "Error desconocido"
                        Toast.makeText(this, "Error al iniciar sesión: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun goToPrincipio() {
        // Redirigir a la pantalla principal
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
        finish() // Asegura que no se pueda volver a esta actividad
    }

    private fun goToHome() {
        // Redirigir a la pantalla de inicio después de un inicio de sesión exitoso
        val i = Intent(this, HomeActivity::class.java) // Asegúrate de que HomeActivity esté correctamente configurada
        startActivity(i)
        finish() // Asegura que no se pueda volver a esta actividad
    }
}
