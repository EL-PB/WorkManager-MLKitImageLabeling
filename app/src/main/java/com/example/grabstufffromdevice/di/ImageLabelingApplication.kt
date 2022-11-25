package com.example.grabstufffromdevice.di

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ImageLabelingApplication: Application(), Configuration.Provider{

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder
    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    override fun getWorkManagerConfiguration(): Configuration {
      return Configuration.Builder()
              .setWorkerFactory(workerFactory)
              .build()
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager.notify(1, notificationBuilder.build())
    }
}