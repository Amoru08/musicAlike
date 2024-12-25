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

        // Inicializa los TextViews
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)

        // Llamada para obtener los detalles del usuario
        fetchUserFromFirestore()
    }

    /**
     * Recupera la información del usuario desde Firestore
     */
    private fun fetchUserFromFirestore() {
        // Recupera el correo electrónico del usuario desde SharedPreferences
        val sharedPreferences = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
        val savedEmail = sharedPreferences.getString("last_user_email", null)

        // Verifica si se encontró el correo guardado
        if (savedEmail.isNullOrEmpty()) {
            Toast.makeText(this, "No hay datos guardados del usuario.", Toast.LENGTH_SHORT).show()
            return
        }

        // Realiza una consulta a Firestore para obtener los detalles del usuario por su email
        firestore.collection("users")
            .document(savedEmail)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Recupera el nombre y el email del documento
                    val displayName = document.getString("name") ?: "Sin nombre"
                    val email = document.getString("email") ?: "Sin email"

                    // Actualiza los TextViews con la información del usuario
                    tvUserName.text = "Nombre: $displayName"
                    tvUserEmail.text = "Email: $email"
                } else {
                    Toast.makeText(this, "No se encontraron datos del usuario.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al recuperar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
