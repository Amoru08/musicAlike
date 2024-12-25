package com.example.musicalike

import android.content.Intent
import android.net.Uri
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

class HomeActivity : AppCompatActivity() {

    private lateinit var textview: TextView
    private lateinit var buton: Button
    private lateinit var buscarButton: Button
    private lateinit var escribe: TextInputEditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var usurSpot: Button

    private lateinit var songAdapter: SongAdapter
    private var songList: MutableList<Song> = mutableListOf()

    // Configuración de Spotify
    private val SCOPE = "user-read-private user-read-email playlist-read-private user-library-read"
    private val CLIENT_ID = "1d1c94387942461b8bd890e34b4ab6c7" // Reemplaza con tu CLIENT_ID
    private val REDIRECT_URI = "musicalike://callback" // Coincide con tu intent-filter en el manifiesto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si el usuario está autenticado
        if (FirebaseAuth.getInstance().currentUser == null) {
            goToPrincipio()
            finish()
            return
        }

        setContentView(R.layout.activity_home)

        // Inicializar vistas
        textview = findViewById(R.id.textView4)
        buton = findViewById(R.id.buttonHome)
        buscarButton = findViewById(R.id.buscar)
        escribe = findViewById(R.id.agregarCancion)
        usurSpot = findViewById(R.id.usuario)
        recyclerView = findViewById(R.id.recyclerViewResults)

        // Configuración del RecyclerView
        songAdapter = SongAdapter(songList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = songAdapter

        // Agregar algunas canciones predeterminadas
        songAdapter.notifyDataSetChanged()

        // Manejar redirección si la app se inicia desde una URI
        val uri = intent?.data
        if (uri != null && uri.scheme == "musicalike" && uri.host == "callback") {
            val authorizationCode = uri.getQueryParameter("code")
            if (authorizationCode != null) {
                // Aquí puedes intercambiar el código por un token de acceso
                Log.d("HomeActivity", "Authorization Code recibido al iniciar: $authorizationCode")
                Toast.makeText(this, "Código recibido al iniciar: $authorizationCode", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: No se recibió el código de autorización", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar el botón buscar para realizar búsqueda
        buscarButton.setOnClickListener {
            val query = escribe.text.toString()
            if (query.isNotEmpty()) {
                val intent = Intent(this, SpotifySearchActivity::class.java)
                intent.putExtra("QUERY", query) // Pasar la consulta como extra
                startActivity(intent)
            } else {
                Toast.makeText(this, "Por favor, escribe algo para buscar", Toast.LENGTH_SHORT).show()
            }
        }

        // Configuración del botón "Atrás"
        buton.setOnClickListener {
            goToPrincipio()
        }

        // Configuración del botón "Conectar Spotify"
        usurSpot.setOnClickListener {
            startSpotifyAuth()
        }
    }

    // Función para iniciar el flujo de autenticación de Spotify
    private fun startSpotifyAuth() {
        /*val authUrl = "https://accounts.spotify.com/authorize" +
                "?client_id=$CLIENT_ID" +
                "&response_type=code" +
                "&redirect_uri=$REDIRECT_URI" +
                "&scope=$SCOPE"*/

        /*val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
        startActivity(intent)*/
        val i = Intent(this, SpotifyUserActivity::class.java)
        startActivity(i)
    }

    // Manejar redirección si la app ya estaba abierta
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = intent?.data
        if (uri != null && uri.scheme == "musicalike" && uri.host == "callback") {
            val authorizationCode = uri.getQueryParameter("code")
            if (authorizationCode != null) {
                Log.d("HomeActivity", "Authorization Code recibido en onNewIntent: $authorizationCode")
                Toast.makeText(this, "Código recibido: $authorizationCode", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: No se recibió el código de autorización", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Navegar a la actividad de inicio
    private fun goToPrincipio() {
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }
}
