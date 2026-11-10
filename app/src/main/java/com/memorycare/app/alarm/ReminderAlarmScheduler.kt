package com.memorycare.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.memorycare.app.model.Reminder
import com.memorycare.app.receiver.ReminderAlarmReceiver
import com.memorycare.app.ui.ReminderAlarmActivity
import com.memorycare.app.ui.ReminderVerificationActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReminderAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    companion object {
        private const val TAG = "ReminderAlarmScheduler"
    }

    /**
     * Schedules an exact system alarm for a given reminder.
     * The alarm will fire at the reminder's scheduled time, even if the phone is asleep or locked.
     */
    fun scheduleReminderAlarm(reminder: Reminder): Boolean {
        if (reminder.isCompleted) {
            cancelReminderAlarm(reminder.id)
            return false
        }

        val triggerTimeMs = calculateNextTriggerTime(reminder.timeFormatted)
        if (triggerTimeMs <= 0L) {
            Log.w(TAG, "Could not parse reminder time: ${reminder.timeFormatted}")
            return false
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderAlarmReceiver.EXTRA_DESC, reminder.description)
            putExtra(ReminderAlarmReceiver.EXTRA_AUDIO_TEXT, reminder.audioPromptText)
            putExtra(ReminderAlarmReceiver.EXTRA_EMOJI, reminder.iconEmoji)
            putExtra(ReminderAlarmReceiver.EXTRA_TIME, reminder.timeFormatted)
            putExtra(ReminderAlarmReceiver.EXTRA_CATEGORY, reminder.category)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode() + 1000,
            Intent(context, ReminderAlarmActivity::class.java).apply {
                putExtra(ReminderAlarmActivity.EXTRA_REMINDER_ID, reminder.id)
                putExtra(ReminderAlarmActivity.EXTRA_TITLE, reminder.title)
                putExtra(ReminderAlarmActivity.EXTRA_DESC, reminder.description)
                putExtra(ReminderAlarmActivity.EXTRA_AUDIO_TEXT, reminder.audioPromptText)
                putExtra(ReminderAlarmActivity.EXTRA_EMOJI, reminder.iconEmoji)
                putExtra(ReminderAlarmActivity.EXTRA_TIME, reminder.timeFormatted)
                putExtra(ReminderAlarmActivity.EXTRA_CATEGORY, reminder.category)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scheduled = scheduleExactAlarmInternal(triggerTimeMs, pendingIntent, showIntent)
        if (scheduled) {
            Log.d(TAG, "Successfully scheduled exact AlarmClock for '${reminder.title}' at ${Date(triggerTimeMs)}")
        }
        return scheduled
    }

    /**
     * Cancels an active alarm for a reminder ID.
     */
    fun cancelReminderAlarm(reminderId: String) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for reminder ID: $reminderId")
        }
    }

    /**
     * Schedules a demo test alarm that triggers in N seconds (default 5s).
     * Ideal for testing lockscreen wake and AI voice playback immediately.
     */
    fun scheduleTestAlarm(secondsFromNow: Int = 5, customReminder: Reminder? = null): Long {
        val reminder = customReminder ?: Reminder(
            id = "rem_test_demo",
            title = "Afternoon Blood Pressure Medicine",
            description = "Please take 1 tablet of Amlodipine with a full glass of water.",
            timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
            category = "Medicine",
            iconEmoji = "💊",
            isCompleted = false,
            audioPromptText = "Attention. It is time for your afternoon blood pressure tablet. Please take 1 tablet with a full glass of lukewarm water now."
        )

        val triggerTimeMs = System.currentTimeMillis() + (secondsFromNow * 1000L)

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderAlarmReceiver.EXTRA_DESC, reminder.description)
            putExtra(ReminderAlarmReceiver.EXTRA_AUDIO_TEXT, reminder.audioPromptText)
            putExtra(ReminderAlarmReceiver.EXTRA_EMOJI, reminder.iconEmoji)
            putExtra(ReminderAlarmReceiver.EXTRA_TIME, reminder.timeFormatted)
            putExtra(ReminderAlarmReceiver.EXTRA_CATEGORY, reminder.category)
            putExtra(ReminderAlarmReceiver.EXTRA_IS_TEST, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode() + 1000,
            Intent(context, ReminderAlarmActivity::class.java).apply {
                putExtra(ReminderAlarmActivity.EXTRA_REMINDER_ID, reminder.id)
                putExtra(ReminderAlarmActivity.EXTRA_TITLE, reminder.title)
                putExtra(ReminderAlarmActivity.EXTRA_DESC, reminder.description)
                putExtra(ReminderAlarmActivity.EXTRA_AUDIO_TEXT, reminder.audioPromptText)
                putExtra(ReminderAlarmActivity.EXTRA_EMOJI, reminder.iconEmoji)
                putExtra(ReminderAlarmActivity.EXTRA_TIME, reminder.timeFormatted)
                putExtra(ReminderAlarmActivity.EXTRA_CATEGORY, reminder.category)
                putExtra(ReminderAlarmActivity.EXTRA_IS_TEST, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scheduled = scheduleExactAlarmInternal(triggerTimeMs, pendingIntent, showIntent)
        if (scheduled) {
            Log.d(TAG, "Test alarm scheduled for $secondsFromNow seconds from now")
            return triggerTimeMs
        }
        return 0L
    }

    /**
     * Schedules a snooze alarm (10 seconds for demo test, 5 minutes for regular alarms).
     */
    fun scheduleSnoozeAlarm(
        reminderId: String,
        title: String,
        description: String,
        audioText: String,
        emoji: String,
        category: String,
        isTest: Boolean = false
    ) {
        val snoozeDelayMs = if (isTest) (10 * 1000L) else (5 * 60 * 1000L)
        val snoozeMs = System.currentTimeMillis() + snoozeDelayMs
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title)
            putExtra(ReminderAlarmReceiver.EXTRA_DESC, description)
            putExtra(ReminderAlarmReceiver.EXTRA_AUDIO_TEXT, audioText)
            putExtra(ReminderAlarmReceiver.EXTRA_EMOJI, emoji)
            putExtra(ReminderAlarmReceiver.EXTRA_TIME, if (isTest) "Snoozed (10s)" else "Snoozed (+5m)")
            putExtra(ReminderAlarmReceiver.EXTRA_CATEGORY, category)
            putExtra(ReminderAlarmReceiver.EXTRA_IS_TEST, isTest)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode() + 1000,
            Intent(context, ReminderAlarmActivity::class.java).apply {
                putExtra(ReminderAlarmActivity.EXTRA_REMINDER_ID, reminderId)
                putExtra(ReminderAlarmActivity.EXTRA_TITLE, title)
                putExtra(ReminderAlarmActivity.EXTRA_DESC, description)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarmInternal(snoozeMs, pendingIntent, showIntent)
    }

    /**
     * Schedules a gentle 30-minute verification check-in alarm after the patient tapped "Done/Stop".
     * Checks if the patient actually took their medicine/routine and confirms adherence.
     */
    fun scheduleVerificationCheckIn(reminderId: String, title: String, delayMinutes: Int = 30): Long {
        val triggerTimeMs = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_TRIGGER_VERIFICATION
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + "_verify").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = PendingIntent.getActivity(
            context,
            (reminderId + "_verify_show").hashCode(),
            Intent(context, ReminderVerificationActivity::class.java).apply {
                putExtra(ReminderVerificationActivity.EXTRA_REMINDER_ID, reminderId)
                putExtra(ReminderVerificationActivity.EXTRA_TITLE, title)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scheduled = scheduleExactAlarmInternal(triggerTimeMs, pendingIntent, showIntent)
        if (scheduled) {
            Log.d(TAG, "Scheduled 30-minute verification check-in for '$title' at ${Date(triggerTimeMs)}")
            return triggerTimeMs
        }
        return 0L
    }

    /**
     * Schedules a quick 15-second verification check-in for demonstration and testing.
     */
    fun scheduleDemoVerificationCheckIn(reminderId: String, title: String, delaySeconds: Int = 15): Long {
        val triggerTimeMs = System.currentTimeMillis() + (delaySeconds * 1000L)
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_TRIGGER_VERIFICATION
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title)
            putExtra(ReminderAlarmReceiver.EXTRA_IS_TEST, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + "_verify").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = PendingIntent.getActivity(
            context,
            (reminderId + "_demo_verify_show").hashCode(),
            Intent(context, ReminderVerificationActivity::class.java).apply {
                putExtra(ReminderVerificationActivity.EXTRA_REMINDER_ID, reminderId)
                putExtra(ReminderVerificationActivity.EXTRA_TITLE, title)
                putExtra(ReminderVerificationActivity.EXTRA_IS_TEST, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scheduled = scheduleExactAlarmInternal(triggerTimeMs, pendingIntent, showIntent)
        if (scheduled) {
            Log.d(TAG, "Demo verification check-in scheduled in $delaySeconds seconds")
            return triggerTimeMs
        }
        return 0L
    }

    /**
     * Schedules an exact alarm using setAlarmClock when possible.
     * AlarmClockInfo guarantees exact firing even in low-power Doze mode,
     * wakes the display hardware, and exempts the broadcast receiver from
     * Android 10+ background activity start restrictions.
     */
    private fun scheduleExactAlarmInternal(
        triggerTimeMs: Long,
        operation: PendingIntent,
        showIntent: PendingIntent? = null
    ): Boolean {
        val mgr = alarmManager ?: return false
        try {
            val showOperation = showIntent ?: PendingIntent.getActivity(
                context,
                operation.hashCode() + 9999,
                Intent(context, ReminderAlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val clockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showOperation)
            mgr.setAlarmClock(clockInfo, operation)
            Log.d(TAG, "Successfully scheduled AlarmClock at ${Date(triggerTimeMs)}")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "setAlarmClock failed, falling back to setExactAndAllowWhileIdle: ${e.message}")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (mgr.canScheduleExactAlarms()) {
                        mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, operation)
                    } else {
                        mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, operation)
                    }
                } else {
                    mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, operation)
                }
                return true
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed scheduling exact alarm fallback: ${fallbackEx.message}")
                return false
            }
        }
    }

    /**
     * Cancels an active verification check-in.
     */
    fun cancelVerificationCheckIn(reminderId: String) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_TRIGGER_VERIFICATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + "_verify").hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled verification check-in for ID: $reminderId")
        }
    }

    /**
     * Re-synchronizes alarms for all active, uncompleted reminders.
     */
    fun syncAllReminders(reminders: List<Reminder>) {
        reminders.forEach { reminder ->
            if (!reminder.isCompleted) {
                scheduleReminderAlarm(reminder)
            } else {
                cancelReminderAlarm(reminder.id)
            }
        }
    }

    /**
     * Parses formatted time string into epoch milliseconds for today or tomorrow.
     */
    private fun calculateNextTriggerTime(timeFormatted: String): Long {
        val formats = listOf(
            "hh:mm a",
            "h:mm a",
            "HH:mm",
            "H:mm",
            "hh:mma",
            "h:mma"
        )

        var parsedDate: Date? = null
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = true
                val date = sdf.parse(timeFormatted.trim())
                if (date != null) {
                    parsedDate = date
                    break
                }
            } catch (_: Exception) {
            }
        }

        if (parsedDate == null) {
            return -1L
        }

        val timeCalendar = Calendar.getInstance().apply { time = parsedDate }
        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        // If time has already passed today, advance to tomorrow
        if (targetCalendar.timeInMillis <= now) {
            targetCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return targetCalendar.timeInMillis
    }
}
