package com.example.myapplication
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.util.Log
import android.view.Surface

import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraDevice: CameraDevice
    private lateinit var imageReader: ImageReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.viewFinder)

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            // Use back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Bind to lifecycle
            cameraProvider.bindToLifecycle(this, cameraSelector, previewView)

            // Open the camera
            openCamera()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun openCamera() {
        val cameraManager = getSystemService(CameraManager::class.java)
        val cameraId = cameraManager.cameraIdList[0] // Use the first camera ID
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCameraCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                }
            }, null)
        } catch (e: CameraAccessException) {
            Log.e("Camera", "Camera access error: ${e.message}")
        }
    }

    private fun createCameraCaptureSession() {
        try {
            val surfaceProvider = previewView.surfaceProvider
            val surface = Surface(surfaceProvider.getSurfaceTexture()) // This is where the issue occurs

            imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1)

            val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder.addTarget(surface)
            captureRequestBuilder.addTarget(imageReader.surface)

            // Set ISO and shutter speed
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF) // Disable auto-exposure
            captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 100) // Set ISO
            captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 30000000000) // Set shutter speed to 30 seconds

            // Create a CameraCaptureSession
            cameraDevice.createCaptureSession(listOf(surface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    // Capture the image
                    session.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                            super.onCaptureCompleted(session, request, result)
                            Log.d("Camera", "Capture completed")
                        }
                    }, null)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e("Camera", "Capture session configuration failed")
                }
            }, null)
        } catch (e: CameraAccessException) {
            Log.e("Camera", "Camera access error: ${e.message}")
        }
    }
}
}