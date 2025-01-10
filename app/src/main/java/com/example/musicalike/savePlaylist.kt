package com.example.musicalike

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
    private val songs = mutableListOf<Song>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_details)

        playlistRecyclerView = findViewById(R.id.songsRecyclerView)
        save = findViewById(R.id.saveButton)

        playlistRecyclerView.layoutManager = LinearLayoutManager(this)

        // Obtener el correo del usuario autenticado
        val userEmail = GoogleSignIn.getLastSignedInAccount(this)?.email

        // Si no hay usuario autenticado, muestra un mensaje de error
        if (userEmail == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener el ID de la playlist seleccionada desde el Intent
        val playlistId = intent.getStringExtra("playlistId")
        if (playlistId == null) {
            Toast.makeText(this, "No se ha seleccionado ninguna playlist", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear el adaptador para las canciones
        val adapter = SongAdapter(songs, userEmail, { song ->
            // Lógica para manejar clics en el botón de favorito
            song.isFavorite = !song.isFavorite
            Log.d("PlaylistsActivity", "Canción favorita: ${song.name} - ${song.isFavorite}")
        }) { song ->
            // Lógica para manejar clic en la canción
            Log.d("PlaylistsActivity", "Canción seleccionada: ${song.name}")
        }

        playlistRecyclerView.adapter = adapter

        // Cargar canciones de la playlist seleccionada
        loadSongsFromPlaylist(playlistId, userEmail)

        save.setOnClickListener {
            Toast.makeText(this, "Se ha clickado en el botón de guardar", Toast.LENGTH_SHORT).show()
            saveSongsToYouTube()
        }
    }

    // Cargar canciones desde Firestore usando el playlistId
    private fun loadSongsFromPlaylist(playlistId: String, userEmail: String) {
        firestoreDb.collection("users")
            .document(userEmail)
            .collection("playlists")
            .document(playlistId)  // Usar el ID de la playlist que se pasó
            .get()
            .addOnSuccessListener { document ->
                // Verificar si la playlist contiene el campo 'songs'
                val songsArray = document.get("songs") as? List<Map<String, Any>> ?: return@addOnSuccessListener

                songs.clear()  // Limpiar la lista de canciones antes de agregar las nuevas

                // Recorrer el array de canciones
                for (songMap in songsArray) {
                    val name = songMap["name"] as? String ?: continue
                    val artist = songMap["artist"] as? String ?: continue

                    val song = Song(name = name, artist = artist)
                    songs.add(song)
                }

                // Notificar al adaptador que los datos han cambiado
                playlistRecyclerView.adapter?.notifyDataSetChanged()

                // Mostrar mensaje si no hay canciones
                if (songs.isEmpty()) {
                    Toast.makeText(this, "No hay canciones en la playlist seleccionada", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar canciones: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Lógica para subir las canciones a YouTube
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

        // Obtener el token de acceso de Google
        val account = GoogleSignIn.getLastSignedInAccount(this)
        val accessToken = account?.serverAuthCode

        if (accessToken == null) {
            Toast.makeText(this, "No se pudo obtener el token de acceso", Toast.LENGTH_SHORT).show()
            return
        }

        // Usamos un Coroutine para la operación en segundo plano
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val songsToUpload = mutableListOf<Song>()

                // Cargar las canciones de Firestore
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

                // Crear una playlist en YouTube si no existe (puedes usar una playlist existente)
                val youtubePlaylistId = createYouTubePlaylist(accessToken, "Mi Playlist desde Musicalike", "Playlist creada automáticamente desde Musicalike")

                if (youtubePlaylistId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PlaylistsActivity, "Error al crear la playlist en YouTube", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Buscar y agregar videos a la playlist de YouTube
                for (song in songsToUpload) {
                    val videoId = searchYouTubeVideo(apiKey = "AIzaSyAUkgeL0z1-owXSORm81ckUZqwqhClOImw", query = "${song.name} ${song.artist}")
                    if (videoId != null) {
                        addVideoToPlaylist(accessToken = accessToken, playlistId = youtubePlaylistId, videoId = videoId)
                    }
                }

                // Mensaje final
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

    // Crear la playlist en YouTube
    suspend fun createYouTubePlaylist(accessToken: String, title: String, description: String): String? {
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

    // Buscar el video en YouTube
    suspend fun searchYouTubeVideo(apiKey: String, query: String): String? {
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
                return id.getString("videoId") // Devuelve el ID del video encontrado
            }
        }

        return null
    }

    // Agregar el video a la playlist de YouTube
    suspend fun addVideoToPlaylist(accessToken: String, playlistId: String, videoId: String) {
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
}
