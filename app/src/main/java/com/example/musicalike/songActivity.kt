package com.example.musicalike

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Song(
    val name: String,   // Nombre de la canción
    val artist: String, // Nombre del artista
    val tags: List<String> = listOf()
)


class SongAdapter(private val songs: MutableList<Song>) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

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

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val songNameTextView: TextView = itemView.findViewById(R.id.songNameTextView)
        private val artistNameTextView: TextView = itemView.findViewById(R.id.artistNameTextView)

        fun bind(song: Song) {
            songNameTextView.text = song.name
            artistNameTextView.text = song.artist
        }
    }
}

