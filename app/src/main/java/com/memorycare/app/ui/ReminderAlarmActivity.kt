package com.memorycare.app.ui

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.alarm.ReminderAlarmScheduler
import com.memorycare.app.alarm.ReminderAlarmService
import com.memorycare.app.alarm.SoothingAlarmPlayer
import com.memorycare.app.data.LocalStorageRepository
import com.memorycare.app.model.ReminderVerificationStatus
import com.memorycare.app.ui.theme.ActionPrimaryBlue
import com.memorycare.app.ui.theme.ActionSuccessGreen
import com.memorycare.app.ui.theme.AlertAmber
import com.memorycare.app.ui.theme.CriticalRed
import com.memorycare.app.ui.theme.DeepSpaceNavy
import com.memorycare.app.ui.theme.OrbitalCyan
import com.memorycare.app.ui.theme.SatelliteGreen
import com.memorycare.app.ui.theme.SpaceNavyCard
import com.memorycare.app.ui.theme.SpaceNavySurface
import kotlinx.coroutines.delay
import java.util.Locale

class ReminderAlarmActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESC = "extra_desc"
        const val EXTRA_AUDIO_TEXT = "extra_audio_text"
        const val EXTRA_EMOJI = "extra_emoji"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_IS_TEST = "extra_is_test"
    }

    private var soothingPlayer: SoothingAlarmPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private lateinit var reminderId: String
    private lateinit var title: String
    private lateinit var description: String
    private lateinit var audioPromptText: String
    private lateinit var emoji: String
    private lateinit var timeFormatted: String
    private lateinit var category: String
    private var isTest: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configure Window to wake device and show over lock screen
        configureLockscreenFlags()

        // 2. Extract Intent Extras
        reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: "rem_unknown"
        title = intent.getStringExtra(EXTRA_TITLE) ?: "Medication Reminder"
        description = intent.getStringExtra(EXTRA_DESC) ?: "Time for your scheduled care routine."
        audioPromptText = intent.getStringExtra(EXTRA_AUDIO_TEXT) ?: description
        emoji = intent.getStringExtra(EXTRA_EMOJI) ?: "💊"
        timeFormatted = intent.getStringExtra(EXTRA_TIME) ?: ""
        category = intent.getStringExtra(EXTRA_CATEGORY) ?: "Medicine"
        isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)

        // 3. Initialize Text-to-Speech Engine
        textToSpeech = TextToSpeech(this, this)

        // 4. Ensure soothing alarm audio is playing (5s for test, 10s for scheduled)
        val chimeDuration = if (isTest) 5 else 10
        if (!ReminderAlarmService.isServiceRunning) {
            soothingPlayer = SoothingAlarmPlayer(this)
            soothingPlayer?.playSoothingChime(durationSeconds = chimeDuration)
        }

        setContent {
            ReminderAlarmContent(
                reminderId = reminderId,
                title = title,
                description = description,
                audioText = audioPromptText,
                emoji = emoji,
                timeFormatted = timeFormatted,
                category = category,
                isTest = isTest,
                onStopChimeAndSpeak = {
                    stopSoothingChime()
                    speakDescription()
                },
                onCompleted = {
                    markReminderCompleted()
                    stopAllAudioAndFinish()
                },
                onSnooze = {
                    snoozeReminder()
                    stopAllAudioAndFinish()
                },
                onReplayVoice = {
                    speakDescription()
                },
                onDismiss = {
                    stopAllAudioAndFinish()
                }
            )
        }
    }

    private fun configureLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }

    override fun onResume() {
        super.onResume()
        configureLockscreenFlags()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        configureLockscreenFlags()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureLockscreenFlags()
    }

    private fun stopSoothingChime() {
        soothingPlayer?.stop()
        try {
            val stopIntent = Intent(this, ReminderAlarmService::class.java).apply {
                action = ReminderAlarmService.ACTION_STOP_CHIME
            }
            startService(stopIntent)
        } catch (_: Exception) {
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            textToSpeech?.language = Locale.getDefault()
            // Slower, measured speech rate (0.72f) so dementia patients can clearly understand every word
            textToSpeech?.setSpeechRate(0.72f)
            textToSpeech?.setPitch(0.96f) // Warm, gentle tone
        }
    }

    private fun speakDescription() {
        stopSoothingChime()
        if (!isTtsReady || textToSpeech == null) return

        // Use natural pauses with punctuation so speech is gentle and unhurried
        val speechText = if (audioPromptText.isNotBlank()) {
            "Attention... $audioPromptText"
        } else {
            "Attention... It is time for your $title... $description"
        }

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioAttributes.USAGE_ALARM)
        }
        textToSpeech?.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, "alarm_tts_utterance")
    }

    private fun markReminderCompleted() {
        try {
            val repository = LocalStorageRepository(this)
            val currentReminders = repository.getReminders()
            val updated = currentReminders.map { reminder ->
                if (reminder.id == reminderId) {
                    reminder.copy(
                        isCompleted = true,
                        verificationStatus = ReminderVerificationStatus.PENDING_VERIFICATION,
                        completedAtTimestamp = System.currentTimeMillis()
                    )
                } else reminder
            }
            repository.saveReminders(updated)
            // Cancel notification
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(ReminderAlarmService.NOTIFICATION_ID_BASE + (reminderId.hashCode() % 500))

            // Schedule 30-minute gentle verification check-in (or 15s in test mode)
            val scheduler = ReminderAlarmScheduler(this)
            if (isTest) {
                scheduler.scheduleDemoVerificationCheckIn(reminderId, title, delaySeconds = 15)
            } else {
                scheduler.scheduleVerificationCheckIn(reminderId, title, delayMinutes = 30)
            }
        } catch (_: Exception) {
        }
    }

    private fun snoozeReminder() {
        try {
            val scheduler = ReminderAlarmScheduler(this)
            scheduler.scheduleSnoozeAlarm(reminderId, title, description, audioPromptText, emoji, category, isTest)
        } catch (_: Exception) {
        }
    }

    private fun stopAllAudioAndFinish() {
        stopSoothingChime()
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {
        }
        try {
            val dismissIntent = Intent(this, ReminderAlarmService::class.java).apply {
                action = ReminderAlarmService.ACTION_DISMISS_ALARM
            }
            startService(dismissIntent)
        } catch (_: Exception) {
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(ReminderAlarmService.NOTIFICATION_ID_BASE + (reminderId.hashCode() % 500))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSoothingChime()
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {
        }
    }
}

