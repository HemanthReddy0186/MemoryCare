package com.memorycare.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.memorycare.app.R
import com.memorycare.app.ui.ReminderAlarmActivity
import com.memorycare.app.ui.ReminderVerificationActivity

/**
 * Foreground Service that handles system-wide alarm playback and notifications.
 *
 * Guarantees that the soothing chime plays out loud even when the app is killed,
 * in the background, or when the phone screen is locked.
 */
class ReminderAlarmService : Service() {

    companion object {
        private const val TAG = "ReminderAlarmService"
        const val CHANNEL_ID_ALARM_V3 = "memory_care_alarm_channel_v3"
        const val NOTIFICATION_ID_BASE = 9100

        const val ACTION_START_ALARM = "com.memorycare.app.ACTION_START_ALARM"
        const val ACTION_START_VERIFICATION = "com.memorycare.app.ACTION_START_VERIFICATION"
        const val ACTION_STOP_CHIME = "com.memorycare.app.ACTION_STOP_CHIME"
        const val ACTION_DISMISS_ALARM = "com.memorycare.app.ACTION_DISMISS_ALARM"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESC = "extra_desc"
        const val EXTRA_AUDIO_TEXT = "extra_audio_text"
        const val EXTRA_EMOJI = "extra_emoji"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_IS_TEST = "extra_is_test"

        var isServiceRunning = false
            private set
    }

    private var soothingPlayer: SoothingAlarmPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        soothingPlayer = SoothingAlarmPlayer(this)
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ALARM
        Log.d(TAG, "onStartCommand received action: $action")

        when (action) {
            ACTION_START_ALARM -> {
                handleStartAlarm(intent)
            }
            ACTION_START_VERIFICATION -> {
                handleStartVerification(intent)
            }
            ACTION_STOP_CHIME -> {
                soothingPlayer?.stop()
            }
            ACTION_DISMISS_ALARM -> {
                handleDismissAlarm()
            }
        }

        return START_NOT_STICKY
    }

    private fun handleStartAlarm(intent: Intent?) {
        val reminderId = intent?.getStringExtra(EXTRA_REMINDER_ID) ?: "rem_alarm"
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Medication Reminder"
        val desc = intent?.getStringExtra(EXTRA_DESC) ?: "Time for your scheduled routine."
        val audioText = intent?.getStringExtra(EXTRA_AUDIO_TEXT) ?: desc
        val emoji = intent?.getStringExtra(EXTRA_EMOJI) ?: "⏰"
        val timeFormatted = intent?.getStringExtra(EXTRA_TIME) ?: ""
        val category = intent?.getStringExtra(EXTRA_CATEGORY) ?: "Reminder"
        val isTest = intent?.getBooleanExtra(EXTRA_IS_TEST, false) ?: false

        createAlarmNotificationChannel()

        // Prepare full-screen intent for ReminderAlarmActivity
        val alarmActivityIntent = Intent(this, ReminderAlarmActivity::class.java).apply {
            putExtra(ReminderAlarmActivity.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmActivity.EXTRA_TITLE, title)
            putExtra(ReminderAlarmActivity.EXTRA_DESC, desc)
            putExtra(ReminderAlarmActivity.EXTRA_AUDIO_TEXT, audioText)
            putExtra(ReminderAlarmActivity.EXTRA_EMOJI, emoji)
            putExtra(ReminderAlarmActivity.EXTRA_TIME, timeFormatted)
            putExtra(ReminderAlarmActivity.EXTRA_CATEGORY, category)
            putExtra(ReminderAlarmActivity.EXTRA_IS_TEST, isTest)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }

        val pendingAlarmActivity = PendingIntent.getActivity(
            this,
            reminderId.hashCode(),
            alarmActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action button on notification
        val dismissIntent = Intent(this, ReminderAlarmService::class.java).apply {
            action = ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            (reminderId + "_dismiss").hashCode(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ALARM_V3)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$emoji $title")
            .setContentText(desc)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$desc\n⏰ Scheduled: $timeFormatted"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingAlarmActivity)
            .setFullScreenIntent(pendingAlarmActivity, true)
            .addAction(0, "Open Alarm", pendingAlarmActivity)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()

        val notifId = NOTIFICATION_ID_BASE + (reminderId.hashCode() % 500)

        // Start Foreground with mediaPlayback type on API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                notifId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(notifId, notification)
        }

        // 1. Gently vibrate device
        triggerGentleVibration()

        // 2. Play soothing bell chime (5s for test, 10s for scheduled)
        val chimeDuration = if (isTest) 5 else 10
        soothingPlayer?.playSoothingChime(durationSeconds = chimeDuration) {
            Log.d(TAG, "$chimeDuration-second soothing chime completed in service")
        }

        // 3. Launch full-screen activity over lock screen / other apps
        try {
            startActivity(alarmActivityIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Direct startActivity deferred to fullScreenIntent: ${e.message}")
        }
    }

    private fun handleStartVerification(intent: Intent?) {
        val reminderId = intent?.getStringExtra(EXTRA_REMINDER_ID) ?: "rem_verify"
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Medication Check-In"
        val isTest = intent?.getBooleanExtra(EXTRA_IS_TEST, false) ?: false

        createAlarmNotificationChannel()

        val verifyActivityIntent = Intent(this, ReminderVerificationActivity::class.java).apply {
            putExtra(ReminderVerificationActivity.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderVerificationActivity.EXTRA_TITLE, title)
            putExtra(ReminderVerificationActivity.EXTRA_IS_TEST, isTest)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }

        val pendingVerifyActivity = PendingIntent.getActivity(
            this,
            (reminderId + "_verify_act").hashCode(),
            verifyActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, ReminderAlarmService::class.java).apply {
            action = ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            (reminderId + "_verify_dismiss").hashCode(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ALARM_V3)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🌸 Gentle Check-In: $title")
            .setContentText("Did you finish taking your $title?")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Checking in with you... Did you finish taking your $title? No worries at all!"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingVerifyActivity)
            .setFullScreenIntent(pendingVerifyActivity, true)
            .addAction(0, "Open Check-In", pendingVerifyActivity)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()

        val notifId = NOTIFICATION_ID_BASE + 500 + (reminderId.hashCode() % 500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notifId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(notifId, notification)
        }

        triggerGentleVibration()

        // Play 3 seconds of soothing chime before AI speaks
        soothingPlayer?.playSoothingChime(durationSeconds = 3) {
            Log.d(TAG, "3-second verification chime completed in service")
        }

        try {
            startActivity(verifyActivityIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Direct verify startActivity deferred to fullScreenIntent: ${e.message}")
        }
    }

    private fun handleDismissAlarm() {
        soothingPlayer?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val existing = notificationManager?.getNotificationChannel(CHANNEL_ID_ALARM_V3)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID_ALARM_V3,
                    "Patient Medication & Safety Alarms",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical system-wide alarms for dementia patient daily medications and safety routines."
                    // Ensure silent channel so system notification sound does not fight with SoothingAlarmPlayer
                    setSound(null, null)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 150, 300)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    setBypassDnd(true)
                }
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MemoryCare:AlarmServiceWakeLock"
            )
            wakeLock?.acquire(30000L) // 30 seconds max
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock: ${e.message}")
        }
    }

    private fun triggerGentleVibration() {
        try {
            val pattern = longArrayOf(0, 350, 200, 350, 200, 350)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        soothingPlayer?.stop()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
