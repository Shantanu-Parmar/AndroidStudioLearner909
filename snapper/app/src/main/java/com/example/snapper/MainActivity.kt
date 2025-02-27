package com.example.snapper  // Make sure the package name matches your project

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Open the default camera app
        openCameraApp()
    }

    private fun openCameraApp() {
        // Create an Intent to launch the default camera app
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // If no camera app is available, display a message
            Toast.makeText(this, "No camera app found!", Toast.LENGTH_SHORT).show()
        }
    }
}
