package com.simformsolutions.myspotify.ui.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import com.simformsolutions.myspotify.utils.IntentData

class NowPlayingService : Service() {

    private val binder = MyBinder()
    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mediaPlayer = MediaPlayer()
        intent?.getStringExtra(IntentData.TRACK_PREVIEW_URL)?.let { track ->
            startPlayback(track)
        }
        return START_NOT_STICKY
    }

    fun startPlayback(url: String) {
        mediaPlayer?.apply {
            stop()
            reset()
            setDataSource(url)
            setOnPreparedListener {
                start()
            }
            prepareAsync()
        }
    }

    fun resumePlayback() {
        mediaPlayer?.start()
    }

    fun pausePlayback() {
        mediaPlayer?.pause()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class MyBinder : Binder() {

        fun getService(): NowPlayingService = this@NowPlayingService
    }
}