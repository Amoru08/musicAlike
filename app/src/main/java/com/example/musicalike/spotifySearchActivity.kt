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

    private var accessToken: String? = null // Token de acceso (se almacenará después de la autenticación)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotify_search)

        searchField = findViewById(R.id.agregarCancion)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
        adapter = SongAdapter(mutableListOf())
        resultsRecyclerView.layoutManager = LinearLayoutManager(this)
        resultsRecyclerView.adapter = adapter

        val query = intent.getStringExtra("QUERY")
        if (!query.isNullOrEmpty()) {
            searchSpotify(query)
            searchField.setText(query)
        }
    }

    // Función para realizar la búsqueda en Spotify
    private fun searchSpotify(query: String) {
        if (accessToken == null) {
            // Si el accessToken es nulo, intenta obtenerlo nuevamente (puede ser un error 401 si ha caducado)
            refreshAccessToken()
        }

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
        val sharedPreferences = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
        val refreshToken = sharedPreferences.getString("refresh_token", null)

        if (refreshToken != null) {
            val formBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", "YOUR_CLIENT_ID") // Reemplaza con tu CLIENT_ID
                .add("client_secret", "YOUR_CLIENT_SECRET") // Reemplaza con tu CLIENT_SECRET
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
                        accessToken = jsonObject.getString("access_token")
                        // Actualiza el token en las preferencias compartidas
                        val editor = sharedPreferences.edit()
                        editor.putString("access_token", accessToken)
                        editor.apply()

                        // Ahora vuelve a realizar la búsqueda con el nuevo token
                        val query = searchField.text.toString()
                        if (query.isNotEmpty()) {
                            searchSpotify(query)
                        }
                    } else {
                        Log.e("SpotifySearch", "Error al renovar el token: ${response.code}")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    Log.e("SpotifySearch", "Error de red al renovar token: ${e.message}")
                }
            })
        } else {
            Toast.makeText(this, "No se encontró el refresh token", Toast.LENGTH_SHORT).show()
        }
    }

}
