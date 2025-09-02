package com.example.bluetoothchat

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 100
    private val APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID
    private val APP_NAME = "BluetoothChat"
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceRecyclerView: RecyclerView
    private lateinit var chatLayout: View
    private lateinit var messageTextView: TextView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var serverButton: Button
    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var deviceAdapter: DeviceAdapter
    private var chatThread: ChatThread? = null
    private val TAG = "BluetoothChat"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        deviceRecyclerView = findViewById(R.id.deviceRecyclerView)
        chatLayout = findViewById(R.id.chatLayout)
        messageTextView = findViewById(R.id.messageTextView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        serverButton = findViewById(R.id.serverButton)

        deviceAdapter = DeviceAdapter(deviceList) { device ->
            connectToDevice(device)
        }
        deviceRecyclerView.layoutManager = LinearLayoutManager(this)
        deviceRecyclerView.adapter = deviceAdapter

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: run {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (checkPermissions()) {
            displayPairedDevices()
        } else {
            requestPermissions()
        }

        serverButton.setOnClickListener {
            if (checkPermissions()) {
                startServer()
            } else {
                requestPermissions()
            }
        }

        sendButton.setOnClickListener {
            val message = messageEditText.text.toString()
            if (message.isNotEmpty()) {
                chatThread?.sendMessage(message)
                messageTextView.append("You: $message\n")
                messageEditText.text.clear()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
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
            displayPairedDevices()
        } else {
            Toast.makeText(this, "Permissions required for Bluetooth", Toast.LENGTH_LONG).show()
        }
    }

    private fun displayPairedDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            deviceList.clear()
            deviceList.addAll(bluetoothAdapter.bondedDevices)
            deviceAdapter.notifyDataSetChanged()
        }
    }

    private fun startServer() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            ServerThread().start()
            Toast.makeText(this, "Listening for connections...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            ClientThread(device).start()
            Toast.makeText(this, "Connecting to ${device.name ?: "device"}...", Toast.LENGTH_SHORT).show()
        }
    }

    private inner class ServerThread : Thread() {
        private val serverSocket: BluetoothServerSocket? by lazy {
            bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
        }

        override fun run() {
            try {
                val socket = serverSocket?.accept()
                socket?.let {
                    runOnUiThread {
                        deviceRecyclerView.visibility = View.GONE
                        serverButton.visibility = View.GONE
                        chatLayout.visibility = View.VISIBLE
                        Toast.makeText(this@MainActivity, "Connected to client", Toast.LENGTH_SHORT).show()
                    }
                    chatThread = ChatThread(socket)
                    chatThread?.start()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Server error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private inner class ClientThread(private val device: BluetoothDevice) : Thread() {
        override fun run() {
            try {
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    val socket = device.createRfcommSocketToServiceRecord(APP_UUID)
                    bluetoothAdapter.cancelDiscovery()
                    socket.connect()
                    runOnUiThread {
                        deviceRecyclerView.visibility = View.GONE
                        serverButton.visibility = View.GONE
                        chatLayout.visibility = View.VISIBLE
                        Toast.makeText(this@MainActivity, "Connected to ${device.name ?: "device"}", Toast.LENGTH_SHORT).show()
                    }
                    chatThread = ChatThread(socket)
                    chatThread?.start()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private inner class ChatThread(private val socket: BluetoothSocket) : Thread() {
        private val input: InputStream = socket.inputStream
        private val output: OutputStream = socket.outputStream

        override fun run() {
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val bytes = input.read(buffer)
                    val message = String(buffer, 0, bytes)
                    runOnUiThread {
                        messageTextView.append("Them: $message\n")
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Chat error: ${e.message}", Toast.LENGTH_LONG).show()
                        deviceRecyclerView.visibility = View.VISIBLE
                        serverButton.visibility = View.VISIBLE
                        chatLayout.visibility = View.GONE
                    }
                    break
                }
            }
        }

        fun sendMessage(message: String) {
            try {
                output.write(message.toByteArray())
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Send error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null && !deviceList.contains(device) &&
                    ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    deviceList.add(device)
                    deviceAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // Ignore if receiver was not registered
        }
        chatThread = null
    }
}

class DeviceAdapter(
    private val devices: List<BluetoothDevice>,
    private val onClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceTextView: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        val name = if (ActivityCompat.checkSelfPermission(
                holder.itemView.context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED) {
            device.name ?: "Unknown Device"
        } else {
            "Permission Denied"
        }
        holder.deviceTextView.text = "$name (${device.address})"
        holder.itemView.setOnClickListener { onClick(device) }
    }

    override fun getItemCount(): Int = devices.size
}
