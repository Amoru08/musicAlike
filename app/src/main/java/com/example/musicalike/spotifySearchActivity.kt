package com.example.musicalike

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class SpotifySearchActivity : AppCompatActivity() {

    private lateinit var searchField: TextInputEditText
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private val client = OkHttpClient()
    private val lastFmService = LastFmService() // Asegúrate de usar LastFmService

    private val apiKey = "AIzaSyAUkgeL0z1-owXSORm81ckUZqwqhClOImw" // Reemplaza con tu clave de API válida
    private var nextPageToken: String? = null
    private var userEmail: String? = null
    private val firestoreDb = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotify_search)

        userEmail = GoogleSignIn.getLastSignedInAccount(this)?.email
        if (userEmail == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        searchField = findViewById(R.id.agregarCancion)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
        adapter = SongAdapter(mutableListOf(), userEmail!!) { song ->
            onFavoriteClicked(song)
        }
        resultsRecyclerView.layoutManager = LinearLayoutManager(this)
        resultsRecyclerView.adapter = adapter

        val searchButton: Button = findViewById(R.id.searchButton)
        searchButton.setOnClickListener {
            val query = searchField.text.toString()
            if (query.isNotEmpty()) {
                nextPageToken = null
                searchYouTube(query)
            } else {
                Toast.makeText(this, "Por favor, ingresa una canción o artista", Toast.LENGTH_SHORT).show()
            }
        }

        resultsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()

                if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                    val query = searchField.text.toString()
                    if (query.isNotEmpty()) {
                        nextPageToken?.let {
                            searchYouTube(query, it)
                        }
                    }
                }
            }
        })
    }

    private fun searchYouTube(query: String, pageToken: String? = null) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = buildUrl(encodedQuery, pageToken)

        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SpotifySearchActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val videos = parseYouTubeVideos(responseBody)
                        runOnUiThread {
                            if (videos.isEmpty()) {
                                Toast.makeText(this@SpotifySearchActivity, "No se encontraron resultados", Toast.LENGTH_SHORT).show()
                            } else {
                                checkFavorites(videos)
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@SpotifySearchActivity, "Error en la búsqueda: Response vacía", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@SpotifySearchActivity, "Error en la búsqueda: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun buildUrl(query: String, pageToken: String?): String {
        var url = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$query&type=video&videoCategoryId=10&maxResults=20&key=$apiKey"
        pageToken?.let { url += "&pageToken=$it" }
        return url
    }

    private fun parseYouTubeVideos(responseBody: String?): List<Song> {
        val videos = mutableListOf<Song>()
        try {
            val jsonObject = JSONObject(responseBody)
            val itemsArray = jsonObject.getJSONArray("items")
            nextPageToken = jsonObject.optString("nextPageToken")

            for (i in 0 until itemsArray.length()) {
                val video = itemsArray.getJSONObject(i)
                val snippet = video.getJSONObject("snippet")
                val videoTitle = snippet.getString("title")
                val channelTitle = snippet.getString("channelTitle")

                if (isSong(videoTitle)) {
                    val tagsArray = snippet.optJSONArray("tags")
                    val tags = mutableListOf<String>()
                    if (tagsArray != null) {
                        for (j in 0 until tagsArray.length()) {
                            tags.add(tagsArray.getString(j))
                        }
                    }

                    val cleanArtistName = cleanArtistName(channelTitle)
                    val song = Song(videoTitle, cleanArtistName, tags)
                    videos.add(song)
                }
            }
        } catch (e: Exception) {
            Log.e("YouTubeSearch", "Error al parsear la respuesta de YouTube: ${e.message}")
        }

        return videos
    }

    private fun isSong(title: String): Boolean {
        val lowerCaseTitle = title.lowercase()
        return !listOf("lyrics", "karaoke", "#shorts", "lyric", "live", "cover", "festival").any {
            lowerCaseTitle.contains(it)
        }
    }

    private fun cleanArtistName(name: String): String {
        return name.replace("VEVO", "", true)
            .replace("- Topic", "", true)
            .split(",")[0]
            .trim()
    }

    private fun cleanSongName(name: String): String {
        return name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
            .replace(Regex("&\\S*"), "").trim()
    }

    private fun checkFavorites(songs: List<Song>) {
        val userRef = firestoreDb.collection("users").document(userEmail!!).collection("favoritos")

        userRef.get().addOnSuccessListener { documents ->
            val favoriteNames = documents.map { it.getString("Nombre") to it.getString("Artista") }.toSet()

            songs.forEach { song ->
                val parts = song.name.split(" - ")
                if (parts.size > 1) {
                    song.artist = cleanArtistName(parts[0].trim())
                    song.name = cleanSongName(parts[1].trim())
                }
                song.isFavorite = favoriteNames.contains(song.name to song.artist)
            }

            adapter.updateResults(songs)
        }
    }

    private fun onFavoriteClicked(song: Song) {
        userEmail?.let { email ->
            Log.d("SpotifySearchActivity", "Favorite clicked for song: ${song.name}")
            val limitedTags = song.tags.take(10)
            val cleanedSongName = cleanSongName(song.name)

            val favoriteSong = mapOf(
                "Nombre" to cleanedSongName,
                "Artista" to song.artist,
                "Tags" to limitedTags
            )

            firestoreDb.collection("users").document(email).collection("favoritos")
                .add(favoriteSong)
                .addOnSuccessListener {
                    Toast.makeText(this, "Canción guardada como favorita", Toast.LENGTH_SHORT).show()
                    lastFmService.searchTagsBySongAndArtist(cleanedSongName, song.artist) { tags ->
                        Log.d("SpotifySearchActivity", "Tags from Last.fm: $tags")  // Log para verificar los tags
                        if (!tags.isNullOrEmpty()) {
                            val topTags = tags.take(10)
                            firestoreDb.collection("users").document(email).collection("favoritos")
                                .whereEqualTo("Nombre", cleanedSongName)
                                .whereEqualTo("Artista", song.artist)
                                .get()
                                .addOnSuccessListener { documents ->
                                    for (document in documents) {
                                        firestoreDb.collection("users").document(email).collection("favoritos")
                                            .document(document.id)
                                            .update("Tags", topTags)
                                    }
                                }
                        }
                    }
                }
        }
    }
}