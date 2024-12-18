package com.example.musicalike

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import java.io.IOException

class HomeActivity : AppCompatActivity() {

    private val clientId = "1d1c94387942461b8bd890e34b4ab6c7"  // Tu client_id
    private val clientSecret = "785cb1c64f1044b0b0597a035e7c8cd8"  // Tu client_secret
    private val redirectUri = "musicalike://callback"  // Tu redirect_uri
    private val state = "some_random_state"  // Estado para protección CSRF

    private val authUrl = "https://accounts.spotify.com/authorize"
    private val tokenUrl = "https://accounts.spotify.com/api/token"
    private val scope = "user-library-read playlist-read-private user-read-email"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Redirigir al usuario a la página de autorización de Spotify
        val authorizationUrl = "$authUrl?client_id=$clientId" +
                "&response_type=code" +
                "&redirect_uri=$redirectUri" +
                "&state=$state" +
                "&scope=$scope"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl))
        startActivity(intent)
    }

    // Este método se llama cuando Spotify redirige al usuario a tu redirectUri
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = intent.data

        // Verifica si la redirección contiene el código de autorización
        if (uri != null && uri.toString().startsWith(redirectUri)) {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                // Llamar a la función para obtener el access token usando el código
                getAccessToken(code)
            }
        }
    }

    // Función para obtener el access token usando el código de autorización
    private fun getAccessToken(code: String) {
        val client = OkHttpClient()

        val formBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", redirectUri)
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .build()

        val request = Request.Builder()
            .url(tokenUrl)
            .post(formBody)
            .build()

        // Hacer la solicitud para obtener el token
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val accessToken = responseBody?.let {
                        val json = Gson().fromJson(it, JsonObject::class.java)
                        json.get("access_token")?.asString
                    }

                    if (accessToken != null) {
                        makeSpotifyApiRequest(accessToken)
                    }
                }
            }
        })
    }

    private fun makeSpotifyApiRequest(accessToken: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me")
            .header("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    println("Response: $responseBody")
                }
            }
        })
    }
}

