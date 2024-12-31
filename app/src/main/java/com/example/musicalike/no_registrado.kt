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
    private lateinit var regis: Button
    private lateinit var email: EditText
    private lateinit var contra: EditText

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_registrado)

        email = findViewById(R.id.elEmail)
        contra = findViewById(R.id.contrasena)
        regis = findViewById(R.id.regis2)
        login = findViewById(R.id.atras)
        login.setOnClickListener {
            goToLogin()
        }
        setup()
    }

    private fun goToLogin() {
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }

    private fun setup() {
        regis.setOnClickListener {
            if (email.text.isNotEmpty() && contra.text.isNotEmpty()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.text.toString(), contra.text.toString())
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            goToHome()
                        } else {
                            showAlert()
                        }
                    }
            } else {
                showAlert("Por favor, complete todos los campos.")
            }
        }
    }

    private fun showAlert(message: String = "Se ha producido un error") {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", null)
        builder.setNegativeButton("Cancelar") { _, _ -> goToLogin() }

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
    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
