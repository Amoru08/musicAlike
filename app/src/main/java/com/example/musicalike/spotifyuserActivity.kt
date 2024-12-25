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
    private lateinit var signOutButton: Button  // Botón para cerrar sesión
    private val requestCode = 9001  // Código de solicitud para la autenticación de Google

    private val firestoreDb = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotifyuser)

        // Inicializa las vistas
        loginButton = findViewById(R.id.authButton)
        detailButton = findViewById(R.id.detalles)
        signOutButton = findViewById(R.id.signOutButton) // Agregar botón de cerrar sesión

        // Configura el botón de login
        loginButton.setOnClickListener { authenticateWithGoogle() }

        // Configura el botón de detalles
        detailButton.setOnClickListener { goToDetalles() }

        // Configura el botón de cerrar sesión
        signOutButton.setOnClickListener { signOutFromGoogle() }
    }

    /**
     * Inicia el flujo de autenticación con Google
     */
    private fun authenticateWithGoogle() {
        Log.d("SpotifyUserActivity", "Iniciando autenticación con Google")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Si el usuario ya ha iniciado sesión, ofrecer la opción de cambiar de cuenta
        if (GoogleSignIn.getLastSignedInAccount(this) != null) {
            googleSignInClient.signOut()  // Cerrar sesión antes de permitir otro inicio
        }

        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == this.requestCode) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // El usuario ha iniciado sesión correctamente
                val account = task.getResult(ApiException::class.java)
                Log.d("SpotifyUserActivity", "Cuenta de Google: ${account?.displayName}, Email: ${account?.email}")
                saveUserDetails(account)
            } catch (e: ApiException) {
                // Error de autenticación
                Log.e("SpotifyUserActivity", "Error de autenticación: ${e.message}")
                Toast.makeText(this, "Error de autenticación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserDetails(account: GoogleSignInAccount?) {
        account?.let {
            val userName = it.displayName ?: "Desconocido"
            val userEmail = it.email ?: "Desconocido"

            Log.d("SpotifyUserActivity", "Usuario: $userName, Email: $userEmail")

            firestoreDb.collection("users")
                .document(userEmail)
                .set(mapOf("name" to userName, "email" to userEmail))
                .addOnSuccessListener {
                    Log.d("SpotifyUserActivity", "Detalles de usuario guardados en Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e("SpotifyUserActivity", "Error al guardar los detalles del usuario: ${e.message}")
                }
        }
    }

    private fun goToDetalles() {
        val i = Intent(this, SpotifyDetailsActivity::class.java)
        startActivity(i)
    }

    /**
     * Cerrar sesión del usuario de Google
     */
    private fun signOutFromGoogle() {
        val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)

        googleSignInClient.signOut()
            .addOnCompleteListener(this) {
                Toast.makeText(this, "Has cerrado sesión exitosamente", Toast.LENGTH_SHORT).show()
            }
    }
}
