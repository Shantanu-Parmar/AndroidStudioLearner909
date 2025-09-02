package com.example.androidexperiments

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.LinearLayout

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        setContentView(layout)

        // Add buttons for each experiment
        addButton(layout, "Exp 1: Hello World") { /* Intent to HelloWorldActivity */ }
        addButton(layout, "Exp 2: Login") { /* Intent to LoginActivity */ }
        addButton(layout, "Exp 3: Calculator") { /* Intent to CalculatorActivity */ }
        addButton(layout, "Exp 4: Multimedia Image") { /* Intent to ImageActivity */ }
        addButton(layout, "Exp 5: Data Persistence") { /* Intent to PreferencesActivity */ }
        addButton(layout, "Exp 6: HTTP Quotes") { /* Intent to QuotesActivity */ }
        addButton(layout, "Exp 7: SMS Handler") { /* Intent to SmsActivity */ }
        addButton(layout, "Exp 8: Bluetooth Devices") { /* Intent to BluetoothScanActivity */ }
        addButton(layout, "Exp 9: Bluetooth Chat") { /* Intent to BluetoothChatActivity */ }
        addButton(layout, "Exp 10: Wi-Fi Signals") { /* Intent to WifiActivity */ }
        addButton(layout, "Exp 11: Google Maps Location") { /* Intent to MapsActivity */ }
        addButton(layout, "Exp 12: Sensor (QR Scanner)") { /* Intent to QrSensorActivity */ }
        addButton(layout, "Exp 13: Camera Access") { /* Intent to CameraActivity */ }
        addButton(layout, "Exp 14: Microphone Access") { /* Intent to AudioActivity */ }
    }

    private fun addButton(layout: LinearLayout, text: String, onClick: () -> Unit) {
        val button = Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
        }
        layout.addView(button)
    }
}