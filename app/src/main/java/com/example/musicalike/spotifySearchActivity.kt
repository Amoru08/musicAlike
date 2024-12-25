package com.example.musicalike

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class SpotifySearchActivity : AppCompatActivity() {

    private lateinit var searchField: TextInputEditText
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private val client = OkHttpClient()

    private val apiKey = "AIzaSyAUkgeL0z1-owXSORm81ckUZqwqhClOImw"  // Reemplaza con tu clave de API de YouTube
    private var nextPageToken: String? = null  // Para paginación de resultados

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotify_search)

        searchField = findViewById(R.id.agregarCancion)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
        adapter = SongAdapter(mutableListOf())
        resultsRecyclerView.layoutManager = LinearLayoutManager(this)
        resultsRecyclerView.adapter = adapter

        // Configurar el botón de búsqueda
        val searchButton: Button = findViewById(R.id.searchButton)
        searchButton.setOnClickListener {
            val query = searchField.text.toString()
            if (query.isNotEmpty()) {
                nextPageToken = null  // Reiniciar la paginación al buscar una nueva canción
                searchYouTube(query)
            } else {
                Toast.makeText(this, "Por favor, ingresa una canción o artista", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar el scroll listener para cargar más resultados cuando el usuario llegue al final
        resultsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()

                if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                    // Se ha llegado al final de la lista, cargar más resultados
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

    // Función para realizar la búsqueda en YouTube
    private fun searchYouTube(query: String, pageToken: String? = null) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = buildUrl(encodedQuery, pageToken)

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SpotifySearchActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val videos = parseYouTubeVideos(responseBody)
                    runOnUiThread {
                        if (videos.isEmpty()) {
                            Toast.makeText(this@SpotifySearchActivity, "No se encontraron resultados", Toast.LENGTH_SHORT).show()
                        } else {
                            adapter.updateResults(videos)
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

    // Construir la URL de búsqueda con el parámetro `maxResults` y `pageToken`
    private fun buildUrl(query: String, pageToken: String?): String {
        var url = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$query&type=video&videoCategoryId=10&maxResults=20&key=$apiKey"
        pageToken?.let {
            url += "&pageToken=$it"  // Agregar el nextPageToken para paginación
        }
        return url
    }

    // Función para parsear la respuesta JSON de YouTube
    private fun parseYouTubeVideos(responseBody: String?): List<Song> {
        val videos = mutableListOf<Song>()

        try {
            val jsonObject = JSONObject(responseBody)
            val itemsArray = jsonObject.getJSONArray("items")
            nextPageToken = jsonObject.optString("nextPageToken")  // Guardar el nextPageToken para la siguiente búsqueda

            for (i in 0 until itemsArray.length()) {
                val video = itemsArray.getJSONObject(i)
                val snippet = video.getJSONObject("snippet")
                val videoTitle = snippet.getString("title")  // Título del video
                val channelTitle = snippet.getString("channelTitle")  // Nombre del canal

                // Obtener etiquetas (si existen)
                val tagsArray = snippet.optJSONArray("tags")
                val tags = mutableListOf<String>()
                if (tagsArray != null) {
                    for (j in 0 until tagsArray.length()) {
                        tags.add(tagsArray.getString(j))
                    }
                }

                // Crear un objeto Song y agregarlo a la lista
                val song = Song(videoTitle, channelTitle, tags)  // Asumiendo "YouTube" como el nombre del artista y las etiquetas
                videos.add(song)
            }
        } catch (e: Exception) {
            Log.e("YouTubeSearch", "Error al parsear la respuesta de YouTube: ${e.message}")
        }

        return videos
    }


}
