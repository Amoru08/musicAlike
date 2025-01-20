package com.example.musicalike

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : BaseActivity() {

    private lateinit var textview: TextView
    private lateinit var back: Button
    private lateinit var search: Button
    private lateinit var user: Button
    private lateinit var playlists: Button
    private lateinit var favorites: Button
    private lateinit var personalizationUser: ImageButton
    private lateinit var palette: ImageButton
    private lateinit var profileImageView: ImageView
    private lateinit var userNameTextView: TextView

    private val auth = FirebaseAuth.getInstance()
    private val REQUEST_CODE_PALETTE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        // Inicializar vistas
        palette = findViewById(R.id.palette)
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

        // Configurar listeners
        search.setOnClickListener { goToBuscar() }
        back.setOnClickListener { goToPrincipio() }
        user.setOnClickListener { startSpotifyAuth() }
        playlists.setOnClickListener { goToPlaylist() }
        favorites.setOnClickListener { goToFavorites() }
        personalizationUser.setOnClickListener { goToPersonalizationUser() }
        profileImageView.setOnClickListener { showDescriptionDialog() }
        palette.setOnClickListener { goToPalette() }
    }

    private fun loadUserProfile() {
        val sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("userName", "Usuario")
        userNameTextView.text = userName

        // Cargar la imagen guardada
        val bitmap = loadProfileImage()
        if (bitmap != null) {
            profileImageView.setImageBitmap(bitmap)
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
            .setPositiveButton("Salir") { dialog, _ -> dialog.dismiss() }
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

    private fun goToPalette() {
        val i = Intent(this, PaletteActivity::class.java)
        startActivityForResult(i, REQUEST_CODE_PALETTE)
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
        startActivityForResult(i, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PALETTE && resultCode == RESULT_OK) {
            recreate()
        }
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            loadUserProfile() // Cargar el perfil actualizado
        } else if (requestCode == REQUEST_CODE_PALETTE && resultCode == RESULT_OK) {
            recreate() // Reinicia la actividad para aplicar el nuevo tema
        }
    }

    private fun loadProfileImage(): Bitmap? {
        return try {
            val fileName = "profile_image.png"
            val fileInputStream = openFileInput(fileName)
            BitmapFactory.decodeStream(fileInputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
