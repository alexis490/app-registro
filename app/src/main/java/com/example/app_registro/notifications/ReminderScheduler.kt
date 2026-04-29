package com.example.app_registro.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.app_registro.data.LocalStore
import com.example.app_registro.data.Record
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val CHANNEL_ID = "pending_records_channel"
    const val RECORD_ALARM_CHANNEL_ID = "record_alarm_channel"
    private const val UNIQUE_WORK_NAME = "pending_records_reminder"
    private const val RECORD_ALARM_PREFIX = "record_alarm_"
    const val KEY_RECORD_ID = "record_id"
    const val KEY_RECORD_ALARM_AT = "record_alarm_at"
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

    fun scheduleRecordAlarm(context: Context, record: Record) {
        val alarmAt = record.alarmAtMillis ?: run {
            cancelRecordAlarm(context, record.id)
            return
        }
        val delayMillis = (alarmAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val inputData = Data.Builder()
            .putString(KEY_RECORD_ID, record.id)
            .putLong(KEY_RECORD_ALARM_AT, alarmAt)
            .build()
        val request = OneTimeWorkRequestBuilder<RecordAlarmWorker>()
            .setInputData(inputData)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            recordAlarmWorkName(record.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelRecordAlarm(context: Context, recordId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(recordAlarmWorkName(recordId))
    }

    fun rescheduleRecordAlarms(context: Context) {
        LocalStore.getRecords().forEach { record ->
            if (record.alarmAtMillis != null && record.alarmAtMillis > System.currentTimeMillis()) {
                scheduleRecordAlarm(context, record)
            } else if (record.alarmAtMillis != null) {
                cancelRecordAlarm(context, record.id)
            }
        }
    }

    private fun recordAlarmWorkName(recordId: String): String = RECORD_ALARM_PREFIX + recordId

    fun formatCloseTime(): String = LocalTime.of(CLOSE_HOUR, CLOSE_MINUTE).toString()
}
