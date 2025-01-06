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
    private lateinit var selectedSong: Song

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

        adapter = SongAdapter(
            songs = mutableListOf(),
            userEmail = userEmail!!,
            onFavoriteClicked = { song -> onFavoriteClicked(song) },
            onSongClicked = { song -> onSongClicked(song) } // Callback adicional
        )

        songRecyclerView.layoutManager = LinearLayoutManager(this)
        songRecyclerView.adapter = adapter

        // Obtener la canción seleccionada pasada a través del Intent
        selectedSong = intent.getParcelableExtra("selectedSong") ?: return

        // Buscar canciones similares
        findSongsWithSimilarTags(selectedSong)
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

    private fun onSongClicked(song: Song) {
        // Aquí puedes implementar la lógica para manejar el clic en la canción
        Log.d("ResultsSearchActivity", "Song clicked: ${song.name}")
        Toast.makeText(this, "Has seleccionado: ${song.name} de ${song.artist}", Toast.LENGTH_SHORT).show()
    }

    private fun findSongsWithSimilarTags(song: Song) {
        // Obtener todas las canciones favoritas del usuario
        firestoreDb.collection("users").document(userEmail!!).collection("favoritos")
            .get()
            .addOnSuccessListener { documents ->
                val favoriteSongs = documents.map { doc ->
                    Song(
                        name = doc.getString("Nombre") ?: "",
                        artist = doc.getString("Artista") ?: "",
                        tags = doc.get("Tags") as? List<String> ?: emptyList(),
                        isFavorite = true
                    )
                }

                // Filtrar canciones que tienen al menos un tag igual y no son del mismo artista
                val similarSongs = favoriteSongs.filter { otherSong ->
                    otherSong.artist != song.artist && otherSong.tags.intersect(song.tags).isNotEmpty()
                }

                // Mostrar resultados
                adapter.updateResults(similarSongs)
            }
            .addOnFailureListener { e ->
                Log.e("ResultsSearchActivity", "Error al buscar canciones similares: ${e.message}")
                Toast.makeText(this, "Error al buscar canciones similares: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}