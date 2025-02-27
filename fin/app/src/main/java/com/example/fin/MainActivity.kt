package com.example.fin

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import com.example.fin.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.SeekBar
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageCaptureException
import java.text.SimpleDateFormat
import java.util.Locale


@ExperimentalZeroShutterLag
class MainActivity : AppCompatActivity() {
    private var isBurstModeActive = false

    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Set up the listener for the capture button
        viewBinding.imagecapture.setOnClickListener {
            handleCapture()
        }

        // Initialize the camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Set up the exposure and shutter speed control
        setupExposureControl()
        setupShutterSpeedControl()
    }

    private fun handleCapture() {
        // Get input values
        val photosPerMinuteStr = viewBinding.photosPerMinute.text.toString()
        val durationStr = viewBinding.minutesDuration.text.toString()

        if (photosPerMinuteStr.isNotEmpty() && durationStr.isNotEmpty()) {
            val photosPerMinute = photosPerMinuteStr.toInt()
            val durationInMinutes = durationStr.toInt()

            // Start burst mode if inputs are valid
            startBurstMode(photosPerMinute, durationInMinutes)
        } else {
            // If inputs are not provided, take a single photo
            takePhoto()
        }
    }

    private fun setupExposureControl() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
            val exposureState = camera.cameraInfo.exposureState

            viewBinding.exposureSeekBar.apply {
                isEnabled = exposureState.isExposureCompensationSupported
                max = exposureState.exposureCompensationRange.upper
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    min = exposureState.exposureCompensationRange.lower
                }
                progress = exposureState.exposureCompensationIndex

                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        camera.cameraControl.setExposureCompensationIndex(progress)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupShutterSpeedControl() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
            val exposureState = camera.cameraInfo.exposureState

            // Adjust max and min values based on your requirements
            val shutterSpeedRange = 1..30000 // Example range (in milliseconds)
            viewBinding.shutterSpeedSeekBar.apply {
                max = shutterSpeedRange.last
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    min = shutterSpeedRange.first
                }
                progress = 500 // Default shutter speed in milliseconds (adjust as needed)

                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        // Set shutter speed, convert milliseconds to nanoseconds
                        val shutterSpeedInNanoseconds = progress * 1_000_000
                        camera.cameraControl.setExposureCompensationIndex(shutterSpeedInNanoseconds)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            // Set up the ImageCapture use case
            val builder = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG) // Enable Zero-Shutter Lag

            // Build the ImageCapture instance
            imageCapture = builder.build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() {
        // Implement permission request logic here
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private fun startBurstMode(photosPerMinute: Int, durationInMinutes: Int) {
        if (isBurstModeActive) return
        isBurstModeActive = true

        val intervalMillis = (60_000 / photosPerMinute).toLong() // Calculate interval in milliseconds
        val totalBurstDuration = durationInMinutes * 60_000L // Convert minutes to milliseconds

        // Start capturing photos in a coroutine
        cameraExecutor.execute {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + totalBurstDuration

            while (System.currentTimeMillis() < endTime) {
                takePhoto() // Capture photo
                Thread.sleep(intervalMillis) // Wait for the next capture
            }
            isBurstModeActive = false // Reset flag after completing burst
        }
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }



}
