package com.example.starpointer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import androidx.compose.foundation.layout.Arrangement
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.starpointer.ui.theme.StarpointerTheme
import com.google.android.gms.location.LocationServices
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors



class CameraActivity : ComponentActivity() {
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.all { it.value }) {
            Log.e("CameraActivity", "Location permission denied")
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Log.e("CameraActivity", "Storage permission denied")
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Log.e("CameraActivity", "Camera permission denied")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions
        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        locationPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        // Copy NCNN model files from assets to internal storage
        val modelDir = File(filesDir, "models")
        if (!modelDir.exists()) modelDir.mkdirs()
        copyAssetToFile("nanodet.param", File(modelDir, "nanodet.param"))
        copyAssetToFile("nanodet.bin", File(modelDir, "nanodet.bin"))
        copyAssetToFile("labels.txt", File(modelDir, "labels.txt"))

        // Initialize NCNN
        System.loadLibrary("nanodet")
        val initialized = initNanoDet(modelDir.absolutePath, File(modelDir, "labels.txt").absolutePath)
        if (!initialized) {
            Log.e("CameraActivity", "Failed to initialize NanoDet")
        }

        val selectedObject = intent.getStringExtra("SELECTED_OBJECT") ?: "Unknown"

        setContent {
            StarpointerTheme @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) {
                CameraScreen(selectedObject) {
                    finish()
                }
            }
        }
    }

    // Copy asset to internal storage
    private fun copyAssetToFile(assetName: String, outputFile: File) {
        if (!outputFile.exists()) {
            assets.open(assetName).use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    // JNI declarations
    private external fun initNanoDet(modelPath: String, labelsPath: String): Boolean
    external fun detectObjects(bitmap: Bitmap): Array<Detection>

    data class Detection(val box: RectF, val label: String, val score: Float)

    companion object {
        // For accessing detectObjects without instantiating CameraActivity
        fun detectObjectsStatic(bitmap: Bitmap): Array<Detection> {
            return CameraActivity().detectObjects(bitmap)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@Composable
fun CameraScreen(selectedObject: String, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Sensor states
    var accValues by remember { mutableStateOf("Acc: X=0 Y=0 Z=0") }
    var gyroValues by remember { mutableStateOf("Gyro: X=0 Y=0 Z=0") }
    var magValues by remember { mutableStateOf("Mag: X=0 Y=0 Z=0") }
    var locationValues by remember { mutableStateOf("Lat: 0.0 Long: 0.0") }
    var currentTime by remember { mutableStateOf("Time: 00:00:00") }

    // Detections state
    val detections = remember { mutableStateListOf<CameraActivity.Detection>() }

    // Setup sensors
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]
                    when (it.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> accValues = "Acc: X=%.1f Y=%.1f Z=%.1f".format(x, y, z)
                        Sensor.TYPE_GYROSCOPE -> gyroValues = "Gyro: X=%.1f Y=%.1f Z=%.1f".format(x, y, z)
                        Sensor.TYPE_MAGNETIC_FIELD -> magValues = "Mag: X=%.1f Y=%.1f Z=%.1f".format(x, y, z)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    // Location setup
    val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationProvider.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            locationValues = "Lat: %.4f Long: %.4f".format(location.latitude, location.longitude)
        }
    }

    // Time update
    val handler = Handler(Looper.getMainLooper())
    val timeUpdater = object : Runnable {
        override fun run() {
            currentTime = "Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
            handler.postDelayed(this, 1000)
        }
    }
    DisposableEffect(Unit) {
        handler.post(timeUpdater)
        onDispose { handler.removeCallbacks(timeUpdater) }
    }

    // Register sensors
    DisposableEffect(Unit) {
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(sensorListener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sensorManager.unregisterListener(sensorListener) }
    }

    // Camera setup
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val executor = Executors.newSingleThreadExecutor()
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { viewContext ->
                val previewView = PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(viewContext)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    imageCapture = ImageCapture.Builder().build()
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(executor) { imageProxy ->
                                val bitmap = imageProxy.toBitmap()
                                val results = CameraActivity.detectObjectsStatic(bitmap)
                                detections.clear()
                                detections.addAll(results.filter { it.label == selectedObject })
                                imageProxy.close()
                            }
                        }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                            imageAnalyzer
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraScreen", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(viewContext))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Draw bounding boxes and labels
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            detections.forEach { detection ->
                val box = detection.box
                val scaleX = canvasWidth / 416f
                val scaleY = canvasHeight / 416f
                val scaledBox = RectF(
                    box.left * scaleX,
                    box.top * scaleY,
                    box.right * scaleX,
                    box.bottom * scaleY
                )
                drawRect(
                    color = Color.Green,
                    topLeft = Offset(scaledBox.left, scaledBox.top),
                    size = Size(scaledBox.width(), scaledBox.height()),
                    style = Stroke(width = 4f)
                )
            }
        }

        // Bounding box labels as Text
        detections.forEach { detection ->
            val box = detection.box
            val scaleX = LocalContext.current.resources.displayMetrics.widthPixels / 416f
            val scaleY = LocalContext.current.resources.displayMetrics.heightPixels / 416f
            val scaledBox = RectF(
                box.left * scaleX,
                box.top * scaleY,
                box.right * scaleX,
                box.bottom * scaleY
            )
            Text(
                text = "${detection.label} (${String.format("%.2f", detection.score)})",
                color = Color.Green,
                fontSize = 14.sp,
                modifier = Modifier
                    .offset(x = scaledBox.left.dp, y = (scaledBox.top - 20).dp)
            )
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = accValues, fontSize = 10.sp, color = Color.White)
            Text(text = gyroValues, fontSize = 10.sp, color = Color.White)
            Text(text = magValues, fontSize = 10.sp, color = Color.White)
            Text(text = locationValues, fontSize = 10.sp, color = Color.White)
            Text(text = currentTime, fontSize = 10.sp, color = Color.White)
        }

        // Capture Button (Bottom-Right)
        Button(
            onClick = { captureImage(context, imageCapture) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Capture")
        }

        // Back Button (Top-Left)
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text("Back")
        }

        // Selected Object (Bottom-Center)
        Text(
            text = "Tracking: $selectedObject",
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

// Helper: Capture Image
private fun captureImage(context: Context, imageCapture: ImageCapture?) {
    val photoFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture?.takePicture(
        outputOptions,
        Executors.newSingleThreadExecutor(),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Log.d("CameraScreen", "Image saved: ${photoFile.absolutePath}")
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Capture failed: ${exception.message}")
            }
        }
    )
}

// Helper: Convert ImageProxy to Bitmap
@RequiresApi(Build.VERSION_CODES.O)
private fun androidx.camera.core.ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val data = ByteArray(ySize + uSize + vSize)
    yBuffer.get(data, 0, ySize)
    uBuffer.get(data, ySize, uSize)
    vBuffer.get(data, ySize + uSize, vSize)

    val yuvImage = android.graphics.YuvImage(data, android.graphics.ImageFormat.NV21, width, height, null)
    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}