package com.example.musicalike

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

class userCustomizationActivity : BaseActivity() {

    private lateinit var editUserName: EditText
    private lateinit var editUserDescription: EditText
    private lateinit var profileImage: ImageView
    private lateinit var btnChangeProfileImage: Button
    private lateinit var btnSaveChanges: Button

    private val PICK_IMAGE_REQUEST_CODE = 1002
    private var profileImageUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("permissionRequested", true)
            editor.apply()
            openImagePicker()
        } else {
            Toast.makeText(this, "Permiso denegado. Habilítalo en configuraciones.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_customization)

        editUserName = findViewById(R.id.editUserName)
        editUserDescription = findViewById(R.id.editUserDescription)
        profileImage = findViewById(R.id.profileImage)
        btnChangeProfileImage = findViewById(R.id.btnChangeProfileImage)
        btnSaveChanges = findViewById(R.id.btnSave)

        btnChangeProfileImage.setOnClickListener {
            checkPermissionAndOpenImagePicker()
        }

        btnSaveChanges.setOnClickListener {
            val userName = editUserName.text.toString()
            val userDescription = editUserDescription.text.toString()
            val sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            if (userName.isNotBlank()) {
                editor.putString("userName", userName)
            }
            if (userDescription.isNotBlank()) {
                editor.putString("userDescription", userDescription)
            }
            profileImageUri?.let { uri ->
                editor.putString("profileImageUri", uri.toString())
            }
            editor.apply()
            Toast.makeText(this, "Cambios guardados exitosamente", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun checkPermissionAndOpenImagePicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                showPermissionRequestDialog(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun showPermissionRequestDialog(permission: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Esta aplicación necesita acceso a tus fotos para cambiar la imagen de perfil. ¿Deseas otorgar el permiso?")
            .setCancelable(false)
            .setPositiveButton("Aceptar") { _, _ ->
                requestPermissionLauncher.launch(permission)
            }
            .setNegativeButton("Denegar") { dialog, _ ->
                dialog.cancel()
                Snackbar.make(findViewById(android.R.id.content), "Permiso denegado", Snackbar.LENGTH_SHORT).show()
            }
        val alert = dialogBuilder.create()
        alert.setTitle("Solicitud de permiso")
        alert.show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            if (imageUri != null) {
                profileImageUri = imageUri
                setProfileImage(imageUri)
            }
        }
    }

    private fun setProfileImage(imageUri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
            val fileName = saveProfileImage(resizedBitmap)
            val sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("profileImageFileName", fileName)
            editor.apply()
            profileImage.setImageBitmap(resizedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileImage(bitmap: Bitmap): String {
        val fileName = "profile_image.png"
        try {
            openFileOutput(fileName, Context.MODE_PRIVATE).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
        }
        return fileName
    }
}
