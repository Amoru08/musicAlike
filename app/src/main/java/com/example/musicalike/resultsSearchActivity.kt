package com.example.musicalike

import android.os.Bundle
import android.util.Log
import android.widget.Button
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

        // Capturar los logs cuando se selecciona una canción
        logSongDetails(selectedSong)

        // Realizar la búsqueda basada en los logs
        searchSongsByLogs(selectedSong)
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

    private fun logSongDetails(song: Song) {
        // Capturar los detalles de la canción en los logs
        Log.d("ResultsSearchActivity", "Selected song details - Name: ${song.name}, Artist: ${song.artist}, Tags: ${song.tags}")
    }

    private fun searchSongsByLogs(selectedSong: Song) {
        // Obtener la información de los logs
        val logs = "Name: ${selectedSong.name}, Artist: ${selectedSong.artist}, Tags: ${selectedSong.tags.joinToString(", ")}"

        // Buscar canciones utilizando la información de los logs
        spotifyService.searchSongsByLogs(logs) { songsByLogs ->
            if (songsByLogs != null) {
                // Log the found songs
                songsByLogs.take(20).forEach { song ->
                    Log.d("ResultsSearchActivity", "Found song: ${song.name} by ${song.artist}")
                }

                // Update the UI with the found songs
                runOnUiThread {
                    adapter.updateResults(songsByLogs.take(20))
                }
            } else {
                Log.d("ResultsSearchActivity", "No songs found with the given logs.")
            }
        }
    }

    private fun savePlaylist() {
        userEmail?.let { email ->
            val playlistName = "My Playlist"
            val playlist = mapOf(
                "name" to playlistName,
                "songs" to adapter.getSongs().map { song ->
                    mapOf(
                        "name" to song.name,
                        "artist" to song.artist
                    )
                }
            )

            firestoreDb.collection("users").document(email).collection("playlists")
                .add(playlist)
                .addOnSuccessListener {
                    Toast.makeText(this, "Playlist guardada exitosamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("ResultsSearchActivity", "Error al guardar la playlist: ${e.message}")
                    Toast.makeText(this, "Error al guardar la playlist: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}