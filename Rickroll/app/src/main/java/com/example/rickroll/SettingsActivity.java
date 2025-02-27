package com.example.rickroll;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences); // Load preferences from XML

        // Example: add a button to show a toast message
        Button exampleButton = findViewById(R.id.example_button);
        exampleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SettingsActivity.this, "Button clicked!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
