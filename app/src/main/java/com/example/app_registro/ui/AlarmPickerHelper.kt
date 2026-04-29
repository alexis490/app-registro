package com.example.app_registro.ui

import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar
import java.util.Locale

object AlarmPickerHelper {
    fun showTimePicker(
        context: Context,
        initialMillis: Long?,
        onSelected: (Long) -> Unit
    ) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = initialMillis ?: System.currentTimeMillis()
        }

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                onSelected(selectedCalendar.timeInMillis)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun formatAlarm(millis: Long?): String {
        if (millis == null) return ""
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        return String.format(
            Locale.getDefault(),
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }
}
