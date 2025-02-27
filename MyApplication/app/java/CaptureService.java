import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class CaptureService extends AccessibilityService {

    private Handler handler = new Handler();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No implementation needed for this use case
    }

    @Override
    public void onInterrupt() {
        // Handle service interruption
    }

    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        startCaptureTask();
    }

    private void startCaptureTask() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                performCapture();
                handler.postDelayed(this, 31000); // 31 seconds
            }
        }, 31000);
    }

    private void performCapture() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            AccessibilityNodeInfo captureButton = findNodeByText(rootNode, "Capture");
            if (captureButton != null) {
                captureButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo root, String text) {
        if (root == null) return null;
        if (text.equals(root.getText())) return root;
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            AccessibilityNodeInfo result = findNodeByText(child, text);
            if (result != null) return result;
        }
        return null;
    }
}
