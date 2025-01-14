package com.example.musicalike

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.firestore.FirebaseFirestore

class ViewFavoritesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FavoriteSongsAdapter
    private val firestoreDb = FirebaseFirestore.getInstance()
    private val favoriteSongs = mutableListOf<Song>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_favorites)

        recyclerView = findViewById(R.id.favoritesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FavoriteSongsAdapter(favoriteSongs)
        recyclerView.adapter = adapter

        loadFavoriteSongs()
    }

    private fun loadFavoriteSongs() {
        val userEmail = GoogleSignIn.getLastSignedInAccount(this)?.email
        if (userEmail == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        firestoreDb.collection("users").document(userEmail).collection("favoritos")
            .get()
            .addOnSuccessListener { result ->
                favoriteSongs.clear()
                for (document in result) {
                    val name = document.getString("Nombre") ?: ""
                    val artist = document.getString("Artista") ?: ""
                    val youtubeUrl = document.getString("YoutubeUrl") ?: ""

                    val song = Song(name = name, artist = artist, youtubeUrl = youtubeUrl, isFavorite = true)
                    favoriteSongs.add(song)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("ViewFavoritesActivity", "Error al cargar canciones favoritas: ${e.message}")
                Toast.makeText(this, "Error al cargar canciones favoritas", Toast.LENGTH_SHORT).show()
            }
    }
}

class FavoriteSongsAdapter(
    private val favoriteSongs: List<Song>
) : RecyclerView.Adapter<FavoriteSongsAdapter.FavoriteSongViewHolder>() {

    class FavoriteSongViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val songNameTextView: TextView = view.findViewById(R.id.songNameTextView)
        val artistNameTextView: TextView = view.findViewById(R.id.artistNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteSongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_song, parent, false)
        return FavoriteSongViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteSongViewHolder, position: Int) {
        val song = favoriteSongs[position]
        holder.songNameTextView.text = song.name
        holder.artistNameTextView.text = song.artist
    }

    override fun getItemCount() = favoriteSongs.size
}