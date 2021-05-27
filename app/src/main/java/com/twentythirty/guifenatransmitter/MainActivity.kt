package com.twentythirty.guifenatransmitter

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.twentythirty.guifenatransmitter.databinding.ActivityMainBinding
import com.twentythirty.guifenatransmitter.services.RecordService

class MainActivity : AppCompatActivity() {
    companion object {
        const val CHANNEL_ID = "Ntap"
        const val CHANNEL_NAME = "Info"
        const val TAG_AMPLITUDE = "amp"
        const val TAG_INTERVAL = "int"

    }

    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)
    private lateinit var binding: ActivityMainBinding
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var permissionToRecordAccepted = false
    private var minAmplitude = 0
    private var updateInterval = 1000L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
        binding.btnStart.setOnClickListener {
            startService()
        }
        binding.btnStop.setOnClickListener {
            stopService()
        }
        binding.btnSave.setOnClickListener {
            minAmplitude = binding.edtMinAmp.text.toString().toInt()
            updateInterval = binding.edtInterval.text.toString().toLong()


        }
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel: NotificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }

    fun startService() {
        val myServiceIntent = Intent(this, RecordService::class.java)
        myServiceIntent.putExtra(TAG_AMPLITUDE, minAmplitude)
        myServiceIntent.putExtra(TAG_INTERVAL, updateInterval)
        ContextCompat.startForegroundService(this, myServiceIntent)
    }

    fun stopService() {
        val serviceIntent = Intent(this, RecordService::class.java)
        stopService(serviceIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }
}