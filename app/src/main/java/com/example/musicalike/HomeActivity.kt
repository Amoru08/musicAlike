package com.example.musicalike

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var textview: TextView
    private lateinit var back: Button
    private lateinit var search: Button
    private lateinit var user: Button
    private lateinit var playlists: Button
    private lateinit var favorites: Button

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            goToPrincipio()
            finish()
            return
        }

        setContentView(R.layout.activity_home)

        textview = findViewById(R.id.textView4)
        back = findViewById(R.id.buttonHome)
        search = findViewById(R.id.buscar)
        user = findViewById(R.id.usuario)
        playlists = findViewById(R.id.playlists)
        favorites = findViewById(R.id.favorites)

        search.setOnClickListener {
           goToBuscar()
        }

        back.setOnClickListener {
            goToPrincipio()
        }

        user.setOnClickListener {
            startSpotifyAuth()
        }

        playlists.setOnClickListener {
            goToPlaylist()
        }

        favorites.setOnClickListener {
            goToFavorites()
        }

    }

    private fun startSpotifyAuth() {
        val i = Intent(this, SpotifyUserActivity::class.java)
        startActivity(i)
    }
    private fun goToPlaylist() {
        val i = Intent(this, viewPlaylistActivity::class.java)
        startActivity(i)
    }
    private fun goToFavorites() {
        val i = Intent(this, ViewFavoritesActivity::class.java)
        startActivity(i)
    }
    private fun goToBuscar(){
        val intent = Intent(this, SpotifySearchActivity::class.java).apply {
            putExtra("USER_EMAIL", auth.currentUser?.email)
        }
        startActivity(intent)
    }

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

    private fun goToPrincipio() {
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }
}