package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import com.example.data.model.Task
import java.util.TimeZone

object CalendarSyncHelper {

    fun insertTaskToCalendarIntent(context: Context, task: Task) {
        val startTime = task.plannedDate ?: System.currentTimeMillis()
        val endTime = startTime + (task.targetPomodoros * 25 * 60 * 1000L).coerceAtLeast(30 * 60 * 1000L) // calculate duration

        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "⚔️ Quest: ${task.title}")
                putExtra(CalendarContract.Events.DESCRIPTION, "Quest details:\n${task.notes}\n\nLife Sector: ${task.sector}\nReward: +${task.xpReward} XP\nTarget Pomodoros: ${task.targetPomodoros}")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                putExtra(CalendarContract.Events.EVENT_LOCATION, "Sector: ${task.sector}")
                putExtra(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                putExtra(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("CalendarSyncHelper", "Error launching calendar intent", e)
        }
    }

    fun syncTaskToCalendarSilently(context: Context, task: Task): Boolean {
        // Safe check for permissions before running content resolver
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            Log.w("CalendarSyncHelper", "WRITE_CALENDAR permission not granted.")
            return false
        }

        return try {
            val startTime = task.plannedDate ?: System.currentTimeMillis()
            val endTime = startTime + (task.targetPomodoros * 25 * 60 * 1000L).coerceAtLeast(30 * 60 * 1000L)

            // Find first visible calendar
            val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            
            var calendarId: Long = 1 // default fallback
            cursor?.use {
                if (it.moveToFirst()) {
                    val idCol = it.getColumnIndex(CalendarContract.Calendars._ID)
                    if (idCol >= 0) {
                        calendarId = it.getLong(idCol)
                    }
                }
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startTime)
                put(CalendarContract.Events.DTEND, endTime)
                put(CalendarContract.Events.TITLE, "⚔️ Quest: ${task.title}")
                put(CalendarContract.Events.DESCRIPTION, "Quest notes:\n${task.notes}\n\nSector: ${task.sector}\nReward: +${task.xpReward} XP")
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri != null
        } catch (e: Exception) {
            Log.e("CalendarSyncHelper", "Error syncing task silently", e)
            false
        }
    }
}
