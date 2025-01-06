package com.example.musicalike

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Parcel
import android.os.Parcelable

data class Song(
    var name: String,   // Nombre de la canción
    var artist: String, // Nombre del artista
    val tags: List<String> = listOf(),
    var isFavorite: Boolean = false // Indicador de favorito
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createStringArrayList() ?: emptyList(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(artist)
        parcel.writeStringList(tags)
        parcel.writeByte(if (isFavorite) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Song> {
        override fun createFromParcel(parcel: Parcel): Song {
            return Song(parcel)
        }

        override fun newArray(size: Int): Array<Song?> {
            return arrayOfNulls(size)
        }
    }
}

class SongAdapter(
    private val songs: MutableList<Song>,
    private val userEmail: String,
    private val onFavoriteClicked: (Song) -> Unit,
    private val onSongClicked: (Song) -> Unit // Callback para el click en la canción
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song, onFavoriteClicked, onSongClicked)
    }

    override fun getItemCount(): Int = songs.size

    fun updateResults(newSongs: List<Song>) {
        songs.clear()
        songs.addAll(newSongs)
        notifyDataSetChanged()
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val songNameTextView: TextView = itemView.findViewById(R.id.songNameTextView)
        private val artistNameTextView: TextView = itemView.findViewById(R.id.artistNameTextView)
        private val favoriteButton: ImageView = itemView.findViewById(R.id.favoriteButton)

        fun bind(song: Song, onFavoriteClicked: (Song) -> Unit, onSongClicked: (Song) -> Unit) {
            songNameTextView.text = song.name
            artistNameTextView.text = song.artist

            // Cambiar el ícono del botón de favorito según el estado de la canción
            favoriteButton.setImageResource(
                if (song.isFavorite) R.drawable.star_point_icon else R.drawable.star_icon
            )

            favoriteButton.setOnClickListener {
                // Alternar el estado de favorito
                song.isFavorite = !song.isFavorite
                favoriteButton.setImageResource(
                    if (song.isFavorite) R.drawable.star_point_icon else R.drawable.star_icon
                )
                onFavoriteClicked(song)
            }

            itemView.setOnClickListener {
                // Llamar al callback cuando se hace click en la canción
                onSongClicked(song)
            }
        }
    }
}
