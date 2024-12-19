package com.example.musicalike

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import java.io.IOException

class HomeActivity : AppCompatActivity() {

    private val clientId = "1d1c94387942461b8bd890e34b4ab6c7" // Spotify Client ID
    private val clientSecret = "785cb1c64f1044b0b0597a035e7c8cd8" // Spotify Client Secret
    private val redirectUri = "musicalike://callback" // Spotify Redirect URI
    private val state = "random_state_123" // CSRF Protection State
    private val authUrl = "https://accounts.spotify.com/authorize"
    private val tokenUrl = "https://accounts.spotify.com/api/token"
    private val scope = "user-library-read playlist-read-private user-read-email"

    private val firestoreDb = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Si ya hay un token de acceso, no redirigir a Spotify
        val sharedPreferences = getSharedPreferences("SpotifyAuth", MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("access_token", null)

        if (accessToken != null) {
            // Si ya hay un token, puedes continuar con el flujo normal de la app
            fetchSpotifyUserData(accessToken)
        } else {
            // Redirigir al usuario a Spotify para autorización
            val authorizationUrl = "$authUrl?client_id=$clientId" +
                    "&response_type=code" +
                    "&redirect_uri=$redirectUri" +
                    "&state=$state" +
                    "&scope=$scope"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl))
            startActivity(intent)
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = intent.data

        // Verifica si la URI recibida es la correcta
        if (uri != null && uri.toString().startsWith(redirectUri)) {
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")

            if (error != null) {
                Log.e("SpotifyAuth", "Error during Spotify authorization: $error")
                Toast.makeText(this, "Authorization failed: $error", Toast.LENGTH_SHORT).show()
            } else if (code != null) {
                Log.d("SpotifyAuth", "Authorization successful, code: $code")
                // Si todo es correcto, inicia el flujo para obtener el token de acceso
                getAccessToken(code)
            } else {
                Log.e("SpotifyAuth", "No authorization code received")
                Toast.makeText(this, "No authorization code received", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("SpotifyAuth", "Intent URI is null or does not match redirect URI")
        }
    }

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

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, "Failed to fetch access token: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                Log.e("SpotifyAuth", "Failed to fetch access token: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val accessToken = responseBody?.let {
                        val json = Gson().fromJson(it, JsonObject::class.java)
                        json.get("access_token")?.asString
                    }

                    if (accessToken != null) {
                        fetchSpotifyUserData(accessToken)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "Error fetching access token: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("SpotifyAuth", "Error in token response: ${response.message}")
                }
            }
        })
    }


    private fun fetchSpotifyUserData(accessToken: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me")
            .header("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("SpotifyUser", "Failed to fetch user data: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val userJson = Gson().fromJson(responseBody, JsonObject::class.java)

                    // Extraer datos del usuario
                    val userId = userJson.get("id")?.asString ?: "unknown"
                    val email = userJson.get("email")?.asString ?: "unknown"
                    val displayName = userJson.get("display_name")?.asString ?: "unknown"

                    Log.d("SpotifyUser", "User ID: $userId, Email: $email, Display Name: $displayName")

                    // Guardar en Firebase
                    saveUserDataToFirebase(userId, email, displayName)
                } else {
                    Log.e("SpotifyUser", "Error in user data response: ${response.message}")
                }
            }
        })
    }

    private fun saveUserDataToFirebase(userId: String, email: String, displayName: String) {
        val userCollection = firestoreDb.collection("users")
        val userData = hashMapOf(
            "email" to email,
            "displayName" to displayName
        )

        userCollection.document(userId).set(userData)
            .addOnSuccessListener {
                Log.d("Firebase", "User data saved successfully")
                runOnUiThread {
                    Toast.makeText(this, "User data saved!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error saving user data: ${e.message}")
            }
    }
}
