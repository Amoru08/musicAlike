package com.example.musicalike

import android.os.Bundle
import android.util.Log
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
    private var refreshToken: String? = null // Token de actualización

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotify_search)

        searchField = findViewById(R.id.agregarCancion)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
        adapter = SongAdapter(mutableListOf())
        resultsRecyclerView.layoutManager = LinearLayoutManager(this)
        resultsRecyclerView.adapter = adapter

        // Obtener el query y los tokens de la Intent
        val query = intent.getStringExtra("QUERY")
        accessToken = intent.getStringExtra("ACCESS_TOKEN")
        refreshToken = intent.getStringExtra("REFRESH_TOKEN")

        if (!query.isNullOrEmpty()) {
            searchSpotify(query)
            searchField.setText(query)
        }
    }

    // Función para realizar la búsqueda en Spotify
    private fun searchSpotify(query: String) {
        if (accessToken == null) {
            // Si el accessToken es nulo, intenta obtenerlo nuevamente
            refreshAccessToken()
        }

        if (accessToken != null) { // Asegúrate de que el token no sea nulo
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.spotify.com/v1/search?q=$encodedQuery&type=track&limit=10"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken") // Asegúrate de usar el token de acceso en la cabecera
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
                        if (response.code == 401) {
                            // Si el código de respuesta es 401, significa que el token ha caducado. Intenta renovarlo.
                            runOnUiThread {
                                Toast.makeText(this@SpotifySearchActivity, "Token expirado, renovando...", Toast.LENGTH_SHORT).show()
                            }
                            refreshAccessToken()  // Intenta renovar el token
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@SpotifySearchActivity, "Error en la búsqueda: ${response.code}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            })
        } else {
            runOnUiThread {
                Toast.makeText(this@SpotifySearchActivity, "No se pudo obtener un token de acceso", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función para parsear la respuesta y extraer los resultados de las canciones
    private fun parseTracks(responseBody: String?): List<String> {
        val results = mutableListOf<String>()
        try {
            val json = JSONObject(responseBody)
            val tracks = json.getJSONObject("tracks").getJSONArray("items")
            for (i in 0 until tracks.length()) {
                val track = tracks.getJSONObject(i)
                val trackName = track.getString("name")
                val artistName = track.getJSONArray("artists").getJSONObject(0).getString("name")
                results.add("$trackName - $artistName")
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Error al procesar resultados: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        return results
    }

    // Función para renovar el token de acceso
    private fun refreshAccessToken() {
        if (refreshToken != null) {
            val formBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken!!)
                .add("client_id", "1d1c94387942461b8bd890e34b4ab6c7") // Reemplaza con tu CLIENT_ID
                .add("client_secret", "785cb1c64f1044b0b0597a035e7c8cd8") // Reemplaza con tu CLIENT_SECRET
                .build()

            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .post(formBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val jsonObject = JSONObject(responseBody)
                        val newAccessToken = jsonObject.getString("access_token")

                        // Actualiza el accessToken y renueva la búsqueda
                        accessToken = newAccessToken
                        runOnUiThread {
                            // Actualiza el token almacenado, y vuelve a realizar la búsqueda
                            Toast.makeText(this@SpotifySearchActivity, "Token renovado", Toast.LENGTH_SHORT).show()
                            val query = searchField.text.toString()
                            if (query.isNotEmpty()) {
                                searchSpotify(query)
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@SpotifySearchActivity, "Error al renovar el token: ${response.code}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@SpotifySearchActivity, "Error de red al renovar token: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } else {
            runOnUiThread {
                Toast.makeText(this@SpotifySearchActivity, "No se encontró el refresh token", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
