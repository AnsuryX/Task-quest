package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mode = intent.getStringExtra("mode") ?: "work"
        val workSoundIndex = intent.getIntExtra("work_sound_setting", 0)
        val breakSoundIndex = intent.getIntExtra("break_sound_setting", 0)
        
        val title = if (mode == "work") {
            "🛡️ FOCUS TIMER COMPLETED"
        } else {
            "🍀 REST PERIOD COMPLETED"
        }
        
        val body = if (mode == "work") {
            "Excellent grind, Ranger! Take a moment to breathe and relax."
        } else {
            "Rest interval finished. Return to the matrix for your next quest!"
        }
        
        // Enforce physical DND reset if active
        NotificationAndSoundHelper.setSystemDndMode(context, false)
        
        // Fire Notification
        NotificationAndSoundHelper.sendNotification(
            context = context,
            channelId = NotificationAndSoundHelper.CHANNEL_POMODORO_ID,
            notificationId = 10101,
            title = title,
            text = body
        )
        
        // Play synthezised tone sequence
        if (mode == "work") {
            NotificationAndSoundHelper.playWorkFinishedCustomSound(context, workSoundIndex)
        } else {
            NotificationAndSoundHelper.playBreakFinishedCustomSound(context, breakSoundIndex)
        }
    }
}
