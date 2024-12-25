package com.example.musicalike

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Track

class SpotifyUserActivity : AppCompatActivity() {

    private lateinit var detail : Button

    private val clientId = "1d1c94387942461b8bd890e34b4ab6c7" // Tu CLIENT_ID de Spotify
    private val redirectUri = "musicalike://callback" // Asegúrate de que este sea tu redirect_uri
    private val requestCode = 1337 // Código de solicitud para la autenticación de Spotify
    private var spotifyAppRemote: SpotifyAppRemote? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotifyuser)

        val loginButton: Button = findViewById(R.id.authButton)
        detail = findViewById(R.id.detalles)
        loginButton.setOnClickListener { authenticateWithSpotify() }
        detail.setOnClickListener {  goToDetalles()}
    }

    private fun authenticateWithSpotify() {
        val builder = AuthorizationRequest.Builder(
            clientId,
            AuthorizationResponse.Type.TOKEN,
            redirectUri
        )

        builder.setScopes(arrayOf("user-read-private", "user-read-email", "playlist-read-private"))
        val request = builder.build()

        AuthorizationClient.openLoginActivity(this, requestCode, request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == this.requestCode) {
            val response = AuthorizationClient.getResponse(resultCode, data)

            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    val accessToken = response.accessToken
                    Log.d("SpotifyUserActivity", "Access Token: $accessToken")
                    // Después de obtener el token, nos conectamos a Spotify
                    connectToSpotifyAppRemote(accessToken)
                }

                AuthorizationResponse.Type.ERROR -> {
                    Log.e("SpotifyUserActivity", "Error al autenticar: ${response.error}")
                }

                else -> {
                    Log.e("SpotifyUserActivity", "Autenticación cancelada o sin éxito")
                }
            }
        }
    }

    private fun connectToSpotifyAppRemote(accessToken: String) {
        // Usamos el token para conectar, pero sin setAccessToken
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        // Conectar a Spotify sin el token en ConnectionParams (el token será manejado internamente)
        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("SpotifyUserActivity", "Conectado a Spotify! Yay!")
                // Aquí podemos empezar a interactuar con Spotify
                startSpotifyPlayback()
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("SpotifyUserActivity", "Error al conectar con Spotify: ${throwable.message}")
            }
        })
    }

    private fun startSpotifyPlayback() {
        spotifyAppRemote?.let {
            // Reproducir una lista de reproducción
            val playlistURI = "spotify:playlist:37i9dQZF1DX2sUQwD7tbmL" // URI de ejemplo
            it.playerApi.play(playlistURI)

            // Suscribirse al estado del reproductor
            it.playerApi.subscribeToPlayerState().setEventCallback { playerState ->
                val track: Track = playerState.track
                Log.d("SpotifyUserActivity", "Reproduciendo: ${track.name} de ${track.artist.name}")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Desconectar de Spotify cuando la actividad se detenga
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
    }
    private fun goToDetalles() {
        val i = Intent(this, SpotifyDetailsActivity::class.java)
        startActivity(i)
    }

}
