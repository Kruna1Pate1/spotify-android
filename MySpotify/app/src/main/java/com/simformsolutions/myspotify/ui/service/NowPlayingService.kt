package com.simformsolutions.myspotify.ui.service

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.IntentCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.simformsolutions.myspotify.R
import com.simformsolutions.myspotify.data.model.local.ItemType
import com.simformsolutions.myspotify.data.model.local.TrackItem
import com.simformsolutions.myspotify.ui.activity.MainActivity
import com.simformsolutions.myspotify.utils.AppConstants
import com.simformsolutions.myspotify.utils.IntentData
import androidx.media.app.NotificationCompat as MediaNotificationCompat

class NowPlayingService : Service() {

    private val binder = MyBinder()
    private val mediaPlayer = MediaPlayer()
    private val receiver = NowPlayingReceiver()
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        setupNotification()
        registerReceiver(receiver, IntentFilter().apply {
            addAction(IntentData.ACTION_TOGGLE_PLAYBACK)
        })
        notificationManager = NotificationManagerCompat.from(this)

        IntentCompat.getParcelableExtra(intent, IntentData.TRACK_ITEM, TrackItem::class.java)
            ?.let { track ->
                setupNotificationTrack(track)
                startPlayback(track)
            }
        return START_NOT_STICKY
    }

    private fun setupNotification() {
        notificationBuilder = NotificationCompat.Builder(this, AppConstants.NOW_PLAYING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOW_PLAYING_NOTIFICATION_ID,
                notificationBuilder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        }
    }

    private fun setupNotificationTrack(trackItem: TrackItem) {
        val args = bundleOf(
            "trackId" to trackItem.id,
            "id" to trackItem.id,
            "type" to ItemType.TRACK
        )

        val nowPlayingIntent = NavDeepLinkBuilder(this)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.nowPlayingFragment, args)
            .createPendingIntent()

        notificationBuilder
            .setContentTitle(trackItem.title)
            .setLargeIcon(null)
            .setColorized(true)
            .setContentText(trackItem.artists)
            .setContentIntent(nowPlayingIntent)
            .setFullScreenIntent(PendingIntent.getActivity(this, 4, Intent(), PendingIntent.FLAG_IMMUTABLE), true)
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(1)
            )
        setupActions(mediaPlayer.isPlaying)
        Glide.with(this)
            .asBitmap()
            .load(trackItem.image)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    notificationBuilder.setLargeIcon(resource)
                    postNotification()
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun setupActions(isPlaying: Boolean) {
        notificationBuilder
            .setOngoing(isPlaying)
            .clearActions()
            // Previous track action
            .addAction(
                R.drawable.ic_previous_24,
                "Previous",
                Intent(IntentData.ACTION_PREVIOUS_TRACK).let { intent ->
                    PendingIntent.getBroadcast(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                })
            // Play/Pause action
            .addAction(
                if (isPlaying) R.drawable.ic_pause_24 else R.drawable.ic_play_24,
                if (isPlaying) "Pause" else "Play",
                Intent(IntentData.ACTION_TOGGLE_PLAYBACK).let { intent ->
                    PendingIntent.getBroadcast(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                })
            // Next track action
            .addAction(
                R.drawable.ic_next_24,
                "Next",
                Intent(IntentData.ACTION_NEXT_TRACK).let { intent ->
                    PendingIntent.getBroadcast(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                })
        postNotification()
    }

    private fun postNotification() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        notificationManager.notify(NOW_PLAYING_NOTIFICATION_ID, notificationBuilder.build())
    }

    fun startPlayback(trackItem: TrackItem) {
        setupNotificationTrack(trackItem)
        mediaPlayer.apply {
            stop()
            reset()
            setDataSource(trackItem.previewUrl)
            setOnPreparedListener {
                sendBroadcast(Intent(IntentData.ACTION_RESUME_PLAYBACK))
                start()
                setupActions(true)
            }
            prepareAsync()
        }
    }

    fun resumePlayback() {
        mediaPlayer.start()
        setupActions(true)
    }

    fun pausePlayback() {
        mediaPlayer.pause()
        setupActions(false)
    }

    fun isPlaying(): Boolean = mediaPlayer.isPlaying

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    inner class MyBinder : Binder() {

        fun getService(): NowPlayingService = this@NowPlayingService
    }

    private inner class NowPlayingReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                IntentData.ACTION_TOGGLE_PLAYBACK -> {
                    if (isPlaying()) {
                        pausePlayback()
                        setupActions(false)
                        sendBroadcast(Intent(IntentData.ACTION_PAUSE_PLAYBACK))
                    } else {
                        resumePlayback()
                        setupActions(true)
                        sendBroadcast(Intent(IntentData.ACTION_RESUME_PLAYBACK))
                    }
                }
            }
        }
    }

    companion object {
        const val NOW_PLAYING_NOTIFICATION_ID = 1
    }
}