@Composable
fun ReminderAlarmContent(
    reminderId: String,
    title: String,
    description: String,
    audioText: String,
    emoji: String,
    timeFormatted: String,
    category: String,
    isTest: Boolean,
    onStopChimeAndSpeak: () -> Unit,
    onCompleted: () -> Unit,
    onSnooze: () -> Unit,
    onReplayVoice: () -> Unit,
    onDismiss: () -> Unit
) {
    val initialChimeSeconds = if (isTest) 5 else 10
    var chimeSecondsLeft by remember(isTest) { mutableIntStateOf(initialChimeSeconds) }
    var isChimeActive by remember { mutableStateOf(true) }
    var isVoiceSpeaking by remember { mutableStateOf(false) }

    // Chime Countdown Timer: after initialChimeSeconds (5s for test, 10s for normal), automatically stop chime and speak description!
    LaunchedEffect(isTest) {
        while (chimeSecondsLeft > 0) {
            delay(1000L)
            chimeSecondsLeft--
        }
        isChimeActive = false
        isVoiceSpeaking = true
        onStopChimeAndSpeak()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "alarmBeacon")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712),
                        DeepSpaceNavy,
                        Color(0xFF0F172A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // =========================================================================
            // TOP BADGE: Full System Alert Status
            // =========================================================================
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isChimeActive) OrbitalCyan.copy(alpha = 0.2f) else SatelliteGreen.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (isChimeActive) OrbitalCyan else SatelliteGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isChimeActive) "🎵 SOOTHING ALARM CHIME" else "🔊 AI VOICE ANNOUNCEMENT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isChimeActive) OrbitalCyan else SatelliteGreen,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Pulsing Icon Beacon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                (if (isChimeActive) OrbitalCyan else SatelliteGreen).copy(alpha = 0.18f)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(SpaceNavyCard)
                            .border(2.dp, if (isChimeActive) OrbitalCyan else SatelliteGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 42.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                if (timeFormatted.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scheduled: $timeFormatted • $category",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OrbitalCyan
                    )
                }
            }

            // =========================================================================
            // CENTER: Instruction Card & Countdown Telemetry
            // =========================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceNavySurface),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CARE INSTRUCTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = description,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Phase 1: 10-Second Soothing Chime Countdown Progress Bar
                    if (isChimeActive) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🌸 Soothing chime playing... Voice instructions in ${chimeSecondsLeft}s",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlertAmber
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { chimeSecondsLeft / initialChimeSeconds.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = OrbitalCyan,
                                trackColor = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    isChimeActive = false
                                    isVoiceSpeaking = true
                                    onStopChimeAndSpeak()
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, OrbitalCyan)
                            ) {
                                Text(
                                    text = "⏭️ Skip Chime & Speak Now",
                                    fontSize = 12.sp,
                                    color = OrbitalCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Phase 2: AI Voice Active Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "🤖", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Companion Voice is speaking instructions",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SatelliteGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onReplayVoice,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SatelliteGreen)
                        ) {
                            Text(
                                text = "🔁 Replay AI Voice",
                                fontSize = 12.sp,
                                color = SatelliteGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // BOTTOM ACTION BUTTONS: High-Contrast Big Touch Targets
            // =========================================================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // PRIMARY BUTTON: "I Took It"
                Button(
                    onClick = onCompleted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ActionSuccessGreen)
                ) {
                    Text(
                        text = "✅  I TOOK IT / DONE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                // REPLAY BUTTON: Dedicated button to replay the AI Voice message anytime
                Button(
                    onClick = {
                        isChimeActive = false
                        isVoiceSpeaking = true
                        onReplayVoice()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    border = BorderStroke(1.dp, OrbitalCyan)
                ) {
                    Text(
                        text = "🔊  REPLAY VOICE MESSAGE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // SECONDARY BUTTON: "Snooze 5 Mins"
                Button(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ActionPrimaryBlue)
                ) {
                    Text(
                        text = if (isTest) "⏰  Snooze (10s Demo)" else "⏰  Snooze 5 Minutes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // DISMISS BUTTON
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Text(
                        text = "Dismiss Alarm",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
