package com.example.multimediaapp

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.io.File
import java.text.DecimalFormat

class ImageDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_details)

        val imageView = findViewById<ImageView>(R.id.imageView)
        val detailsTextView = findViewById<TextView>(R.id.detailsTextView)

        val imageUriString = intent.getStringExtra("IMAGE_URI")
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            imageView.setImageURI(imageUri)

            val details = StringBuilder()
            try {
                val file = File(imageUri.path ?: return)
                val fileName = file.name
                val fileSize = file.length() / 1024.0 // Size in KB
                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri))
                val width = bitmap.width
                val height = bitmap.height
                val path = imageUri.path

                details.append("File Name: $fileName\n")
                details.append("File Size: ${DecimalFormat("#.##").format(fileSize)} KB\n")
                details.append("Dimensions: ${width}x${height} pixels\n")
                details.append("Path: $path")
            } catch (e: Exception) {
                details.append("Error retrieving image details: ${e.message}")
            }
            detailsTextView.text = details.toString()
        } else {
            detailsTextView.text = "No image selected"
        }
    }
}