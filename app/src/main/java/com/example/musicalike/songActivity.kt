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
    var name: String,
    var artist: String,
    var youtubeUrl: String = "",
    val tags: List<String> = listOf(),
    var isFavorite: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createStringArrayList() ?: emptyList(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(artist)
        parcel.writeString(youtubeUrl)
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
    private var songs: List<Song>,
    private val userEmail: String,
    private val onFavoriteClicked: (Song) -> Unit,
    private val onSongClicked: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songNameTextView: TextView = itemView.findViewById(R.id.songNameTextView)
        val artistNameTextView: TextView = itemView.findViewById(R.id.artistNameTextView)
        val favoriteButton: ImageView = itemView.findViewById(R.id.favoriteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.songNameTextView.text = song.name
        holder.artistNameTextView.text = song.artist
        holder.favoriteButton.setImageResource(
            if (song.isFavorite) R.drawable.star_point_icon else R.drawable.star_icon
        )

        holder.favoriteButton.setOnClickListener {
            onFavoriteClicked(song)
        }

        holder.itemView.setOnClickListener {
            onSongClicked(song)
        }
    }

    override fun getItemCount() = songs.size

    fun updateResults(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }

    fun getSongs(): List<Song> {
        return songs
    }
}