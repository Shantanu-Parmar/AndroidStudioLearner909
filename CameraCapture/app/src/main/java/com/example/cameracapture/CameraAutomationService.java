package com.example.cameracapture;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.Handler;
import android.os.Looper;
import java.util.List;
public class CameraAutomationService extends AccessibilityService {

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable captureRunnable;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Handle accessibility events if needed
    }

    @Override
    public void onInterrupt() {
        // Handle service interruption
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        // Schedule photo capture every 5 seconds
        captureRunnable = new Runnable() {
            @Override
            public void run() {
                openCameraAndCapture();
                handler.postDelayed(this, 5000); // Schedule next capture in 5 seconds
            }
        };
        handler.post(captureRunnable);
    }

    private void openCameraAndCapture() {
        // Open the camera app
        performGlobalAction(GLOBAL_ACTION_RECENTS);
        performGlobalAction(GLOBAL_ACTION_HOME);

        // Wait for a few seconds to ensure the camera app opens
        handler.postDelayed(() -> {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                // Find the camera app
                List<AccessibilityNodeInfo> cameraNodes = rootNode.findAccessibilityNodeInfosByText("Camera");
                if (cameraNodes != null && !cameraNodes.isEmpty()) {
                    AccessibilityNodeInfo cameraNode = cameraNodes.get(0);
                    if (cameraNode.isClickable()) {
                        cameraNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            }

            // Wait for the camera app to open and take a picture
            handler.postDelayed(() -> {
                AccessibilityNodeInfo rootNodeAfterOpen = getRootInActiveWindow();
                if (rootNodeAfterOpen != null) {
                    List<AccessibilityNodeInfo> captureButtons = rootNodeAfterOpen.findAccessibilityNodeInfosByText("Capture");
                    if (captureButtons != null && !captureButtons.isEmpty()) {
                        AccessibilityNodeInfo captureButton = captureButtons.get(0);
                        if (captureButton.isClickable()) {
                            captureButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }
            }, 3000); // Wait for 3 seconds to ensure camera app is ready
        }, 5000); // Wait for 5 seconds to ensure camera app is open
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && captureRunnable != null) {
            handler.removeCallbacks(captureRunnable);
        }
    }
}
