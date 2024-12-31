package com.example.musicalike

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

data class Song(
    val name: String,   // Nombre de la canción
    val artist: String, // Nombre del artista
    val tags: List<String> = listOf()
)

class SongAdapter(private val songs: MutableList<Song>) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private val firestoreDb = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song)
    }

    override fun getItemCount(): Int = songs.size

    // Método para actualizar los resultados de la búsqueda
    fun updateResults(newSongs: List<Song>) {
        songs.clear()
        songs.addAll(newSongs)
        notifyDataSetChanged()
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val songNameTextView: TextView = itemView.findViewById(R.id.songNameTextView)
        private val artistNameTextView: TextView = itemView.findViewById(R.id.artistNameTextView)
        private val favoriteButton: ImageView = itemView.findViewById(R.id.favoriteButton)

        fun bind(song: Song) {
            songNameTextView.text = song.name
            artistNameTextView.text = song.artist

            favoriteButton.setOnClickListener {
                addToFavorites(song)
                favoriteButton.setImageResource(R.drawable.star_icon)  // Cambiar el icono a estrella llena
            }
        }

        private fun addToFavorites(song: Song) {
            val favoriteSong = hashMapOf(
                "Plataforma" to "YouTube", // O la plataforma correspondiente
                "Nombre" to song.name,
                "Artista" to song.artist
            )

            firestoreDb.collection("favoritos")
                .add(favoriteSong)
                .addOnSuccessListener {
                    // Éxito al guardar en Firestore
                }
                .addOnFailureListener { e ->
                    // Error al guardar en Firestore
                    e.printStackTrace()
                }
        }
    }
}