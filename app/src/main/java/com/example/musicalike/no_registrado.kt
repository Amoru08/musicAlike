package com.example.musicalike

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class no_registrado : BaseActivity() {

    private lateinit var login: Button
    private lateinit var regis: Button
    private lateinit var googleSignUp: Button
    private lateinit var email: EditText
    private lateinit var contra: EditText
    private lateinit var exit: Button

    private val requestCodeGoogleSignIn = 9001
    private val firestoreDb = FirebaseFirestore.getInstance()
    private val defaultPassword = "ContraseñaPredeterminada123" // Contraseña predeterminada

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_registrado)

        email = findViewById(R.id.elEmail)
        contra = findViewById(R.id.contrasena)
        regis = findViewById(R.id.regis2)
        login = findViewById(R.id.atras)
        exit = findViewById(R.id.atras)

        setup()

        exit.setOnClickListener { goToMain() }
    }

    private fun goToMain() {
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }

    private fun setup() {
        regis.setOnClickListener {
            val emailInput = email.text.toString().trim()
            val passwordInput = contra.text.toString().trim()

            if (emailInput.isNotEmpty() && passwordInput.isNotEmpty()) {
                if (isValidEmail(emailInput)) {
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(emailInput, passwordInput)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                goToHome() // Ir a la página principal si el registro es exitoso
                            } else {
                                showAlert("Error al registrar. Intente nuevamente.")
                            }
                        }
                } else {
                    showAlert("Por favor, ingrese un correo válido.")
                }
            } else {
                showAlert("Por favor, complete todos los campos.")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == this.requestCodeGoogleSignIn) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("no_registrado", "Cuenta de Google: ${account?.displayName}, Email: ${account?.email}")
                fillCredentials(account) // Rellena los campos con los datos de la cuenta
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.e("no_registrado", "Error en la autenticación de Google: ${e.message}")
                Toast.makeText(this, "Error en la autenticación de Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fillCredentials(account: GoogleSignInAccount?) {
        account?.let {
            email.setText(it.email ?: "")
            contra.setText(defaultPassword) // Rellena con una contraseña predeterminada
            Toast.makeText(this, "Credenciales rellenadas automáticamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        account?.let {
            val credential = GoogleAuthProvider.getCredential(it.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        saveUserDetails(account)
                        goToHome()
                    } else {
                        Toast.makeText(this, "Error al registrar con Google", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun saveUserDetails(account: GoogleSignInAccount?) {
        account?.let {
            val userName = it.displayName ?: "Desconocido"
            val userEmail = it.email ?: "Desconocido"
            val userDocument = firestoreDb.collection("users").document(userEmail)

            Log.d("no_registrado", "Usuario: $userName, Email: $userEmail")

            userDocument.set(mapOf("name" to userName, "email" to userEmail))
                .addOnSuccessListener {
                    Log.d("no_registrado", "Documento del usuario creado/actualizado exitosamente.")
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    Log.e("no_registrado", "Error al crear/actualizar el documento del usuario: ${e.message}")
                }
        }
    }

    private fun showAlert(message: String = "Se ha producido un error") {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", null)
        builder.setNegativeButton("Cancelar") { _, _ -> goToHome() }

        val dialog: android.app.AlertDialog = builder.create()
        dialog.show()
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java) // Cambia a la actividad que corresponda
        startActivity(intent)
        finish()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

}
