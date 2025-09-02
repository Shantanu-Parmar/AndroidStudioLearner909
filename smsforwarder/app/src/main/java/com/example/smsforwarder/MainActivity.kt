package com.example.smsforwarder

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 100
    private val smsReceiver = SmsReceiver()
    private lateinit var statusTextView: TextView
    private val forwardToNumber = "1234567890" // Replace with the target phone number

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)

        if (checkPermissions()) {
            registerSmsReceiver()
            statusTextView.text = "Listening for SMS..."
        } else {
            requestPermissions()
        }
    }

    private fun checkPermissions(): Boolean {
        val receiveSms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
        val sendSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        val readPhoneState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        return receiveSms == PackageManager.PERMISSION_GRANTED &&
                sendSms == PackageManager.PERMISSION_GRANTED &&
                readPhoneState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_PHONE_STATE
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            registerSmsReceiver()
            statusTextView.text = "Listening for SMS..."
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            statusTextView.text = "Permissions denied. App cannot function."
            Toast.makeText(this, "Permissions required for SMS forwarding", Toast.LENGTH_LONG).show()
        }
    }

    private fun registerSmsReceiver() {
        val filter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        registerReceiver(smsReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(smsReceiver)
        } catch (e: Exception) {
            // Ignore if receiver was not registered
        }
    }

    inner class SmsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
                val bundle = intent.extras
                if (bundle != null) {
                    val pdus = bundle.get("pdus") as Array<*>
                    val format = bundle.getString("format")
                    for (pdu in pdus) {
                        val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray, format)
                        val sender = smsMessage.originatingAddress ?: "Unknown"
                        val messageBody = smsMessage.messageBody ?: ""
                        val message = "From: $sender\nMessage: $messageBody"
                        try {
                            SmsManager.getDefault().sendTextMessage(
                                forwardToNumber,
                                null,
                                message,
                                null,
                                null
                            )
                            Toast.makeText(context, "SMS forwarded to $forwardToNumber", Toast.LENGTH_SHORT).show()
                            statusTextView.text = "Last forwarded: $message"
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to forward SMS: ${e.message}", Toast.LENGTH_LONG).show()
                            statusTextView.text = "Error: ${e.message}"
                        }
                    }
                }
            }
        }
    }
}