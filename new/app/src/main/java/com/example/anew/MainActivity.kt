package com.example.anew

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private lateinit var textureView: TextureView
    private lateinit var captureButton: Button
    private var isCapturing = false
    private var captureCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.textureView)
        captureButton = findViewById(R.id.captureButton)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Check for camera permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            setupCameraAndCapture()
        }

        captureButton.setOnClickListener {
            if (!isCapturing) {
                captureImage()
            } else {
                Toast.makeText(this, "Already capturing", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCameraAndCapture() {
        cameraId = getCameraWithMaxExposureTime() ?: run {
            Toast.makeText(this, "No suitable camera found", Toast.LENGTH_SHORT).show()
            return
        }
        openCamera(cameraId)
    }

    private fun getCameraWithMaxExposureTime(): String? {
        val cameraIdList = cameraManager.cameraIdList
        var bestCameraId: String? = null
        var maxExposureTime: Long = 0

        for (cameraId in cameraIdList) {
            try {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val exposureTimeRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)

                if (exposureTimeRange != null && exposureTimeRange.upper > maxExposureTime) {
                    maxExposureTime = exposureTimeRange.upper
                    bestCameraId = cameraId
                }
            } catch (e: CameraAccessException) {
                Log.e("CameraAccess", "Error accessing camera $cameraId: ${e.message}")
            }
        }
        return bestCameraId
    }

    private fun openCamera(cameraId: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startPreview()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                Log.e("CameraError", "Camera error: $error")
            }
        }, null)
    }

    private fun startPreview() {
        val surfaceTexture = textureView.surfaceTexture
        surfaceTexture?.setDefaultBufferSize(1920, 1080)

        val surface = Surface(surfaceTexture)

        // Set up the ImageReader for RAW format (DNG)
        imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.RAW_SENSOR, 1)

        val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)
        captureRequestBuilder.addTarget(imageReader.surface)

        cameraDevice.createCaptureSession(listOf(surface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(this@MainActivity, "Configuration failed", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    private fun captureImage() {
        if (isCapturing) return

        isCapturing = true
        val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder.addTarget(imageReader.surface)

        // Use the maximum exposure time for the capture
        val exposureTimeRange = cameraManager.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)

        // Set exposure time to maximum allowed for the camera
        exposureTimeRange?.let {
            captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, it.upper)
        }

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                saveImage(it)
                Log.d("CameraCapture", "Image captured: $captureCount")
                captureCount++ // Increment the count after capturing
                it.close()
                isCapturing = false // Allow capturing again
            }
        }, Handler(Looper.getMainLooper()))

        captureSession.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                super.onCaptureCompleted(session, request, result)
                Log.d("CameraCapture", "Capture completed: $captureCount")
            }
        }, null)
    }

    private fun saveImage(image: Image) {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Save the image to a file in DNG format
        val file = File(getExternalFilesDir(null), "captured_image_${captureCount}.dng")
        FileOutputStream(file).use { output ->
            output.write(bytes)
            Log.d("CameraCapture", "Image saved: ${file.absolutePath}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                setupCameraAndCapture()
            } else {
                Log.e("CameraPermission", "Camera permission denied.")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Release resources
        if (::cameraDevice.isInitialized) {
            cameraDevice.close()
        }
        if (::captureSession.isInitialized) {
            captureSession.close()
        }
        if (::imageReader.isInitialized) {
            imageReader.close()
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }
}
