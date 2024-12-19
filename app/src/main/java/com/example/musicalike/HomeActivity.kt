package com.example.musicalike

import android.content.Intent
import android.os.Bundle
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
    private lateinit var usurSpot: Button // Botón para ir a SpotifyUserActivity

    private lateinit var songAdapter: SongAdapter
    private var songList: MutableList<String> = mutableListOf()

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

        // Agregar algunas canciones predeterminadas para probar
        songList.add("Song 1")
        songList.add("Song 2")
        songList.add("Song 3")
        songAdapter.notifyDataSetChanged()

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

        // Configuración del botón "Atras"
        buton.setOnClickListener {
            goToPrincipio()
        }

        // Configuración del botón "Add Spotify User"
        usurSpot.setOnClickListener {
            val intent = Intent(this, SpotifyUserActivity::class.java)
            startActivity(intent)
        }
    }

    private fun goToPrincipio() {
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }
}
