package com.memorycare.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.memorycare.app.alarm.ReminderAlarmService
import com.memorycare.app.data.LocalStorageRepository
import com.memorycare.app.model.ReminderVerificationStatus
import com.memorycare.app.ui.ReminderAlarmActivity
import com.memorycare.app.ui.ReminderVerificationActivity

/**
 * BroadcastReceiver triggered by AlarmManager at scheduled reminder times.
 * Wakes the hardware and delegates immediately to ReminderAlarmService for reliable
 * foreground audio playback, notifications, and full-screen display.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
        const val ACTION_TRIGGER_REMINDER = "com.memorycare.app.ACTION_REMINDER_ALARM"
        const val ACTION_TRIGGER_VERIFICATION = "com.memorycare.app.ACTION_TRIGGER_VERIFICATION"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESC = "extra_desc"
        const val EXTRA_AUDIO_TEXT = "extra_audio_text"
        const val EXTRA_EMOJI = "extra_emoji"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_IS_TEST = "extra_is_test"

        const val CHANNEL_ID_ALARM = ReminderAlarmService.CHANNEL_ID_ALARM_V3
        const val NOTIFICATION_ID_BASE = ReminderAlarmService.NOTIFICATION_ID_BASE
    }

    override fun onReceive(context: Context, intent: Intent) {
        val receivedAction = intent.action ?: ACTION_TRIGGER_REMINDER

        if (receivedAction == ACTION_TRIGGER_VERIFICATION) {
            handleVerificationTrigger(context, intent)
            return
        }

        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: "rem_${System.currentTimeMillis()}"
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Scheduled Medication Reminder"
        val desc = intent.getStringExtra(EXTRA_DESC) ?: "Time for your scheduled routine."
        val audioText = intent.getStringExtra(EXTRA_AUDIO_TEXT) ?: desc
        val emoji = intent.getStringExtra(EXTRA_EMOJI) ?: "⏰"
        val timeFormatted = intent.getStringExtra(EXTRA_TIME) ?: ""
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "Reminder"
        val isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)

        Log.d(TAG, "🚨 Alarm received for: $title (ID: $reminderId)")

        // 1. Acquire WakeLock to immediately wake CPU and screen
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MemoryCare:ReceiverWakeLock"
            )
            wakeLock?.acquire(15000L)
        } catch (e: Exception) {
            Log.e(TAG, "Could not acquire wakelock in receiver: ${e.message}")
        }

        // 2. Start Foreground Alarm Service to guarantee soothing audio playback & notification
        val serviceIntent = Intent(context, ReminderAlarmService::class.java).apply {
            action = ReminderAlarmService.ACTION_START_ALARM
            putExtra(ReminderAlarmService.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmService.EXTRA_TITLE, title)
            putExtra(ReminderAlarmService.EXTRA_DESC, desc)
            putExtra(ReminderAlarmService.EXTRA_AUDIO_TEXT, audioText)
            putExtra(ReminderAlarmService.EXTRA_EMOJI, emoji)
            putExtra(ReminderAlarmService.EXTRA_TIME, timeFormatted)
            putExtra(ReminderAlarmService.EXTRA_CATEGORY, category)
            putExtra(ReminderAlarmService.EXTRA_IS_TEST, isTest)
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ReminderAlarmService: ${e.message}", e)
        }

        // 3. If overlay permission is granted or device allows, start activity directly
        try {
            val alarmIntent = Intent(context, ReminderAlarmActivity::class.java).apply {
                putExtra(ReminderAlarmActivity.EXTRA_REMINDER_ID, reminderId)
                putExtra(ReminderAlarmActivity.EXTRA_TITLE, title)
                putExtra(ReminderAlarmActivity.EXTRA_DESC, desc)
                putExtra(ReminderAlarmActivity.EXTRA_AUDIO_TEXT, audioText)
                putExtra(ReminderAlarmActivity.EXTRA_EMOJI, emoji)
                putExtra(ReminderAlarmActivity.EXTRA_TIME, timeFormatted)
                putExtra(ReminderAlarmActivity.EXTRA_CATEGORY, category)
                putExtra(ReminderAlarmActivity.EXTRA_IS_TEST, isTest)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            context.startActivity(alarmIntent)
        } catch (e: Exception) {
            Log.d(TAG, "Direct launch will be handled by fullScreenIntent in service: ${e.message}")
        }
    }

    private fun handleVerificationTrigger(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Scheduled Medication"
        val isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)

        Log.d(TAG, "🌸 Verification check-in triggered for: $title (ID: $reminderId)")

        // Check if reminder is still pending verification
        try {
            val repository = LocalStorageRepository(context)
            val currentReminders = repository.getReminders()
            val reminder = currentReminders.firstOrNull { it.id == reminderId }
            if (reminder != null && reminder.verificationStatus != ReminderVerificationStatus.PENDING_VERIFICATION && !isTest) {
                Log.d(TAG, "Reminder $reminderId status is ${reminder.verificationStatus}. Skipping check-in.")
                return
            }
        } catch (_: Exception) {
        }

        // Wake screen and CPU
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MemoryCare:VerificationWakeLock"
            )
            wakeLock?.acquire(15000L)
        } catch (_: Exception) {
        }

        // 2. Start Foreground Alarm Service to guarantee soothing audio playback & notification on lockscreen
        val serviceIntent = Intent(context, ReminderAlarmService::class.java).apply {
            this.action = ReminderAlarmService.ACTION_START_VERIFICATION
            putExtra(ReminderAlarmService.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmService.EXTRA_TITLE, title)
            putExtra(ReminderAlarmService.EXTRA_IS_TEST, isTest)
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start verification ReminderAlarmService: ${e.message}", e)
        }

        // 3. If overlay permission is granted, launch activity directly
        val verifyIntent = Intent(context, ReminderVerificationActivity::class.java).apply {
            putExtra(ReminderVerificationActivity.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderVerificationActivity.EXTRA_TITLE, title)
            putExtra(ReminderVerificationActivity.EXTRA_IS_TEST, isTest)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }

        try {
            context.startActivity(verifyIntent)
        } catch (e: Exception) {
            Log.d(TAG, "Direct launch deferred to fullScreenIntent: ${e.message}")
        }
    }
}
