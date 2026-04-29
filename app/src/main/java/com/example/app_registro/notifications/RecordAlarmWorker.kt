package com.example.app_registro.notifications

import android.Manifest
import android.app.PendingIntent
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.app_registro.R
import com.example.app_registro.data.LocalStore
import com.example.app_registro.ui.RecordsActivity

class RecordAlarmWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        LocalStore.initialize(applicationContext)
        val recordId = inputData.getString(ReminderScheduler.KEY_RECORD_ID) ?: return Result.success()
        val alarmAtMillis = inputData.getLong(ReminderScheduler.KEY_RECORD_ALARM_AT, 0L)
        val record = LocalStore.getRecords().firstOrNull { it.id == recordId } ?: return Result.success()

        if (record.alarmAtMillis != alarmAtMillis || alarmAtMillis <= 0L) {
            return Result.success()
        }

        showNotification(record.productName, record.code, record.storeName)
        return Result.success()
    }

    private fun showNotification(productName: String, code: String, storeName: String) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            ReminderScheduler.RECORD_ALARM_CHANNEL_ID
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.record_alarm_title, productName))
            .setContentText(applicationContext.getString(R.string.record_alarm_body, code, storeName))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(createContentIntent())
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(productName.hashCode() + code.hashCode(), notification)
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(applicationContext, RecordsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            applicationContext,
            5010,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
