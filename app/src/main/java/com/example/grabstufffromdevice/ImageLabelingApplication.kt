package com.example.grabstufffromdevice

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class ImageLabelingApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "image_labelling_channel",
                "Label Images",
                NotificationManager.IMPORTANCE_HIGH
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}