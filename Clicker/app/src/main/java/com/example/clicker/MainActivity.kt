package com.example.clicker

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up a button to open the camera when clicked
        val button: Button = findViewById(R.id.button_open_camera)
        button.setOnClickListener {
            openCameraApp()
        }
    }

    private fun openCameraApp() {
        // Create an intent to open the camera app
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            startActivity(cameraIntent)
        } else {
            // If no camera app is found, show a message
            Toast.makeText(this, "No camera app found!", Toast.LENGTH_SHORT).show()
        }
    }
}
