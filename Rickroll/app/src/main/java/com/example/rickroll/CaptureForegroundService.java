package com.example.rickroll;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

public class CaptureForegroundService extends Service {

    private static final String CHANNEL_ID = "CameraCaptureChannel";
    private static final int NOTIFICATION_ID = 1;
    private Handler handler;
    private Runnable captureRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        handler = new Handler();
        captureRunnable = new Runnable() {
            @Override
            public void run() {
                takePicture();
                handler.postDelayed(this, 5000); // Repeat every 5 seconds
            }
        };

        // Start the loop
        handler.post(captureRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Camera Capture Service")
                .setContentText("Capturing photos every 5 seconds.")
                .setSmallIcon(R.drawable.ic_camera) // Replace with your icon
                .build();

        startForeground(NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    private void takePicture() {
        // Get the root node
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

        if (rootNode != null) {
            // Find the capture button
            AccessibilityNodeInfo captureButton = findCaptureButton(rootNode);
            if (captureButton != null) {
                captureButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.d("CaptureForegroundService", "Capture button clicked.");
            } else {
                Log.d("CaptureForegroundService", "Capture button not found.");
            }
        } else {
            Log.d("CaptureForegroundService", "Root node is null.");
        }
    }

    private AccessibilityNodeInfo findCaptureButton(AccessibilityNodeInfo node) {
        if (node == null) return null;

        // Replace with actual logic to find the capture button
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null && "Capture".equals(child.getContentDescription())) {
                return child;
            }
        }
        return null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(captureRunnable);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Camera Capture Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
