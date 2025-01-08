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
    private val spotifyService = SpotifyService("d95be9b432a2437c913e965dcd72487d", "73b6598e631e4e51834db25238b82b32") // Reemplaza con tus credenciales

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
            onSongClicked = { song -> onSongClicked(song) }
        )

        songRecyclerView.layoutManager = LinearLayoutManager(this)
        songRecyclerView.adapter = adapter

        // Obtener la canción seleccionada pasada a través del Intent
        selectedSong = intent.getParcelableExtra("selectedSong") ?: return

        // Obtener los tags de Firestore y buscar canciones similares
        getTagsFromFirestoreAndSearchSongs(selectedSong)
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

    private fun getTagsFromFirestoreAndSearchSongs(song: Song) {
        firestoreDb.collection("users").document(userEmail!!).collection("favoritos")
            .whereEqualTo("Nombre", song.name)
            .whereEqualTo("Artista", song.artist)
            .get()
            .addOnSuccessListener { documents ->
                val tags = documents.flatMap { it.get("Tags") as? List<String> ?: emptyList() }
                if (tags.isNotEmpty()) {
                    Log.d("ResultsSearchActivity", "Tags found: $tags")
                    searchSongsByTagsAndArtist(tags, song.artist)
                } else {
                    Log.d("ResultsSearchActivity", "No tags available for search.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ResultsSearchActivity", "Error getting tags: ${e.message}")
                Toast.makeText(this, "Error obteniendo los tags: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun searchSongsByTagsAndArtist(tags: List<String>, artist: String) {
        if (tags.isEmpty()) {
            Log.d("ResultsSearchActivity", "No tags available for search.")
            return
        }

        // Limitar los tags a los primeros 10
        m

        // Definir las palabras clave a excluir
        val excludeKeywords = listOf("lyrics", "karaoke", "#shorts", "lyric", "live", "cover", "festival", "sub")

        spotifyService.searchSongsByTags(limitedTags) { songsByTags ->
            spotifyService.searchSongsByArtist(artist) { songsByArtist ->
                if (songsByTags != null && songsByArtist != null) {
                    val combinedSongs = (songsByArtist.take(3) + songsByTags)
                        .shuffled()
                        .distinct()
                        .take(20)

                    // Filtrar canciones que contienen palabras clave excluidas en el nombre o en los tags
                    val filteredSongs = combinedSongs.filterNot { song ->
                        // Comprobar si el nombre de la canción o algún tag contiene las palabras clave a excluir
                        song.name.containsAny(excludeKeywords, ignoreCase = true) ||
                                song.tags.any { it.containsAny(excludeKeywords, ignoreCase = true) }
                    }

                    // Log de las canciones filtradas
                    filteredSongs.forEach { song ->
                        Log.d("ResultsSearchActivity", "Found song: ${song.name} by ${song.artist}")
                    }

                    // Update the UI with the filtered songs
                    runOnUiThread {
                        adapter.updateResults(filteredSongs)
                    }
                } else {
                    Log.d("ResultsSearchActivity", "No songs found with the given tags or artist.")
                }
            }
        }
    }

    // Extensión para comprobar si el texto contiene alguna de las palabras clave (sin importar mayúsculas/minúsculas)
    private fun String.containsAny(keywords: List<String>, ignoreCase: Boolean = false): Boolean {
        return keywords.any { this.contains(it, ignoreCase) }
    }

}

