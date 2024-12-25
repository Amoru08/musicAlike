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

    private var accessToken: String? = null // Token de acceso

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotify_search) // Asegúrate de que este sea el nombre correcto de tu archivo XML

        searchField = findViewById(R.id.agregarCancion)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
        adapter = SongAdapter(mutableListOf())
        resultsRecyclerView.layoutManager = LinearLayoutManager(this)
        resultsRecyclerView.adapter = adapter

        // Obtener el token de acceso de la Intent
        accessToken = intent.getStringExtra("ACCESS_TOKEN")

        // Configurar el botón de búsqueda
        val searchButton: Button = findViewById(R.id.searchButton) // Asegúrate de que este botón esté en tu XML
        searchButton.setOnClickListener {
            val query = searchField.text.toString()
            if (query.isNotEmpty()) {
                searchSpotify(query)
            } else {
                Toast.makeText(this, "Por favor, ingresa una canción o artista", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función para realizar la búsqueda en Spotify
    private fun searchSpotify(query: String) {
        if (accessToken != null) {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.spotify.com/v1/search?q=$encodedQuery&type=track&limit=10"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken") // Usar el token de acceso en la cabecera
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
                        val tracks = parseTracks(responseBody)
                        runOnUiThread {
                            if (tracks.isEmpty()) {
                                Toast.makeText(this@SpotifySearchActivity, "No se encontraron resultados", Toast.LENGTH_SHORT).show()
                            } else {
                                adapter.updateResults(tracks)
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@SpotifySearchActivity, "Error en la búsqueda: ${response.code}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } else {
            runOnUiThread {
                Toast.makeText(this, "Token de acceso no disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función para parsear la respuesta JSON de Spotify
    private fun parseTracks(responseBody: String?): List<Song> {
        val tracks = mutableListOf<Song>()

        try {
            val jsonObject = JSONObject(responseBody)
            val tracksArray = jsonObject.getJSONObject("tracks").getJSONArray("items")

            for (i in 0 until tracksArray.length()) {
                val track = tracksArray.getJSONObject(i)
                val trackName = track.getString("name") // Nombre de la canción
                val artistName = track.getJSONArray("artists").getJSONObject(0).getString("name") // Nombre del primer artista
                val trackUrl = track.getString("external_urls") // URL para escuchar la canción en Spotify

                // Crear un objeto Song y agregarlo a la lista
                val song = Song(trackName, artistName, trackUrl)
                tracks.add(song)
            }
        } catch (e: Exception) {
            Log.e("SpotifySearch", "Error al parsear la respuesta de Spotify: ${e.message}")
        }

        return tracks
    }
}
