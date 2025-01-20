package com.example.musicalike

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.firestore.FirebaseFirestore

class ResultsSearchActivity : BaseActivity() {

    private lateinit var songRecyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private lateinit var savePlaylist: Button
    private val firestoreDb = FirebaseFirestore.getInstance()
    private var userEmail: String? = null
    private lateinit var selectedSong: Song
    private lateinit var exit: Button
    private val spotifyService = SpotifyService("d95be9b432a2437c913e965dcd72487d", "73b6598e631e4e51834db25238b82b32")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results_search)

        Log.d("ResultsSearchActivity", "onCreate called")
        exit = findViewById(R.id.backButton)
        savePlaylist = findViewById(R.id.savePlaylistButton)
        songRecyclerView = findViewById(R.id.songRecyclerView)
        userEmail = GoogleSignIn.getLastSignedInAccount(this)?.email

        exit.setOnClickListener { goToSearch() }

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

        savePlaylist.setOnClickListener {
            savePlaylist()
        }

        selectedSong = intent.getParcelableExtra("selectedSong") ?: return
        logSongDetails(selectedSong)
        searchSongs(selectedSong)
    }

    private fun onFavoriteClicked(song: Song) {
        userEmail?.let { email ->
            Log.d("ResultsSearchActivity", "Favorite clicked for song: ${song.name}")
            val limitedTags = song.tags.take(10)
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
        Log.d("ResultsSearchActivity", "Song clicked: ${song.name}")
        Toast.makeText(this, "Has seleccionado: ${song.name} de ${song.artist}", Toast.LENGTH_SHORT).show()
    }

    private fun logSongDetails(song: Song) {
        Log.d("ResultsSearchActivity", "Selected song details - Name: ${song.name}, Artist: ${song.artist}, Tags: ${song.tags}")
    }

    private fun searchSongs(selectedSong: Song) {
        val logs = "Name: ${selectedSong.name}, Artist: ${selectedSong.artist}, Tags: ${selectedSong.tags.joinToString(", ")}"
        spotifyService.searchSongsByLogs(logs) { songsByLogs ->
            if (songsByLogs != null) {
                val filteredSongs = songsByLogs.filter { song ->
                    !song.name.equals(selectedSong.name, ignoreCase = true)
                }
                val uniqueSongs = mutableSetOf<Pair<String, String>>()
                val distinctSongs = filteredSongs.filter { song ->
                    val uniqueKey = Pair(song.name, song.artist)
                    if (uniqueSongs.contains(uniqueKey)) {
                        false
                    } else {
                        uniqueSongs.add(uniqueKey)
                        true
                    }
                }
                distinctSongs.take(20).forEach { song ->
                    Log.d("ResultsSearchActivity", "Found song: ${song.name} by ${song.artist}")
                }
                runOnUiThread {
                    adapter.updateResults(distinctSongs.take(20))
                }
            } else {
                Log.d("ResultsSearchActivity", "No songs found with the given logs.")
            }

        }
    }

    private fun savePlaylist() {
        userEmail?.let { email ->
            val playlistId = "songs_like_${selectedSong.name}_from_${selectedSong.artist}".replace(" ", "_")
            val playlistRef = firestoreDb.collection("users").document(email).collection("playlists").document(playlistId)

            val songs = adapter.getSongs().map { song ->
                mapOf(
                    "name" to song.name,
                    "artist" to song.artist,
                    "tags" to song.tags
                )
            }
            val playlistData = mapOf(
                "name" to "Songs like ${selectedSong.name} from ${selectedSong.artist}",
                "songs" to songs
            )
            playlistRef.set(playlistData)
                .addOnSuccessListener {
                    Log.d("ResultsSearchActivity", "Playlist document created with all songs")
                    Toast.makeText(this, "Playlist guardada exitosamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("ResultsSearchActivity", "Error al crear la playlist: ${e.message}")
                    Toast.makeText(this, "Error al crear la playlist: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun goToSearch() {
        val i = Intent(this, SpotifySearchActivity::class.java)
        startActivity(i)
    }
}