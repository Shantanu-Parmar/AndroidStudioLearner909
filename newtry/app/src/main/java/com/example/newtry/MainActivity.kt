import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.newtry.ui.theme.NewtryTheme

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession

    private var cameraPreviewSurface: Surface? = null

    // ISO and Shutter speed ranges
    private var minIso: Int = 100
    private var maxIso: Int = 3200

    private val minShutterSpeed: Long = 1_000_000 // 1/1000 seconds
    private val maxShutterSpeed: Long = 30_000_000_000L // 30 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NewtryTheme {
                CameraControlScreen()
            }
        }

        openCamera()
    }

    @Composable
    fun CameraControlScreen() {
        var isoValue by remember { mutableStateOf((maxIso + minIso) / 2f) }
        var shutterSpeedValue by remember { mutableStateOf(minShutterSpeed.toFloat()) }
        val context = LocalContext.current

        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview in the background
            CameraPreviewView()

            // Controls in the foreground
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ISO Control
                Column {
                    Text(text = "ISO: ${isoValue.toInt()}")
                    Slider(
                        value = isoValue,
                        onValueChange = { newValue ->
                            isoValue = newValue
                            setManualISO(newValue.toInt())
                        },
                        valueRange = minIso.toFloat()..maxIso.toFloat()
                    )
                }

                // Shutter Speed Control
                Column {
                    Text(text = "Shutter Speed: ${shutterSpeedValue / 1_000_000} s")
                    Slider(
                        value = shutterSpeedValue,
                        onValueChange = { newValue ->
                            shutterSpeedValue = newValue
                            setManualShutterSpeed(newValue.toLong())
                        },
                        valueRange = minShutterSpeed.toFloat()..maxShutterSpeed.toFloat()
                    )
                }

                // Capture Button
                Button(onClick = {
                    captureImage(context)
                }) {
                    Text(text = "Capture Image")
                }
            }
        }
    }

    @Composable
    fun CameraPreviewView() {
        AndroidView(
            factory = { context ->
                TextureView(context).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            cameraPreviewSurface = Surface(surface)
                            createCameraPreviewSession()
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {}

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            cameraPreviewSurface = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    private fun captureImage(context: Context) {
        // Capture Image logic
        try {
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            cameraCaptureSession.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    Toast.makeText(context, "Image Captured", Toast.LENGTH_SHORT).show()
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun setManualISO(isoValue: Int) {
        captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, isoValue)
        updatePreview()
    }

    private fun setManualShutterSpeed(shutterSpeedValue: Long) {
        captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, shutterSpeedValue)
        updatePreview()
    }

    private fun updatePreview() {
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun openCamera() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0] // Back camera
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            minIso = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.lower ?: 100
            maxIso = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.upper ?: 3200

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
                    createCameraPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cameraDevice.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    cameraDevice.close()

                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun createCameraPreviewSession() {
        val surface = cameraPreviewSurface ?: return
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)

        cameraDevice.run {
            this.createCaptureSession(
                /* outputs = */ listOf(surface),
                /* callback = */ object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        updatePreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        // Handle failure
                    }
                },
                /* handler = */ null
            )
        }
    }
}
