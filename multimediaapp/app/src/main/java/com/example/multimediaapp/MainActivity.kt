package com.example.multimediaapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var currentPhotoPath: String

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            when (lastRequestedAction) {
                "camera" -> launchCamera()
                "gallery" -> launchGallery()
            }
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                startImageDetailsActivity(uri)
            }
        }
    }

    private val captureImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = Uri.fromFile(File(currentPhotoPath))
            startImageDetailsActivity(uri)
        }
    }

    private var lastRequestedAction = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonPickImage = findViewById<Button>(R.id.buttonPickImage)
        val buttonCaptureImage = findViewById<Button>(R.id.buttonCaptureImage)

        buttonPickImage.setOnClickListener {
            lastRequestedAction = "gallery"
            checkPermissionAndLaunch(Manifest.permission.READ_MEDIA_IMAGES, "gallery")
        }

        buttonCaptureImage.setOnClickListener {
            lastRequestedAction = "camera"
            checkPermissionAndLaunch(Manifest.permission.CAMERA, "camera")
        }
    }

    private fun checkPermissionAndLaunch(permission: String, action: String) {
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                when (action) {
                    "gallery" -> launchGallery()
                    "camera" -> launchCamera()
                }
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun launchCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile: File? = createImageFile()
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "com.example.multimediaapp.fileprovider",
                it
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            captureImageLauncher.launch(intent)
        }
    }

    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
                currentPhotoPath = absolutePath
            }
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, "Error creating file", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun startImageDetailsActivity(uri: Uri) {
        val intent = Intent(this, ImageDetailsActivity::class.java)
        intent.putExtra("IMAGE_URI", uri.toString())
        startActivity(intent)
    }
}