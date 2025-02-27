package com.example.cameraxcapture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.sdf.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private ImageCapture imageCapture;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int photoCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        PreviewView previewView = findViewById(R.id.previewView);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider, previewView);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider, PreviewView previewView) {
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        imageCapture = new ImageCapture.Builder().build();

        cameraProvider.unbindAll();

        cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture);

        // Start capturing images every 2 seconds for 1 minute
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (photoCount < 30) {
                    capturePhoto();
                    photoCount++;
                    handler.postDelayed(this, 2000); // Capture every 2 seconds
                } else {
                    Toast.makeText(MainActivity.this, "Photo capturing completed!", Toast.LENGTH_SHORT).show();
                }
            }
        }, 2000); // Initial delay of 2 seconds
    }

    private void capturePhoto() {
        File photoFile = new File(getExternalFilesDir(null), "photo_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, Executors.newSingleThreadExecutor(), new ImageCapture.OnImageSavedCallback() {

            public void onImageSaved(@NonNull ImageCaptureOutputFileResults output) {
                Log.d("CameraXApp", "Photo captured successfully: " + photoFile.getAbsolutePath());
            }

            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {

            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("CameraXApp", "Photo capture failed: " + exception.getMessage());
            }
        });
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
