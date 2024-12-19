package com.example.musicalike

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import androidx.activity.result.contract.ActivityResultContracts



class SpotifyUserActivity : AppCompatActivity() {

    private val CLIENT_ID = "1d1c94387942461b8bd890e34b4ab6c7"
    private val CLIENT_SECRET = "785cb1c64f1044b0b0597a035e7c8cd8"
    private val REDIRECT_URI = "musicalike://callback"  // Callback URI definido en tu app de Spotify

    private val firestoreDb = FirebaseFirestore.getInstance()

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var backButton: Button

    // Configura el launcher para manejar la redirección
    private val authenticationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (data != null) {
                val uri = data.data
                if (uri != null) {
                    val code = uri.getQueryParameter("code")
                    if (code != null) {
                        exchangeCodeForToken(code)  // Intercambiar el código por un token
                    } else {
                        Toast.makeText(this, "Error: No se recibió el código de autenticación.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotifyuser)

        // Inicialización de vistas
        emailEditText = findViewById(R.id.emailSpotify)
        passwordEditText = findViewById(R.id.passwSpotify)
        loginButton = findViewById(R.id.irHome)
        backButton = findViewById(R.id.atras2)

        // Configuración del botón de inicio de sesión
        loginButton.setOnClickListener {
            authenticateWithSpotify()
        }

        // Manejo del botón de retroceso
        backButton.setOnClickListener {
            finish()  // Termina la actividad actual
        }
    }

    /**
     * Inicia el proceso de autenticación con Spotify
     */
    private fun authenticateWithSpotify() {
        val authUri = Uri.Builder()
            .scheme("https")
            .authority("accounts.spotify.com")
            .appendPath("authorize")
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", "user-library-read playlist-read-private user-read-email")  // Scopes necesarios
            .build()

        // Inicia el navegador con la URL de autenticación
        val intent = Intent(Intent.ACTION_VIEW, authUri)
        authenticationLauncher.launch(intent)
    }

    /**
     * Intercambia el código de autorización por un token de acceso
     */
    private fun exchangeCodeForToken(code: String) {
        val client = OkHttpClient()

        val formBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
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
                    val accessToken = jsonObject.getString("access_token")
                    val refreshToken = jsonObject.optString("refresh_token", "")
                    saveTokens(accessToken, refreshToken)

                    // Una vez obtenemos el access token, obtenemos los detalles del usuario
                    getSpotifyUserDetails(accessToken)  // Obtener detalles del usuario

                    runOnUiThread {
                        Toast.makeText(this@SpotifyUserActivity, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show()
                        navigateToHome()  // Redirige a la pantalla principal
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@SpotifyUserActivity, "Error al obtener el token. Inténtalo nuevamente.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SpotifyUserActivity, "Error en la red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    /**
     * Guarda el token de acceso y el token de actualización en SharedPreferences
     */
    private fun saveTokens(accessToken: String, refreshToken: String) {
        val sharedPreferences = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("access_token", accessToken)
        editor.putString("refresh_token", refreshToken)
        editor.apply()
    }

    /**
     * Obtiene los detalles del usuario de Spotify (por ejemplo, nombre de usuario, id de usuario)
     */
    private fun getSpotifyUserDetails(accessToken: String) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me")  // Endpoint de la API de Spotify para obtener los detalles del usuario
            .header("Authorization", "Bearer $accessToken")  // Usamos el token de acceso en el encabezado
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val jsonObject = JSONObject(responseBody)
                        val userId = jsonObject.getString("id")  // ID del usuario de Spotify
                        val displayName = jsonObject.getString("display_name")  // Nombre de usuario

                        // Guardamos la información del usuario en Firebase
                        saveUserDetailsToFirebase(userId, displayName)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@SpotifyUserActivity, "Error al obtener los detalles del usuario.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SpotifyUserActivity, "Error en la red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
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

    /**
     * Navega a la pantalla principal
     */
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()  // Finaliza la actividad actual
    }
}
