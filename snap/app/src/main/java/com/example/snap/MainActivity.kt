import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var handler: Handler
    private var isCapturing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        handler = Handler(Looper.getMainLooper())

        // Create a button programmatically
        val button = Button(this).apply {
            text = "Capture Images"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Set button click listener
        button.setOnClickListener {
            if (!isCapturing) {
                startCapturing()
            } else {
                stopCapturing()
            }
        }

        // Create a layout and add the button
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(button)
        }

        setContentView(layout)
    }

    private fun startCapturing() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
            return
        }

        isCapturing = true
        handler.postDelayed(object : Runnable {
            private var count = 0

            override fun run() {
                if (count < 60) {
                    captureImage()
                    count++
                    handler.postDelayed(this, 1000)
                } else {
                    stopCapturing()
                }
            }
        }, 1000)
    }

    private fun stopCapturing() {
        isCapturing = false
        handler.removeCallbacksAndMessages(null)
        Log.d("Capture", "Stopped capturing images")
    }

    private fun captureImage() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
            return
        }

        try {
            val cameraId = cameraManager.cameraIdList[0]
            val characteristics: CameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)

            val imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1)
            val captureRequestBuilder = cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    val captureRequest = captureRequestBuilder.build()
                    camera.createCaptureSession(listOf(imageReader.surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            session.capture(captureRequest, object : CameraCaptureSession.CaptureCallback() {
                                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                                    super.onCaptureCompleted(session, request, result)
                                    Log.d("Capture", "Image captured")
                                }
                            }, handler)
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e("Camera", "Configuration failed")
                        }
                    }, handler)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                }
            }, handler)

        } catch (e: CameraAccessException) {
            Log.e("Camera", "Camera access exception: ${e.message}")
        }
    }
}
