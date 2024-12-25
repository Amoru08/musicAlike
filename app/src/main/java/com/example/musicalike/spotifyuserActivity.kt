package com.example.musicalike

import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.*

class SpotifyUserActivity : AppCompatActivity() {

    private val CLIENT_ID = "1d1c94387942461b8bd890e34b4ab6c7"
    private val CLIENT_SECRET = "785cb1c64f1044b0b0597a035e7c8cd8"
    private val REDIRECT_URI = "musicalike://callback"
    private val firestoreDb = FirebaseFirestore.getInstance()
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var backButton: Button

    // Configura el launcher para manejar la redirección
    private val authenticationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val intent = result.data
                val code = intent?.getStringExtra("code") // Spotify auth code
                if (code != null) {
                    // Intercambia el código por un token de acceso
                    getAccessToken(code)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotifyuser)

        // Inicializa las vistas
        emailEditText = findViewById(R.id.emailSpotify)
        passwordEditText = findViewById(R.id.passwSpotify)
        loginButton = findViewById(R.id.authButton)
        backButton = findViewById(R.id.detalles)

        loginButton.setOnClickListener {
            initiateSpotifyLogin()
        }

        backButton.setOnClickListener {
            // Código para manejar la acción de regresar
            finish()
        }
    }

    /**
     * Inicia el flujo de autenticación de Spotify
     */
    private fun initiateSpotifyLogin() {
        val authUrl = "https://accounts.spotify.com/authorize?" +
                "client_id=$CLIENT_ID" +
                "&response_type=code" +
                "&redirect_uri=$REDIRECT_URI" +
                "&scope=user-read-private user-read-email" // Los permisos necesarios

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
        authenticationLauncher.launch(intent)
    }

    /**
     * Intercambia el código de autorización por un token de acceso
     */
    private fun getAccessToken(authCode: String) {
        // Realiza la solicitud HTTP de manera asíncrona utilizando coroutines
        CoroutineScope(Dispatchers.IO).launch {
            // Configura el interceptor de logging
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()

            val body = FormBody.Builder()
                .add("code", authCode)
                .add("redirect_uri", REDIRECT_URI)
                .add("grant_type", "authorization_code")
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .build()

            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .post(body)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    val jsonObject = JSONObject(responseData)
                    val accessToken = jsonObject.getString("access_token")

                    // Llama para obtener detalles del usuario
                    fetchUserDetails(accessToken)
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SpotifyUserActivity, "Error al intercambiar el código", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SpotifyUserActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Obtiene los detalles del usuario desde la API de Spotify
     */
    private fun fetchUserDetails(accessToken: String) {
        // Realiza la solicitud HTTP de manera asíncrona utilizando coroutines
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()

            val request = Request.Builder()
                .url("https://api.spotify.com/v1/me") // Endpoint para obtener los datos del usuario
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    val jsonObject = JSONObject(responseData)
                    val userId = jsonObject.getString("id")  // ID del usuario de Spotify
                    val displayName = jsonObject.getString("display_name")  // Nombre de usuario

                    // Guardamos la información del usuario en Firebase
                    saveUserDetailsToFirebase(userId, displayName)
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SpotifyUserActivity, "Error al obtener los detalles", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SpotifyUserActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Guarda los detalles del usuario en la base de datos de Firebase
     */
    private fun saveUserDetailsToFirebase(userId: String, displayName: String) {
        val userRef = firestoreDb.collection("SpotifyUser").document(userId)
        val userData = hashMapOf(
            "userId" to userId,
            "displayName" to displayName
        )
        userRef.set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Usuario guardado en Firebase.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar el usuario: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
