package com.example.musicalike

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var textview: TextView
    private lateinit var back: Button
    private lateinit var search: Button
    private lateinit var user: Button
    private lateinit var playlists: Button
    private lateinit var favorites: Button
    private lateinit var personalizationUser: ImageButton

    private lateinit var profileImageView: ImageView
    private lateinit var userNameTextView: TextView

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            goToPrincipio()
            finish()
            return
        }

        setContentView(R.layout.activity_home)

        // Inicializar vistas
        personalizationUser = findViewById(R.id.user)
        textview = findViewById(R.id.textView4)
        back = findViewById(R.id.buttonHome)
        search = findViewById(R.id.buscar)
        user = findViewById(R.id.usuario)
        playlists = findViewById(R.id.playlists)
        favorites = findViewById(R.id.favorites)

        profileImageView = findViewById(R.id.profileImage)
        userNameTextView = findViewById(R.id.userName)

        // Cargar datos guardados de SharedPreferences
        loadUserProfile()

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
        personalizationUser.setOnClickListener {
            goToPersonalizationUser()
        }

        // Mostrar descripción en cuadro de diálogo al hacer clic en la imagen de perfil
        profileImageView.setOnClickListener {
            showDescriptionDialog()
        }
    }

    private fun loadUserProfile() {
        val sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("userName", "Usuario")
        val profileImageUriString = sharedPreferences.getString("profileImageUri", null)

        // Mostrar datos en la interfaz
        userNameTextView.text = userName

        if (profileImageUriString != null) {
            try {
                val profileImageUri = Uri.parse(profileImageUriString)
                val inputStream = contentResolver.openInputStream(profileImageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                profileImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                profileImageView.setImageResource(R.drawable.user) // Imagen por defecto
            }
        } else {
            profileImageView.setImageResource(R.drawable.user) // Imagen por defecto
        }
    }

    private fun showDescriptionDialog() {
        val sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val userDescription = sharedPreferences.getString("userDescription", "No hay descripción disponible")

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage(userDescription)
            .setCancelable(true)
            .setPositiveButton("Salir") { dialog, _ ->
                dialog.dismiss()
            }
        val alert = dialogBuilder.create()
        alert.setTitle("Descripción del Usuario")
        alert.show()
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

    private fun goToBuscar() {
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

    private fun goToPersonalizationUser() {
        val i = Intent(this, userCustomizationActivity::class.java)
        startActivity(i)
    }

    override fun onResume() {
        super.onResume()

        // Cargar los datos del perfil
        val sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("userName", "Usuario")
        val profileImageUri = sharedPreferences.getString("profileImageUri", null)

        // Actualizar el nombre de usuario
        userNameTextView.text = userName

        // Actualizar la imagen de perfil
        if (profileImageUri != null) {
            try {
                val imageUri = Uri.parse(profileImageUri)
                val inputStream = contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                profileImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                profileImageView.setImageResource(R.drawable.user) // Imagen por defecto
            }
        } else {
            profileImageView.setImageResource(R.drawable.user) // Imagen por defecto
        }
    }
}
