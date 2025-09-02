package com.example.audiorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.experimental.and

class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 100
    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private lateinit var amplitudeTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private var outputFile: File? = null
    private val TAG = "AudioRecorder"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        amplitudeTextView = findViewById(R.id.amplitudeTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        stopButton.isEnabled = false

        startButton.setOnClickListener @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO) {
            if (checkPermissions()) {
                startRecording()
            } else {
                requestPermissions()
            }
        }

        stopButton.setOnClickListener {
            stopRecording()
        }
    }

    private fun checkPermissions(): Boolean {
        val recordPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val storagePermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else true // Scoped storage on Android 10+
        return recordPermission && storagePermission
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            startRecording()
        } else {
            Toast.makeText(this, "Record audio permission required", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        if (isRecording) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            )

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            outputFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "AudioRecording_$timeStamp.wav")
            } else {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AudioRecording_$timeStamp.wav")
            }

            audioRecord?.startRecording()
            isRecording = true
            startButton.isEnabled = false
            stopButton.isEnabled = true
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()

            thread {
                val buffer = ShortArray(BUFFER_SIZE)
                val tempFile = File.createTempFile("audio_temp", null, cacheDir)
                RandomAccessFile(tempFile, "rw").use { raf ->
                    while (isRecording) {
                        val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                        if (read > 0) {
                            // Calculate amplitude in dB
                            var sum = 0.0
                            for (i in 0 until read) {
                                sum += buffer[i] * buffer[i].toDouble()
                            }
                            val amplitude = Math.sqrt(sum / read)
                            val db = if (amplitude > 0) 20 * Math.log10(amplitude) else 0.0
                            runOnUiThread {
                                amplitudeTextView.text = "Amplitude: %.2f dB".format(db)
                            }
                            // Write raw PCM data to temp file
                            for (i in 0 until read) {
                                raf.writeShort(buffer[i].toInt())
                            }
                        }
                    }
                }
                // Convert to WAV after recording stops
                if (isRecording) return@thread
                writeWavFile(tempFile, outputFile!!)
                tempFile.delete()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Recording error: ${e.message}", Toast.LENGTH_LONG).show()
            stopRecording()
        }
    }

    private fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        startButton.isEnabled = true
        stopButton.isEnabled = false
        amplitudeTextView.text = "Amplitude: 0.00 dB"
        Toast.makeText(this, "Recording saved to ${outputFile?.absolutePath}", Toast.LENGTH_LONG).show()
    }

    private fun writeWavFile(input: File, output: File) {
        try {
            RandomAccessFile(input, "r").use { rafIn ->
                FileOutputStream(output).use { fos ->
                    val totalAudioLen = rafIn.length()
                    val totalDataLen = totalAudioLen + 36
                    val channels = 1
                    val sampleRate = SAMPLE_RATE
                    val byteRate = SAMPLE_RATE * 2 * channels

                    // Write WAV header
                    fos.write("RIFF".toByteArray())
                    fos.write(intToByteArray(totalDataLen.toInt()))
                    fos.write("WAVE".toByteArray())
                    fos.write("fmt ".toByteArray())
                    fos.write(intToByteArray(16)) // Subchunk1Size (16 for PCM)
                    fos.write(shortToByteArray(1)) // AudioFormat (1 for PCM)
                    fos.write(shortToByteArray(channels.toShort())) // NumChannels
                    fos.write(intToByteArray(sampleRate)) // SampleRate
                    fos.write(intToByteArray(byteRate)) // ByteRate
                    fos.write(shortToByteArray((2 * channels).toShort())) // BlockAlign
                    fos.write(shortToByteArray(16)) // BitsPerSample
                    fos.write("data".toByteArray())
                    fos.write(intToByteArray(totalAudioLen.toInt()))

                    // Write audio data
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (rafIn.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving WAV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToByteArray(value: Short): ByteArray {
        val intValue = value.toInt() // Convert Short to Int
        return byteArrayOf(
            (intValue and 0xFF).toByte(),
            ((intValue shr 8) and 0xFF).toByte() // Now shr is operating on an Int
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }
}
