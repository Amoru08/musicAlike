package com.example.musicalike

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class no_registrado : AppCompatActivity() {

    private lateinit var login: Button
    private lateinit var regis: Button
    private lateinit var googleSignUp: Button
    private lateinit var email: EditText
    private lateinit var contra: EditText
    private lateinit var exit: Button

    private val requestCodeGoogleSignIn = 9001
    private val firestoreDb = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_registrado)

        email = findViewById(R.id.elEmail)
        contra = findViewById(R.id.contrasena)
        regis = findViewById(R.id.regis2)
        login = findViewById(R.id.atras)
        googleSignUp = findViewById(R.id.regisGoogle) // Nuevo botón para registro con Google
        exit = findViewById(R.id.atras)

        login.setOnClickListener { goToLogin() }
        setup()

        // Configuración para el botón de Google Sign-Up
        googleSignUp.setOnClickListener { authenticateWithGoogle() }

        // Configuración para el botón de salida
        exit.setOnClickListener { goToHome() }
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

    private fun authenticateWithGoogle() {
        Log.d("no_registrado", "Iniciando autenticación con Google")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Agrega tu ID de cliente web de Firebase
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, requestCodeGoogleSignIn)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == this.requestCodeGoogleSignIn) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("no_registrado", "Cuenta de Google: ${account?.displayName}, Email: ${account?.email}")
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.e("no_registrado", "Error en la autenticación de Google: ${e.message}")
                Toast.makeText(this, "Error en la autenticación de Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        account?.let {
            val credential = GoogleAuthProvider.getCredential(it.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        saveUserDetails(account) // Guarda los detalles del usuario en Firestore
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
                    // Documento del usuario creado/actualizado exitosamente
                    Log.d("no_registrado", "Documento del usuario creado/actualizado exitosamente.")
                }
                .addOnFailureListener { e ->
                    // Manejar el error al crear/actualizar el documento del usuario
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
        builder.setNegativeButton("Cancelar") { _, _ -> goToLogin() }

        val dialog: android.app.AlertDialog = builder.create()
        dialog.show()
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
