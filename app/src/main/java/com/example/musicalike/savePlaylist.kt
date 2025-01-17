package com.example.musicalike

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class PlaylistsActivity : AppCompatActivity() {

    private val firestoreDb = FirebaseFirestore.getInstance()
    private lateinit var playlistRecyclerView: RecyclerView
    private lateinit var save: Button
    private lateinit var exit: Button
    private val songs = mutableListOf<Song>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_details)

        playlistRecyclerView = findViewById(R.id.songsRecyclerView)
        save = findViewById(R.id.saveButton)
        exit=findViewById(R.id.backButton)

        exit.setOnClickListener { goBack() }

        playlistRecyclerView.layoutManager = LinearLayoutManager(this)

        val userEmail = GoogleSignIn.getLastSignedInAccount(this)?.email

        if (userEmail == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val playlistId = intent.getStringExtra("playlistId")
        if (playlistId == null) {
            Toast.makeText(this, "No se ha seleccionado ninguna playlist", Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = SongAdapter(songs, userEmail, { song ->
            song.isFavorite = !song.isFavorite
            Log.d("PlaylistsActivity", "Canción favorita: ${song.name} - ${song.isFavorite}")
        }) { song ->
            // Lógica para manejar clic en la canción
            Log.d("PlaylistsActivity", "Canción seleccionada: ${song.name}")
        }

        playlistRecyclerView.adapter = adapter
        loadSongsFromPlaylist(playlistId, userEmail)

        save.setOnClickListener {
            Toast.makeText(this, "Se ha clickado en el botón de guardar", Toast.LENGTH_SHORT).show()
            saveSongsToYouTube()
        }
    }
    private fun loadSongsFromPlaylist(playlistId: String, userEmail: String) {
        firestoreDb.collection("users")
            .document(userEmail)
            .collection("playlists")
            .document(playlistId)
            .get()
            .addOnSuccessListener { document ->
                val songsArray = document.get("songs") as? List<Map<String, Any>> ?: return@addOnSuccessListener
                songs.clear()
                for (songMap in songsArray) {
                    val name = songMap["name"] as? String ?: continue
                    val artist = songMap["artist"] as? String ?: continue

                    val song = Song(name = name, artist = artist)
                    songs.add(song)
                }
                playlistRecyclerView.adapter?.notifyDataSetChanged()
                if (songs.isEmpty()) {
                    Toast.makeText(this, "No hay canciones en la playlist seleccionada", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar canciones: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveSongsToYouTube() {
        val userEmail = GoogleSignIn.getLastSignedInAccount(this)?.email
        if (userEmail == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedPlaylistId = intent.getStringExtra("playlistId") ?: run {
            Toast.makeText(this, "No hay playlists seleccionadas", Toast.LENGTH_SHORT).show()
            return
        }

        val account = GoogleSignIn.getLastSignedInAccount(this)
        val accessToken = account?.serverAuthCode

        if (accessToken == null) {
            Toast.makeText(this, "No se pudo obtener el token de acceso", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val songsToUpload = mutableListOf<Song>()

                val result = firestoreDb.collection("users")
                    .document(userEmail)
                    .collection("playlists")
                    .document(selectedPlaylistId)
                    .collection("songs")
                    .get()
                    .await()

                for (document in result) {
                    val name = document.getString("name") ?: continue
                    val artist = document.getString("artist") ?: continue
                    val song = Song(name = name, artist = artist)
                    songsToUpload.add(song)
                }

                if (songsToUpload.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PlaylistsActivity, "No hay canciones para subir", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val youtubePlaylistId = createYouTubePlaylist(accessToken, "Mi Playlist desde Musicalike", "Playlist creada automáticamente desde Musicalike")

                if (youtubePlaylistId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PlaylistsActivity, "Error al crear la playlist en YouTube", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                for (song in songsToUpload) {
                    val videoId = searchYouTubeVideo(apiKey = "AIzaSyAUkgeL0z1-owXSORm81ckUZqwqhClOImw", query = "${song.name} ${song.artist}")
                    if (videoId != null) {
                        addVideoToPlaylist(accessToken = accessToken, playlistId = youtubePlaylistId, videoId = videoId)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PlaylistsActivity, "Canciones cargadas en YouTube", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PlaylistsActivity, "Error al cargar canciones a YouTube: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

     fun createYouTubePlaylist(accessToken: String, title: String, description: String): String? {
        val url = URL("https://www.googleapis.com/youtube/v3/playlists?part=snippet,status")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val jsonBody = JSONObject()
        val snippet = JSONObject().apply {
            put("title", title)
            put("description", description)
        }
        val status = JSONObject().apply {
            put("privacyStatus", "public")
        }
        jsonBody.put("snippet", snippet)
        jsonBody.put("status", status)

        connection.outputStream.write(jsonBody.toString().toByteArray())
        connection.outputStream.flush()

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(response)
            return jsonResponse.getString("id") // Devuelve el ID de la playlist creada
        }

        return null
    }

    fun searchYouTubeVideo(apiKey: String, query: String): String? {
        val url = URL("https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&q=${query.replace(" ", "+")}&key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "GET"

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(response)
            val items = jsonResponse.getJSONArray("items")
            if (items.length() > 0) {
                val firstItem = items.getJSONObject(0)
                val id = firstItem.getJSONObject("id")
                return id.getString("videoId")
            }
        }

        return null
    }

    fun addVideoToPlaylist(accessToken: String, playlistId: String, videoId: String) {
        val url = URL("https://www.googleapis.com/youtube/v3/playlistItems?part=snippet")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val jsonBody = JSONObject()
        val snippet = JSONObject().apply {
            put("playlistId", playlistId)
            put("resourceId", JSONObject().apply {
                put("kind", "youtube#video")
                put("videoId", videoId)
            })
        }
        jsonBody.put("snippet", snippet)

        connection.outputStream.write(jsonBody.toString().toByteArray())
        connection.outputStream.flush()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Error al agregar el video a la playlist")
        }
    }
    private fun goBack() {
        finish()
    }
}
