package com.twentythirty.guifenatransmitter.services

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.twentythirty.guifenatransmitter.MainActivity
import com.twentythirty.guifenatransmitter.R
import com.twentythirty.guifenatransmitter.data.MainRepository
import com.twentythirty.guifenatransmitter.data.PayloadModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File
import kotlin.math.abs
import kotlin.math.log10


class RecordService : Service() {
    private lateinit var mRecorder: MediaRecorder
    private val mainRepository: MainRepository by inject()
    private var sensorId = 0
    private var amplitudeValue = 0
    private var amplitudeDb = 0.0
    private var postApiStatus = ""
    private var referenceAmplitude = 70
    private var updateInterval = 20000L
    private lateinit var mainHandler: Handler
    private lateinit var file: File
    private lateinit var mp: MediaPlayer
    private var isRecording = false
    private val updateAmplitude = object : Runnable {
        override fun run() {
            val tempAmplitude = mRecorder.maxAmplitude
            if (tempAmplitude > amplitudeValue) {
                amplitudeValue = tempAmplitude
            }
            amplitudeDb = 20 * log10(abs(amplitudeValue).toDouble())
            updateNotification(
                "Recording...",
                "Amplitude: $amplitudeDb\nReference amp: $referenceAmplitude"
            )
            mainHandler.postDelayed(this, 1000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sensorId = intent!!.getIntExtra(MainActivity.TAG_SENSORID, 0)
        referenceAmplitude = intent.getIntExtra(MainActivity.TAG_AMPLITUDE, 70)
        updateInterval = intent.getLongExtra(MainActivity.TAG_INTERVAL, 20000L)
        mainHandler = Handler(Looper.getMainLooper())
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val notification = NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
            .setContentTitle("Starting recorder...")
            .setContentText("please wait")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
        startRecord()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        if (isRecording) {
            mRecorder.stop()
        }
        if (::mp.isInitialized) {
            if (mp.isPlaying) {
                mp.stop()
            }
        }
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun startRecord() {
        if (::mp.isInitialized) {
            if (mp.isPlaying) {
                mp.stop()
            }
        }
        amplitudeValue = 0
        isRecording = true
        file = File(this.filesDir, "tes.m4a")
        mRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.toString())
        }
        try {
            mRecorder.prepare()
            mRecorder.start()
            Log.d("FARIN", "started")
        } catch (e: Exception) {
            Log.d("FARIN", e.toString())
        }
        mainHandler.post(updateAmplitude)
        mainHandler.postDelayed({ stopRecord() }, 10000)
    }

    private fun stopRecord() {
        if (isRecording) {
            mRecorder.stop()
            mRecorder.reset()
            isRecording = false
        }
        if (amplitudeDb > referenceAmplitude) {
            Log.d("farin", "Sound detected. Start Post Request...")
            val payloadModel = PayloadModel(
                audio = convertBase64(file),
                sensor_id = sensorId
            )

            postAudio(payloadModel.audio, payloadModel.sensor_id)
        } else {
            Log.d("farin", "No sound detected")
            postAudio(null, 1)

            mainHandler.removeCallbacks(updateAmplitude)
            mRecorder.reset()
            mRecorder.release()
            updateNotification("No sound", "Starting recorder..")
            mainHandler.postDelayed({ startRecord() }, updateInterval)
        }

    }

    private fun convertBase64(file: File): String =
        Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)

    private fun largeLog(tag: String?, content: String) {
        if (content.length > 4000) {
            Log.d(tag, content.substring(0, 4000))
            largeLog(tag, content.substring(4000))
        } else {
            Log.d(tag, content)
        }
    }

    fun updateNotification(title: String, subTitle: String) {
//        Log.d("farin", "cantik")
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val notification = NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subTitle)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setSound(null)
            .build()
        with(NotificationManagerCompat.from(this)) {
            notify(1, notification)
        }
    }

    private fun postAudio(audio: String?, sensorId: Int){
        GlobalScope.launch(Dispatchers.IO) {
            val response = mainRepository.pushPost(audio, sensorId)
            if (response.isSuccessful){
                Log.d("farin", "${response.code()} : ${response.message()}")
                postApiStatus = "${response.code()} : ${response.message()}"

                mainHandler.removeCallbacks(updateAmplitude)
                mRecorder.release()

                updateNotification("Post Request Status", postApiStatus)

                mainHandler.postDelayed({ startRecord() }, updateInterval)
            } else{
                Log.e("farin", "${response.code()} : ${response.message()}")
                postApiStatus = "${response.code()} : ${response.message()}"

                mainHandler.removeCallbacks(updateAmplitude)
                mRecorder.release()

                updateNotification(postApiStatus, "starting re-record")

                mainHandler.postDelayed({ startRecord() }, updateInterval)
            }
        }
    }
}