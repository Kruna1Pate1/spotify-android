package com.simformsolutions.myspotify

import android.app.Application
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.simformsolutions.myspotify.utils.AppConstants
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MySpotifyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeNotificationChannels()
    }

    /**
     * Initializes the required notification channels for the app.
     */
    private fun initializeNotificationChannels() {
        val nowPlayingChannel = NotificationChannelCompat.Builder(
            AppConstants.NOW_PLAYING_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW
        ).setName("Now playing").setDescription("Now playing notification").build()

        with(NotificationManagerCompat.from(this)) {
            createNotificationChannelsCompat(listOf(nowPlayingChannel))
        }
    }
}