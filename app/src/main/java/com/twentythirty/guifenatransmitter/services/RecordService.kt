package com.twentythirty.guifenatransmitter.services

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.twentythirty.guifenatransmitter.MainActivity
import com.twentythirty.guifenatransmitter.R
import java.io.File
import java.io.IOException
import kotlin.math.abs
import kotlin.math.log10


class RecordService : Service() {
    private lateinit var mRecorder: MediaRecorder
    private var amplitudeValue = 0
    private var amplitudeDb = 0.0
    private var referenceAmplitude = 70
    private var updateInterval = 1000L
    private lateinit var mainHandler: Handler
    private lateinit var file: File
    private lateinit var mp: MediaPlayer
    private var isRecording = false
    private val updateAmplitude = object : Runnable {
        override fun run() {
            val tempAplitude = mRecorder.maxAmplitude
            if (tempAplitude > amplitudeValue) {
                amplitudeValue = tempAplitude
            }
            amplitudeDb = 20 * log10(abs(amplitudeValue).toDouble())
            updateNotification("Recording...", "Amplitude: $amplitudeDb")
            mainHandler.postDelayed(this, updateInterval)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        referenceAmplitude = intent!!.getIntExtra(MainActivity.TAG_AMPLITUDE, 70)
        updateInterval = intent.getLongExtra(MainActivity.TAG_INTERVAL, 10000L)
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
        if (mp.isPlaying) {
            mp.stop()
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
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        mainHandler.post(updateAmplitude)
        mainHandler.postDelayed({ stopRecord() }, 10000L)
    }

    private fun stopRecord() {
        if (isRecording) {
            mRecorder.stop()
            isRecording = false
        }
        if (amplitudeDb > referenceAmplitude) {
            //Upload to cloud
            updateNotification("Playing sound..", "denger")
            mp = MediaPlayer()
            mp.apply {
                setDataSource(file.toString())
                prepare()
                start()
            }
            //"ADD COROUTINE FOR UPLOADING")
            mainHandler.removeCallbacks(updateAmplitude)
            mRecorder.release()
            mainHandler.postDelayed({ startRecord() }, 10000L)
        } else {
            mainHandler.removeCallbacks(updateAmplitude)
            mRecorder.release()
            mainHandler.postDelayed({ startRecord() }, 10000L)
        }

    }

    fun updateNotification(title: String, subTitle: String) {
        Log.d("farin", "cantik")
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
}