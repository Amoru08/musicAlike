package com.example.musicalike

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class SpotifyDetailsActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotify_detail)

        // Inicialización de vistas
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)

        // Intentar obtener los detalles del usuario desde SharedPreferences
        val (userId, displayName) = getUserDetails()

        // Verificar si los detalles están disponibles
        if (userId.isNotEmpty() && displayName.isNotEmpty()) {
            // Si los detalles del usuario están guardados, mostrar en la interfaz de usuario
            tvUserName.text = "Nombre: $displayName"
            tvUserEmail.text = "ID de Usuario: $userId"
        } else {
            // Si no se encuentran detalles en SharedPreferences, intentar obtenerlos de la API
            val sharedPreferences: SharedPreferences = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
            val accessToken = sharedPreferences.getString("access_token", null)

            if (accessToken != null) {
                fetchSpotifyUserDetails(accessToken)
            } else {
                Toast.makeText(this, "Token de acceso no encontrado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Realiza una solicitud a la API de Spotify para obtener los detalles del usuario.
     */
    private fun fetchSpotifyUserDetails(accessToken: String) {
        val client = OkHttpClient()

        // Crear la solicitud para obtener el perfil del usuario
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me") // Endpoint para obtener los detalles del usuario
            .addHeader("Authorization", "Bearer $accessToken") // Incluir el token de acceso en la cabecera
            .build()

        // Ejecutar la solicitud en segundo plano
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    // Parsear la respuesta JSON
                    val jsonObject = JSONObject(responseBody)
                    val userName = jsonObject.getString("display_name") // Nombre del usuario
                    val userEmail = jsonObject.getString("email") // Correo electrónico del usuario
                    val userId = jsonObject.getString("id") // ID del usuario

                    // Guardar los detalles del usuario en SharedPreferences
                    saveUserDetails(userId, userName)

                    // Actualizar la interfaz de usuario en el hilo principal
                    runOnUiThread {
                        tvUserName.text = "Nombre: $userName"
                        tvUserEmail.text = "Email: $userEmail"
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@SpotifyDetailsActivity, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // En caso de fallo en la solicitud
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SpotifyDetailsActivity, "Error en la solicitud: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    /**
     * Guarda los detalles del usuario (user_id, display_name) en SharedPreferences
     */
    private fun saveUserDetails(userId: String, displayName: String) {
        val sharedPreferences = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("user_id", userId)
        editor.putString("display_name", displayName)
        editor.apply()  // Guarda los datos en SharedPreferences
    }

    /**
     * Obtiene los detalles del usuario desde SharedPreferences
     */
    private fun getUserDetails(): Pair<String, String> {
        val sharedPreferences = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getString("user_id", "") ?: ""
        val displayName = sharedPreferences.getString("display_name", "") ?: ""
        return Pair(userId, displayName)
    }
}
