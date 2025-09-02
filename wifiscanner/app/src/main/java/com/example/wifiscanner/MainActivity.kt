package com.example.wifiscanner
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var wifiManager: WifiManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var scanButton: Button
    private val wifiList = mutableListOf<android.net.wifi.ScanResult>()
    private lateinit var wifiAdapter: WifiAdapter
    private val TAG = "WifiScanner"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.wifiRecyclerView)
        scanButton = findViewById(R.id.scanButton)
        wifiAdapter = WifiAdapter(wifiList) { scanResult ->
            connectToWifi(scanResult)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = wifiAdapter

        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Please enable Wi-Fi", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (checkPermissions()) {
            startWifiScan()
        } else {
            requestPermissions()
        }

        scanButton.setOnClickListener {
            if (checkPermissions()) {
                startWifiScan()
            } else {
                requestPermissions()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.CHANGE_NETWORK_STATE)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.CHANGE_NETWORK_STATE)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            startWifiScan()
        } else {
            Toast.makeText(this, "Permissions required for Wi-Fi scanning", Toast.LENGTH_LONG).show()
        }
    }

    private fun startWifiScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            wifiList.clear()
            wifiAdapter.notifyDataSetChanged()
            wifiManager.startScan()
            Toast.makeText(this, "Scanning for Wi-Fi networks...", Toast.LENGTH_SHORT).show()
            registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        }
    }

    private fun connectToWifi(scanResult: android.net.wifi.ScanResult) {
        val ssid = scanResult.SSID
        val capabilities = scanResult.capabilities
        if (capabilities.contains("WPA") || capabilities.contains("WEP")) {
            // Show password dialog for secured networks
            val passwordInput = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("Connect to $ssid")
                .setMessage("Enter password")
                .setView(passwordInput)
                .setPositiveButton("Connect") { _, _ ->
                    val password = passwordInput.text.toString()
                    if (password.isNotEmpty()) {
                        configureAndConnectWifi(ssid, password, capabilities)
                    } else {
                        Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Connect to open network
            configureAndConnectWifi(ssid, null, capabilities)
        }
    }

    private fun configureAndConnectWifi(ssid: String, password: String?, capabilities: String) {
        val wifiConfig = WifiConfiguration().apply {
            SSID = "\"$ssid\""
            if (password != null) {
                if (capabilities.contains("WPA")) {
                    preSharedKey = "\"$password\""
                } else if (capabilities.contains("WEP")) {
                    wepKeys[0] = "\"$password\""
                    wepTxKeyIndex = 0
                }
            }
            allowedKeyManagement.set(if (password == null) WifiConfiguration.KeyMgmt.NONE else WifiConfiguration.KeyMgmt.WPA_PSK)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
            val networkId = wifiManager.addNetwork(wifiConfig)
            if (networkId != -1) {
                wifiManager.disconnect()
                wifiManager.enableNetwork(networkId, true)
                wifiManager.reconnect()
                Toast.makeText(this, "Connecting to $ssid...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to configure network", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    wifiList.clear()
                    wifiList.addAll(wifiManager.scanResults)
                    wifiAdapter.notifyDataSetChanged()
                    Toast.makeText(this@MainActivity, "Scan complete", Toast.LENGTH_SHORT).show()
                }
                try {
                    unregisterReceiver(this)
                } catch (e: Exception) {
                    // Ignore if receiver was not registered
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(wifiReceiver)
        } catch (e: Exception) {
            // Ignore if receiver was not registered
        }
    }
}

class WifiAdapter(
    private val wifiList: List<android.net.wifi.ScanResult>,
    private val onClick: (android.net.wifi.ScanResult) -> Unit
) : RecyclerView.Adapter<WifiAdapter.WifiViewHolder>() {
    class WifiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val wifiTextView: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return WifiViewHolder(view)
    }

    override fun onBindViewHolder(holder: WifiViewHolder, position: Int) {
        val scanResult = wifiList[position]
        val ssid = scanResult.SSID.takeIf { it.isNotEmpty() } ?: "Hidden SSID"
        val signalStrength = WifiManager.calculateSignalLevel(scanResult.level, 100)
        val security = if (scanResult.capabilities.contains("WPA") || scanResult.capabilities.contains("WEP")) "Secured" else "Open"
        holder.wifiTextView.text = "$ssid (${scanResult.level} dBm, $signalStrength%, $security)"
        holder.itemView.setOnClickListener { onClick(scanResult) }
    }

    override fun getItemCount(): Int = wifiList.size
}
