package com.example.musicalike

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

enum class ProviderType {
    BASIC
}

class no_registrado : AppCompatActivity() {

    private lateinit var login: Button
    private lateinit var regis2: Button
    private lateinit var email: EditText
    private lateinit var contra: EditText
    private lateinit var acceder: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_registrado)

        email = findViewById(R.id.elEmail)
        contra = findViewById(R.id.contrasena)
        regis2 = findViewById(R.id.regis2)
        login = findViewById(R.id.atras)
        // Configurar listeners para botones
        login.setOnClickListener {
            goToLogin()
        }

        // Llamar a setup para configurar autenticación
        setup()
    }

    private fun goToLogin() {
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }

    private fun setup() {
        title = "Autenticación"

        // Listener para el botón de registro
        regis2.setOnClickListener {
            if (email.text.isNotEmpty() && contra.text.isNotEmpty()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.text.toString(), contra.text.toString())
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            showHome(it.result?.user?.email ?: "", ProviderType.BASIC)
                        } else {
                            showAlert()
                        }
                    }
            } else {
                showAlert("Por favor, complete todos los campos.")
            }
        }

        // Listener para el botón de acceso

    }

    private fun showAlert(message: String = "Se ha producido un error") {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome(email: String, provider: ProviderType) {
        val homeIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
    }
}
