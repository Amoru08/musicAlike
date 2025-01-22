package com.example.musicalike

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.firestore.FirebaseFirestore

class SpotifyDetailsActivity : BaseActivity() {

    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var exit: Button
    private val firestoreDb = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotify_detail)

        exit = findViewById(R.id.backButton)
        userNameTextView = findViewById(R.id.userNameTextView)
        userEmailTextView = findViewById(R.id.userEmailTextView)

        exit.setOnClickListener { goToBack() }
        val currentUserEmail = GoogleSignIn.getLastSignedInAccount(this)?.email
        if (currentUserEmail != null) {
            getUserDetailsFromFirestore(currentUserEmail)
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUserDetailsFromFirestore(email: String) {
        firestoreDb.collection("users")
            .document(email)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val userName = documentSnapshot.getString("name")
                    val userEmail = documentSnapshot.getString("email")
                    userNameTextView.text = "Nombre: $userName"
                    userEmailTextView.text = "Correo: $userEmail"
                } else {
                    Toast.makeText(this, "No se encontraron detalles para este usuario", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener los detalles: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToBack() {
        val i = Intent(this, SpotifyUserActivity::class.java)
        startActivity(i)
    }
}