package com.example.app_registro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.app_registro.data.LocalStore
import com.example.app_registro.notifications.ReminderScheduler

class AppRegistroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalStore.initialize(this)
        createNotificationChannel()
        ReminderScheduler.scheduleNextReminder(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            ReminderScheduler.CHANNEL_ID,
            getString(R.string.pending_records_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.pending_records_channel_description)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}
