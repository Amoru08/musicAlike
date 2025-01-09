package com.example.musicalike

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.firestore.FirebaseFirestore

class PlaylistDetailsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private val firestoreDb = FirebaseFirestore.getInstance()
    private val songs = mutableListOf<Song>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_playlist) // Asegúrate de tener este layout

        recyclerView = findViewById(R.id.playlistRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SongAdapter(
            songs = songs,
            userEmail = "",
            onFavoriteClicked = {},
            onSongClicked = {}
        )
        recyclerView.adapter = adapter

        val playlistId = intent.getStringExtra("playlistId") ?: return
        loadPlaylistSongs(playlistId)
    }

    private fun loadPlaylistSongs(playlistId: String) {
        val userEmail = GoogleSignIn.getLastSignedInAccount(this)?.email
        if (userEmail == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        firestoreDb.collection("users").document(userEmail)
            .collection("playlists").document(playlistId)
            .get()
            .addOnSuccessListener { document ->
                val songList = document.get("songs") as? List<Map<String, Any>> ?: emptyList()
                songs.clear()
                songs.addAll(songList.map { songData ->
                    Song(
                        name = songData["name"] as String,
                        artist = songData["artist"] as String,
                        tags = (songData["tags"] as List<String>?) ?: emptyList()
                    )
                })
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("PlaylistDetailsActivity", "Error al cargar canciones: ${e.message}")
                Toast.makeText(this, "Error al cargar canciones", Toast.LENGTH_SHORT).show()
            }
    }
}
