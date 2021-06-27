package com.twentythirty.guifenatransmitter

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.twentythirty.guifenatransmitter.data.MainRepository
import com.twentythirty.guifenatransmitter.databinding.ActivityMainBinding
import com.twentythirty.guifenatransmitter.services.RecordService
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    companion object {
        const val CHANNEL_ID = "Ntap"
        const val CHANNEL_NAME = "Info"
        const val TAG_AMPLITUDE = "amp"
        const val TAG_INTERVAL = "int"
        const val TAG_SENSORID = "sensor_id"
        const val TAG_LOCATION = "location"
        const val TAG_SENSORNAME = "sensor_name"

    }

    private var permissions: Array<String> = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val mainRepository: MainRepository by inject()
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var permissionToRecordAccepted = false
    private var permissionToGetLocation = false
    private var permissionToGetCoarseLocation = false
    private var minAmplitude = 0
    private var updateInterval = 10000L
    private var sensorId: Int? = null
    private var sensorName: String? = null
    private var locationCoordinate: String? = null
    private var isSetting = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
        getSavedData()

        binding.textView.text = sensorName
        binding.tvCoordinate.text = locationCoordinate
        binding.edtSensorName.setText(sensorName)


        binding.btnStart.setOnClickListener {
            if (sensorName != null && locationCoordinate != null) {
                binding.txStatus.text = getString(R.string.online_status)
                binding.txStatus.setTextColor(Color.parseColor("#5E6537"))
                startService()
                Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "You need to set location and sensor name first",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.btnStop.setOnClickListener {
            binding.txStatus.text = getString(R.string.offline_status)
            binding.txStatus.setTextColor(Color.parseColor("#A60000"))
            Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show()
            stopService()
        }

        binding.btnSave.setOnClickListener {
            saveButton()
        }

        binding.btnLocation.setOnClickListener {
            fetchLocation()
        }

        binding.btnSetting.setOnClickListener {
            isSetting = !isSetting
            isSetting(isSetting)
        }

        binding.btnCancel.setOnClickListener {
            isSetting = !isSetting
            isSetting(isSetting)
        }
        createNotificationChannel()
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val task = fusedLocationProviderClient.lastLocation
        task.addOnSuccessListener {
            if (it != null) {
                locationCoordinate = "${it.latitude},${it.longitude}"
                Toast.makeText(this, "Location set to $locationCoordinate", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this, "The App Can't get the location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getSavedData() {
        getSharedPreferences("main", MODE_PRIVATE).apply {
            minAmplitude = getInt(TAG_AMPLITUDE, 70)
            updateInterval = getLong(TAG_INTERVAL, 10000L)
            sensorId = getInt(TAG_SENSORID, 0)
            sensorName = getString(TAG_SENSORNAME, null)
            locationCoordinate = getString(TAG_LOCATION, null)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun isSetting(set: Boolean) {
        if (set) {
            binding.btnSetting.visibility = View.VISIBLE
            binding.settingSection.visibility = View.GONE
        } else {
            binding.btnSetting.visibility = View.GONE
            binding.settingSection.visibility = View.VISIBLE
        }
    }

    private fun saveButton() {
        val preferences = getSharedPreferences("main", MODE_PRIVATE)

        var tempMinAmplitude = binding.edtMinAmp.text.toString()
        var tempUpdateInterval = binding.edtInterval.text.toString()
        val tempSensorName = binding.edtSensorName.text.toString()

        if (tempMinAmplitude.isBlank()) {
            tempMinAmplitude = 70.toString()
        }

        if (tempUpdateInterval.isBlank()) {
            tempUpdateInterval = 10.toString()
        }

        if (tempSensorName.isNotBlank() && locationCoordinate != null) {
            isSetting = !isSetting
            isSetting(isSetting)


            val prefLocation = preferences.getString(TAG_LOCATION, null)
            if (tempSensorName != sensorName || locationCoordinate != prefLocation){
                addSensor(tempSensorName, locationCoordinate)
            }

            //assign EditText value to variable
            val toSecond = tempUpdateInterval.toLong() * 1000
            minAmplitude = tempMinAmplitude.toInt()
            updateInterval = toSecond
            sensorName = tempSensorName

            binding.textView.text = sensorName
            binding.tvCoordinate.text = locationCoordinate

            val text =
                "Amplitude threshold set to $tempMinAmplitude dB \n Record Interval set to $tempUpdateInterval s'"
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

            //save value to shared pref
            preferences.edit().apply {
                putInt(TAG_AMPLITUDE, minAmplitude)
                putLong(TAG_INTERVAL, updateInterval)
                putString(TAG_SENSORNAME, sensorName)
                putString(TAG_LOCATION, locationCoordinate)
                apply()
            }
        } else {
            Toast.makeText(
                this,
                "Field sensor name must be fill and location must be set",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun startService() {
        val myServiceIntent = Intent(this, RecordService::class.java)
        myServiceIntent.putExtra(TAG_AMPLITUDE, minAmplitude)
        myServiceIntent.putExtra(TAG_INTERVAL, updateInterval)
        myServiceIntent.putExtra(TAG_SENSORID, sensorId)
        ContextCompat.startForegroundService(this, myServiceIntent)
    }

    private fun stopService() {
        val serviceIntent = Intent(this, RecordService::class.java)
        stopService(serviceIntent)
    }

    private fun addSensor(name: String?, location: String?) {
        this.lifecycleScope.launch {
            val response = mainRepository.addSensor(name!!, location!!)

            if (response.isSuccessful) {
                Log.d("farin", "${response.code()} : ${response.body()?.sensor_id}")

                sensorId = response.body()?.sensor_id
                val preferences = getSharedPreferences("main", MODE_PRIVATE)
                preferences.edit().apply {
                    putInt(TAG_SENSORID, sensorId!!)
                    apply()
                }
            }
        }
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

        permissionToGetLocation = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[1] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToGetLocation) finish()

        permissionToGetCoarseLocation = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[1] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToGetCoarseLocation) finish()
    }
}