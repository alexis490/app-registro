package com.example.app_registro.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.app_registro.R
import com.example.app_registro.data.LocalStore

class ReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        LocalStore.initialize(applicationContext)
        val pendingRecords = LocalStore.getPendingRecordsForReminder()
        if (pendingRecords.isNotEmpty()) {
            showNotification(pendingRecords.size)
        }
        ReminderScheduler.scheduleNextReminder(applicationContext)
        return Result.success()
    }

    private fun showNotification(pendingCount: Int) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.pending_records_notification_title))
            .setContentText(
                applicationContext.getString(
                    R.string.pending_records_notification_body,
                    pendingCount
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, notification)
    }
}
