package com.example.cameracaptureapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var locationManager: LocationManager
    private var gpsLocation: Location? = null
    private lateinit var sensorManager: SensorManager
    private var accelerometerData = FloatArray(3)
    private val handler = Handler()
    private lateinit var captureRunnable: Runnable

     override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Camera Executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize location manager and sensor manager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Register accelerometer listener
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(object : SensorEventListener {
             fun onSensorChanged(event: SensorEvent) {
                accelerometerData = event.values.clone()
            }

             fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        // Capture button listener
        val captureButton = findViewById<Button>(R.id.captureButton)
        captureButton.setOnClickListener {
            if (checkPermissions()) {
                startCamera()
                startLocationUpdates()
                startImageCapture()
            }
        }

        // Request necessary permissions
        requestPermissions()
    }

    private fun checkPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return cameraPermission == PackageManager.PERMISSION_GRANTED && locationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    if (!it.value) {
                        Toast.makeText(this, "Permission ${it.key} denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        requestMultiplePermissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION))
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
                .also {
                    it.setSurfaceProvider(findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder).surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("MainActivity", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, object : LocationListener {
             fun onLocationChanged(location: Location) {
                gpsLocation = location
            }

             fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
             fun onProviderEnabled(provider: String) {}
             fun onProviderDisabled(provider: String) {}
        })
    }

    private fun startImageCapture() {
        captureRunnable = object : Runnable {
             fun run() {
                captureImage()
                handler.postDelayed(this, 1000) // Capture every 1 second
            }
        }
        handler.post(captureRunnable)
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
             fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Log.d("MainActivity", "Image saved: ${photoFile.absolutePath}")
                saveDataToCSV(photoFile.name)
            }

             fun onError(exception: ImageCaptureException) {
                Log.e("MainActivity", "Image capture failed: ${exception.message}", exception)
            }
        })
    }

    private fun saveDataToCSV(imageName: String) {
        val csvFile = File(getExternalFilesDir(null), "data.csv")

        val latitude = gpsLocation?.latitude ?: "Unknown"
        val longitude = gpsLocation?.longitude ?: "Unknown"

        val csvData = String.format(
            Locale.US, "%s,%s,%s,%f,%f,%f\n",
            imageName, latitude.toString(), longitude.toString(),
            accelerometerData[0], accelerometerData[1], accelerometerData[2]
        )

        try {
            val writer = FileWriter(csvFile, true)
            writer.append(csvData)
            writer.close()
        } catch (e: IOException) {
            Log.e("MainActivity", "Error writing to CSV file", e)
        }
    }

        fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        handler.removeCallbacks(captureRunnable)
    }
}
