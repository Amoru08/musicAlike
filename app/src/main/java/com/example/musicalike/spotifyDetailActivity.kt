package com.example.musicalike

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class SpotifyDetailsActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotify_detail)

        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)

        fetchUserFromFirestore()
    }

    private fun fetchUserFromFirestore() {
        val sharedPreferences = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
        val savedEmail = sharedPreferences.getString("last_user_email", null)

        if (savedEmail.isNullOrEmpty()) {
            Toast.makeText(this, "No hay datos guardados del usuario.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users")
            .document(savedEmail)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val email = document.getString("email") ?: "Sin email"
                    val password = document.getString("password") ?: "Sin contraseña"

                    tvUserName.text = "Email: $email"
                    tvUserEmail.text = "Contraseña: $password"
                } else {
                    Toast.makeText(this, "No se encontraron datos del usuario.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al recuperar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
