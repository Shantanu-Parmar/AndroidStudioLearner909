package com.example.cameracapture;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button stopServiceButton = findViewById(R.id.stopServiceButton);
        stopServiceButton.setOnClickListener(v -> {
            stopService(new Intent(SettingsActivity.this, CameraAutomationService.class));
            finish();
        });

        // Open accessibility settings
        Button openAccessibilitySettingsButton = findViewById(R.id.openAccessibilitySettingsButton);
        openAccessibilitySettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
    }
}
