package com.example.musicalike

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class si_registrado : BaseActivity() {

    private lateinit var atras: Button
    private lateinit var elEmail: EditText
    private lateinit var laContra: EditText
    private lateinit var ir: Button
    private lateinit var googleSignInButton: Button

    private val requestCode = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("si_registrado", "Entrando en si_registrado")
        setContentView(R.layout.activity_si_registrado)

        atras = findViewById(R.id.atras2)
        elEmail = findViewById(R.id.elEmail)
        laContra = findViewById(R.id.laContra)
        ir = findViewById(R.id.irHome)

        val bundle = intent.extras
        Log.d("si_registrado", "Extras recibidos: $bundle")

        setup()

    }

    private fun setup() {
        atras.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            goToPrincipio()
        }

        ir.setOnClickListener {
            val emailInput = elEmail.text.toString()
            val passwordInput = laContra.text.toString()

            if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese email y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseAuth.getInstance().signInWithEmailAndPassword(emailInput, passwordInput)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        goToHome()
                    } else {
                        val errorMessage = task.exception?.message ?: "Error desconocido"
                        Toast.makeText(this, "Error al iniciar sesión: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun goToPrincipio() {
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
        finish()
    }

    private fun goToHome() {
        val i = Intent(this, HomeActivity::class.java)
        startActivity(i)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == this.requestCode) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("si_registrado", "Cuenta de Google: ${account?.displayName}, Email: ${account?.email}")
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.e("si_registrado", "Error de autenticación: ${e.message}")
                Toast.makeText(this, "Error de autenticación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        account?.let {
            val credential = GoogleAuthProvider.getCredential(it.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        goToHome()
                    } else {
                        Toast.makeText(this, "Error al autenticar con Google", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}