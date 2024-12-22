package com.example.musicalike

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class SpotifyUserActivity : AppCompatActivity() {

    // Elementos de la UI
    private lateinit var emailSpotify: EditText
    private lateinit var passwSpotify: EditText
    private lateinit var detailsButton: Button

    // Tokens de acceso y refresco
    private var accessToken: String? = null

    // Spotify OAuth2.0 Configuración
    private val CLIENT_ID = "1d1c94387942461b8bd890e34b4ab6c7" // Obténlo desde el panel de Spotify Developer
    private val CLIENT_SECRET = "785cb1c64f1044b0b0597a035e7c8cd8" // Obténlo desde el panel de Spotify Developer
    private val REDIRECT_URI = "musicalike://callback" // Asegúrate de configurarlo en Spotify Developer
    private val AUTH_URL = "https://accounts.spotify.com/authorize"
    private val TOKEN_URL = "https://accounts.spotify.com/api/token"
    private val SCOPE = "user-read-private user-read-email playlist-read-private"

    // Cliente HTTP reutilizable
    private val httpClient: OkHttpClient by lazy { OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotifyuser)

        emailSpotify = findViewById(R.id.emailSpotify)
        passwSpotify = findViewById(R.id.passwSpotify)
        detailsButton = findViewById(R.id.details)

        // Botón para iniciar el proceso de autorización
        detailsButton.setOnClickListener {
            startSpotifyAuth()
        }
    }

    // Inicia el flujo de autorización hacia Spotify
    private fun startSpotifyAuth() {
        val authUrl = "$AUTH_URL?client_id=$CLIENT_ID&response_type=code&redirect_uri=$REDIRECT_URI&scope=$SCOPE"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
        startActivity(intent)
    }

    // Captura el código de autorización al regresar a tu aplicación
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            val uri = data?.data
            val code = uri?.getQueryParameter("code") // Código de autorización

            Log.d("SpotifyAuth", "Authorization Code: $code")

            if (code != null) {
                exchangeCodeForAccessToken(code)
            } else {
                Toast.makeText(this, "Error al obtener el código de autorización", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Intercambia el código de autorización por un access token
    private fun exchangeCodeForAccessToken(code: String) {
        val formBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .build()

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(formBody)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SpotifyAuth", "Error al obtener el token: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    val jsonObject = JSONObject(responseData)

                    val accessToken = jsonObject.getString("access_token")
                    val refreshToken = jsonObject.optString("refresh_token")

                    Log.d("SpotifyAuth", "Access Token: $accessToken")
                    Log.d("SpotifyAuth", "Refresh Token: $refreshToken")

                    saveAccessToken(accessToken, jsonObject.getInt("expires_in"))
                    if (!refreshToken.isNullOrEmpty()) saveRefreshToken(refreshToken)
                } else {
                    Log.e("SpotifyAuth", "Error en el intercambio de código: ${response.message}")
                }
            }
        })
    }

    // Guarda el access token en SharedPreferences
    // Guarda el access token en SharedPreferences
    private fun saveAccessToken(token: String, expiresIn: Int) {
        val sharedPreferences = getSharedPreferences("spotify_prefs", MODE_PRIVATE)
        val expirationTime = System.currentTimeMillis() + (expiresIn * 1000) // Convierte segundos a milisegundos
        sharedPreferences.edit()
            .putString("access_token", token)
            .putLong("expiration_time", expirationTime)
            .apply()

        // Log para verificar que se guardó correctamente
        val savedToken = sharedPreferences.getString("access_token", null)
        Log.d("SpotifyAuth", "Access token guardado: $savedToken")
    }


    // Guarda el refresh token en SharedPreferences
    private fun saveRefreshToken(token: String) {
        Log.d("SpotifyAuth", "Guardando refresh token: $token")
        val sharedPreferences = getSharedPreferences("spotify_prefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("refresh_token", token).apply()
    }

    // Verifica si el token aún es válido
    // Verifica si el token aún es válido
    private fun isTokenValid(): Boolean {
        val sharedPreferences = getSharedPreferences("spotify_prefs", MODE_PRIVATE)
        val expirationTime = sharedPreferences.getLong("expiration_time", 0)
        val isValid = System.currentTimeMillis() < expirationTime
        Log.d("SpotifyAuth", "Token válido: $isValid")
        return isValid
    }


    // Refresca el access token si es necesario
    private fun ensureValidTokenAndProceed(action: () -> Unit) {
        if (isTokenValid()) {
            action()
        } else {
            refreshAccessToken {
                action()
            }
        }
    }

    // Refresca el token y ejecuta una acción después
    private fun refreshAccessToken(onSuccess: () -> Unit) {
        val refreshToken = getRefreshTokenFromPreferences()

        if (refreshToken.isNullOrEmpty()) {
            runOnUiThread {
                Toast.makeText(this, "Refresh token no encontrado, por favor autentícate nuevamente.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val formBody = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .build()

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(formBody)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SpotifyUserActivity, "Error al refrescar el token: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.d("SpotifyAuth", "Respuesta completa: $responseData")

                if (response.isSuccessful) {
                    val jsonObject = JSONObject(responseData)
                    val accessToken = jsonObject.getString("access_token")
                    val refreshToken = jsonObject.optString("refresh_token") // Podría estar vacío

                    Log.d("SpotifyAuth", "Access Token: $accessToken")
                    Log.d("SpotifyAuth", "Refresh Token: $refreshToken")

                    saveAccessToken(accessToken, jsonObject.getInt("expires_in"))
                    if (!refreshToken.isNullOrEmpty()) {
                        saveRefreshToken(refreshToken)
                    } else {
                        Log.e("SpotifyAuth", "El servidor no devolvió un refresh token.")
                    }
                } else {
                    Log.e("SpotifyAuth", "Error en el intercambio de código: ${response.code} ${response.message}")
                }
            }
        })
    }

    private fun getRefreshTokenFromPreferences(): String? {
        val sharedPreferences = getSharedPreferences("spotify_prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("refresh_token", null)
        Log.d("SpotifyAuth", "Refresh token recuperado: $token")
        return token
    }
}
