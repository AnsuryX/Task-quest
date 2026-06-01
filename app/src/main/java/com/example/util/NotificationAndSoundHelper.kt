package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object NotificationAndSoundHelper {

    const val CHANNEL_ALERTS_ID = "chronos_alerts"
    const val CHANNEL_POMODORO_ID = "pomodoro_timer"

    /**
     * Initializes high-priority notification channels so alerts make sound and vibrate on modern devices.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Chronicles & Motivation Alerts Channel (High Priority)
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Chronos RPG Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tactical strategy alerts, daily goals and motivation updates."
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(alertsChannel)

            // 2. Focus & Pomodoro Timer Completed Channel (High Priority)
            val pomodoroChannel = NotificationChannel(
                CHANNEL_POMODORO_ID,
                "Pomodoro Focus Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Work session, short rest and deep rest completion alarms."
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(pomodoroChannel)
        }
    }

    /**
     * Sends a push notification with sound and vibrant styling.
     */
    fun sendNotification(context: Context, channelId: String, notificationId: Int, title: String, text: String) {
        // Enforce runtime permission checks for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Cannot post notification because permission is lacking
                return
            }
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // sound, flashlights, vibration
            .setSound(defaultSoundUri)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // --- High Fidelity Arcade Game Sound Synthesizers via ToneGenerator ---
    
    /**
     * Plays a customizable chime based on selected user sound preference theme for completed work.
     */
    fun playWorkFinishedCustomSound(context: Context, themeIndex: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                when (themeIndex) {
                    0 -> { // Arcade
                        tg.startTone(ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE, 200)
                        delay(250)
                        tg.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                        delay(150)
                        tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 300)
                    }
                    1 -> { // Classic Digital
                        for (i in 1..4) {
                            tg.startTone(ToneGenerator.TONE_SUP_DIAL, 80)
                            delay(160)
                        }
                    }
                    2 -> { // Zen Gong
                        tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 800)
                    }
                    3 -> { // Cosmic Wave
                        tg.startTone(ToneGenerator.TONE_PROP_PROMPT, 150)
                        delay(120)
                        tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
                        delay(320)
                        tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 200)
                    }
                }
                tg.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        playDefaultDeviceSound(context, RingtoneManager.TYPE_ALARM, 1800)
    }

    /**
     * Plays a customizable chime based on selected user sound preference theme for completed break.
     */
    fun playBreakFinishedCustomSound(context: Context, themeIndex: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                when (themeIndex) {
                    0 -> { // Arcade
                        tg.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 200)
                        delay(200)
                        tg.startTone(ToneGenerator.TONE_CDMA_PIP, 250)
                    }
                    1 -> { // Classic Digital
                        tg.startTone(ToneGenerator.TONE_SUP_DIAL, 120)
                        delay(180)
                        tg.startTone(ToneGenerator.TONE_SUP_DIAL, 120)
                    }
                    2 -> { // Zen Gong
                        tg.startTone(ToneGenerator.TONE_CDMA_PIP, 400)
                    }
                    3 -> { // Cosmic Wave
                        tg.startTone(ToneGenerator.TONE_CDMA_PIP, 100)
                        delay(120)
                        tg.startTone(ToneGenerator.TONE_SUP_PIP, 200)
                    }
                }
                tg.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        playDefaultDeviceSound(context, RingtoneManager.TYPE_NOTIFICATION, 1200)
    }

    /**
     * Plays a triumphant, retro arcade game chime for completed work sessions (fallback).
     */
    fun playWorkFinishedSound(context: Context) {
        playWorkFinishedCustomSound(context, 0)
    }

    /**
     * Plays a breezy, light double ding for completed break durations (fallback).
     */
    fun playBreakFinishedSound(context: Context) {
        playBreakFinishedCustomSound(context, 0)
    }

    /**
     * Plays an futuristic lock-on sound when Focus Shield (DND) is armed.
     */
    fun playShieldEngagedSound() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                tg.startTone(ToneGenerator.TONE_PROP_PROMPT, 150)
                delay(180)
                tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
                tg.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Plays a soft power-down unlock sound when Focus Shield ends or pauses.
     */
    fun playShieldDisengagedSound() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
                tg.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 150)
                delay(180)
                tg.startTone(ToneGenerator.TONE_SUP_PIP, 200)
                tg.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Plays a grand positive level up chime sequence!
     */
    fun playLevelUpSound() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                for (i in 1..3) {
                    tg.startTone(ToneGenerator.TONE_CDMA_PIP, 100)
                    delay(120)
                }
                tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 400)
                tg.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playDefaultDeviceSound(context: Context, ringtoneType: Int, durationMs: Long) {
        try {
            val alertUri = RingtoneManager.getDefaultUri(ringtoneType)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, alertUri)
            ringtone?.play()
            // Stop it after the specified timer duration so it doesn't ring forever
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    if (ringtone?.isPlaying == true) {
                        ringtone.stop()
                    }
                } catch (e: Exception) {}
            }, durationMs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Do Not Disturb System Policy Access ---

    /**
     * Checks whether we have Notification Policy Access/DND controls granted by the user.
     */
    fun isDndPermissionGranted(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
    }

    /**
     * Navigates the user to Settings to grant Notification Policy/DND permissions.
     */
    fun requestDndPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Controls the device's actual physical DND mode!
     * Silences all notifications when isZoneActive is true, and restores normal status when false.
     */
    fun setSystemDndMode(context: Context, isZoneActive: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            try {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    if (isZoneActive) {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                    } else {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}
