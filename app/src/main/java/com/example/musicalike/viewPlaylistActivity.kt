package com.example.musicalike

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.firestore.FirebaseFirestore


class viewPlaylistActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var save : Button
    private lateinit var adapter: PlaylistAdapter
    private val firestoreDb = FirebaseFirestore.getInstance()
    private val playlists = mutableListOf<Playlist>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_playlist)
        recyclerView = findViewById(R.id.songsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = PlaylistAdapter(playlists) { playlist ->
            // En viewPlaylistActivity (cuando el usuario selecciona una playlist)
            val intent = Intent(this, PlaylistsActivity::class.java)
            intent.putExtra("playlistId", playlist.id) // Pasar el ID de la playlist seleccionada
            startActivity(intent)

        }
        recyclerView.adapter = adapter

        loadPlaylists()

    }

    private fun loadPlaylists() {
        val userEmail = GoogleSignIn.getLastSignedInAccount(this)?.email
        if (userEmail == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        firestoreDb.collection("users").document(userEmail).collection("playlists")
            .get()
            .addOnSuccessListener { result ->
                playlists.clear()
                for (document in result) {
                    val playlist = Playlist(
                        name = document.getString("name") ?: "",
                        id = document.id
                    )
                    playlists.add(playlist)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("PlaylistsActivity", "Error al cargar playlists: ${e.message}")
                Toast.makeText(this, "Error al cargar playlists", Toast.LENGTH_SHORT).show()
            }
    }
}



data class Playlist(
    val name: String = "",
    val id: String = ""
)
class PlaylistAdapter(
    private val playlists: List<Playlist>,
    private val onPlaylistClicked: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    class PlaylistViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val playlistNameTextView: TextView = view.findViewById(R.id.playlistNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        // Inflar el layout del item de playlist
        val view = LayoutInflater.from(parent.context).inflate(R.layout.playlist_view, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]
        // Asignar el nombre de la playlist al TextView
        holder.playlistNameTextView.text = playlist.name

        // Configurar el clic en el CardView
        holder.view.setOnClickListener {
            onPlaylistClicked(playlist)
        }
    }

    override fun getItemCount() = playlists.size
}





