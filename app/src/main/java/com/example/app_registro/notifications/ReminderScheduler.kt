package com.example.app_registro.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val CHANNEL_ID = "pending_records_channel"
    private const val UNIQUE_WORK_NAME = "pending_records_reminder"
    private const val CLOSE_HOUR = 21
    private const val CLOSE_MINUTE = 0

    fun scheduleNextReminder(context: Context) {
        val now = LocalDateTime.now()
        var nextRun = now.withHour(CLOSE_HOUR).withMinute(CLOSE_MINUTE).withSecond(0).withNano(0)
        if (!now.isBefore(nextRun)) {
            nextRun = nextRun.plusDays(1)
        }

        val delayMillis = Duration.between(now, nextRun).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun formatCloseTime(): String = LocalTime.of(CLOSE_HOUR, CLOSE_MINUTE).toString()
}
