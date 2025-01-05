package com.example.musicalike

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.firestore.FirebaseFirestore

class ResultsSearchActivity : AppCompatActivity() {

    private lateinit var songRecyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private val firestoreDb = FirebaseFirestore.getInstance()
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results_search)

        Log.d("ResultsSearchActivity", "onCreate called")

        songRecyclerView = findViewById(R.id.songRecyclerView)
        userEmail = GoogleSignIn.getLastSignedInAccount(this)?.email

        if (userEmail == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        adapter = SongAdapter(mutableListOf(), userEmail!!) { song ->
            onFavoriteClicked(song)
        }

        songRecyclerView.layoutManager = LinearLayoutManager(this)
        songRecyclerView.adapter = adapter

        // Obtener los resultados de la búsqueda pasados a través del Intent
        val videos = intent.getParcelableArrayListExtra<Song>("videos")
        if (videos != null) {
            adapter.updateResults(videos)
        }
    }

    private fun onFavoriteClicked(song: Song) {
        userEmail?.let { email ->
            Log.d("ResultsSearchActivity", "Favorite clicked for song: ${song.name}")
            // Limitar tags a los primeros 10
            val limitedTags = song.tags.take(10)
            // Guardar la canción favorita en Firestore
            val favoriteSong = mapOf(
                "Nombre" to song.name,
                "Artista" to song.artist,
                "Tags" to limitedTags
            )

            firestoreDb.collection("users").document(email).collection("favoritos")
                .add(favoriteSong)
                .addOnSuccessListener {
                    Log.d("ResultsSearchActivity", "Favorite song saved: ${song.name}")
                    Toast.makeText(this, "Canción guardada como favorita", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("ResultsSearchActivity", "Error al guardar la canción favorita: ${e.message}")
                    Toast.makeText(this, "Error al guardar la canción favorita: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}