package com.example.musicalike

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.firestore.FirebaseFirestore

class SpotifyUserActivity : AppCompatActivity() {

    private lateinit var detailButton: Button
    private lateinit var loginButton: Button
    private lateinit var signOutButton: Button
    private lateinit var exit: Button

    private val requestCode = 9001

    private val firestoreDb = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotifyuser)

        loginButton = findViewById(R.id.authButton)
        detailButton = findViewById(R.id.detalles)
        signOutButton = findViewById(R.id.signOutButton)
        exit = findViewById(R.id.exit)

        loginButton.setOnClickListener { authenticateWithGoogle() }

        detailButton.setOnClickListener { goToDetalles() }

        signOutButton.setOnClickListener { signOutFromGoogle() }

        exit.setOnClickListener { goToHome() }
    }

    private fun authenticateWithGoogle() {
        Log.d("SpotifyUserActivity", "Iniciando autenticación con Google")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        if (GoogleSignIn.getLastSignedInAccount(this) != null) {
            googleSignInClient.signOut()
        }
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == this.requestCode) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("SpotifyUserActivity", "Cuenta de Google: ${account?.displayName}, Email: ${account?.email}")
                saveUserDetails(account)
            } catch (e: ApiException) {
                Log.e("SpotifyUserActivity", "Error de autenticación: ${e.message}")
                Toast.makeText(this, "Error de autenticación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserDetails(account: GoogleSignInAccount?) {
        account?.let {
            val userName = it.displayName ?: "Desconocido"
            val userEmail = it.email ?: "Desconocido"
            val userDocument = firestoreDb.collection("users").document(userEmail)

            Log.d("SpotifyUserActivity", "Usuario: $userName, Email: $userEmail")

            userDocument.set(mapOf("name" to userName, "email" to userEmail))
                .addOnSuccessListener {
                    // Documento del usuario creado/actualizado exitosamente
                    Log.d("SpotifyUserActivity", "Documento del usuario creado/actualizado exitosamente.")
                }
                .addOnFailureListener { e ->
                    // Manejar el error al crear/actualizar el documento del usuario
                    e.printStackTrace()
                    Log.e("SpotifyUserActivity", "Error al crear/actualizar el documento del usuario: ${e.message}")
                }
        }
    }

    private fun goToDetalles() {
        val i = Intent(this, SpotifyDetailsActivity::class.java)
        startActivity(i)
    }
    private fun goToHome() {
        val i = Intent(this, HomeActivity::class.java)
        startActivity(i)
    }

    private fun signOutFromGoogle() {
        val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)

        googleSignInClient.signOut()
            .addOnCompleteListener(this) {
                Toast.makeText(this, "Has cerrado sesión exitosamente", Toast.LENGTH_SHORT).show()
            }
    }

}