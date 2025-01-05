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
    private val userEmail: String, // Asegúrate de pasar userEmail al adaptador
    private val onFavoriteClicked: (Song) -> Unit // Cambiar el tipo de lambda a (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

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

            // Establecer el icono basado en si la canción es favorita o no
            favoriteButton.setImageResource(
                if (song.isFavorite) R.drawable.star_point_icon else R.drawable.star_icon
            )

            favoriteButton.setOnClickListener {
                song.isFavorite = !song.isFavorite
                updateFavoriteStatus(song)
                favoriteButton.setImageResource(
                    if (song.isFavorite) R.drawable.star_point_icon else R.drawable.star_icon
                )
                onFavoriteClicked(song) // Llamar a la lambda pasada al adaptador
            }
        }

        private fun updateFavoriteStatus(song: Song) {
            if (song.isFavorite) {
                addToFavorites(song)
            } else {
                removeFromFavorites(song)
            }
        }

        private fun addToFavorites(song: Song) {
            // Separar el nombre y asignar la parte antes del guion como artista
            val parts = song.name.split(" - ")
            if (parts.size > 1) {
                song.artist = parts[0].trim()
                song.name = cleanSongName(parts[1].trim())
            }

            firestoreDb.collection("users").document(userEmail).collection("favoritos")
                .whereEqualTo("Nombre", song.name)
                .whereEqualTo("Artista", song.artist)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        val favoriteSong = hashMapOf(
                            "Plataforma" to "YouTube",
                            "Nombre" to song.name,
                            "Artista" to song.artist
                        )

                        firestoreDb.collection("users").document(userEmail).collection("favoritos")
                            .add(favoriteSong)
                            .addOnSuccessListener {
                                // Éxito al guardar en Firestore
                            }
                            .addOnFailureListener { e ->
                                // Error al guardar en Firestore
                                e.printStackTrace()
                            }
                    } else {
                        // Ya existe en los favoritos
                    }
                }
                .addOnFailureListener { e ->
                    // Error al verificar en Firestore
                    e.printStackTrace()
                }
        }

        private fun removeFromFavorites(song: Song) {
            firestoreDb.collection("users").document(userEmail).collection("favoritos")
                .whereEqualTo("Nombre", song.name)
                .whereEqualTo("Artista", song.artist)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        firestoreDb.collection("users").document(userEmail).collection("favoritos").document(document.id).delete()
                    }
                }
                .addOnFailureListener { e ->
                    // Error al eliminar en Firestore
                    e.printStackTrace()
                }
        }

        private fun cleanSongName(name: String): String {
            return name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                .replace(Regex("&\\S*"), "").trim()
        }
    }
}