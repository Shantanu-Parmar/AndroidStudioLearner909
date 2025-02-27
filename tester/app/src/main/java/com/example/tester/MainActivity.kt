@file:Suppress("DEPRECATION")

package com.example.tester

import android.hardware.Camera
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private var mCamera: Camera? = null
    private lateinit var mTextViewParams: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        val buttonGetParams = findViewById<Button>(R.id.button_get_params)
        mTextViewParams = findViewById(R.id.textview_params)

        // Set a listener to get the camera parameters when button is clicked
        buttonGetParams.setOnClickListener {
            try {
                // Open the camera
                mCamera = Camera.open()

                // Safely access the camera parameters
                mCamera?.let { camera ->
                    val params = camera.parameters
                    val flattenedParams = params.flatten()

                    // Display the flattened parameters in the TextView
                    mTextViewParams.text = flattenedParams

                    // Release the camera after using it
                    camera.release()
                } ?: run {
                    // Handle case where the camera couldn't be opened
                    mTextViewParams.text = "Error: Unable to open camera."
                }

            } catch (e: Exception) {
                // Handle exceptions, if any
                mTextViewParams.text = "Error accessing camera: ${e.message}"
            }
        }
    }
}
