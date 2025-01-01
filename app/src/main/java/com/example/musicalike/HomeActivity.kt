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
    //private lateinit var escribe: TextInputEditText
    private lateinit var usurSpot: Button

    private lateinit var songAdapter: SongAdapter
    private var songList: MutableList<Song> = mutableListOf()

    private val SCOPE = "user-read-private user-read-email playlist-read-private user-library-read"
    private val CLIENT_ID = "1d1c94387942461b8bd890e34b4ab6c7"
    private val REDIRECT_URI = "musicalike://callback"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            goToPrincipio()
            finish()
            return
        }

        setContentView(R.layout.activity_home)

        textview = findViewById(R.id.textView4)
        buton = findViewById(R.id.buttonHome)
        buscarButton = findViewById(R.id.buscar)
        usurSpot = findViewById(R.id.usuario)

        songAdapter = SongAdapter(songList)
        songAdapter.notifyDataSetChanged()

        val uri = intent?.data
        if (uri != null && uri.scheme == "musicalike" && uri.host == "callback") {
            val authorizationCode = uri.getQueryParameter("code")
            if (authorizationCode != null) {
                Log.d("HomeActivity", "Authorization Code recibido al iniciar: $authorizationCode")
                Toast.makeText(this, "Código recibido al iniciar: $authorizationCode", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: No se recibió el código de autorización", Toast.LENGTH_SHORT).show()
            }
        }

        buscarButton.setOnClickListener {
                val intent = Intent(this, SpotifySearchActivity::class.java)
                startActivity(intent)

        }

        buton.setOnClickListener {
            goToPrincipio()
        }

        usurSpot.setOnClickListener {
            startSpotifyAuth()
        }
    }

    private fun startSpotifyAuth() {
        val i = Intent(this, SpotifyUserActivity::class.java)
        startActivity(i)
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
