package com.example.app_registro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.app_registro.data.LocalStore
import com.example.app_registro.notifications.ReminderScheduler

class AppRegistroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalStore.initialize(this)
        createNotificationChannel()
        ReminderScheduler.scheduleNextReminder(this)
        ReminderScheduler.rescheduleRecordAlarms(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val reminderChannel = NotificationChannel(
            ReminderScheduler.CHANNEL_ID,
            getString(R.string.pending_records_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.pending_records_channel_description)
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .build()
        val recordAlarmChannel = NotificationChannel(
            ReminderScheduler.RECORD_ALARM_CHANNEL_ID,
            getString(R.string.record_alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.record_alarm_channel_description)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            setSound(defaultSoundUri, audioAttributes)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(reminderChannel)
        manager?.createNotificationChannel(recordAlarmChannel)
    }
}
