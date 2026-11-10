package com.memorycare.app.ui

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.memorycare.app.alarm.ReminderAlarmService
import androidx.compose.animation.AnimatedVisibility
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
import com.memorycare.app.alarm.SoothingAlarmPlayer
import com.memorycare.app.data.LocalStorageRepository
import com.memorycare.app.model.AlertSeverity
import com.memorycare.app.model.AlertType
import com.memorycare.app.model.CaretakerAlert
import com.memorycare.app.model.ReminderVerificationStatus
import com.memorycare.app.ui.theme.ActionPrimaryBlue
import com.memorycare.app.ui.theme.ActionSuccessGreen
import com.memorycare.app.ui.theme.DeepSpaceNavy
import com.memorycare.app.ui.theme.OrbitalCyan
import com.memorycare.app.ui.theme.SatelliteGreen
import com.memorycare.app.ui.theme.SpaceNavyCard
import com.memorycare.app.ui.theme.SpaceNavySurface
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Full-screen gentle 30-minute verification check-in activity.
 * Wakes the device and calmly checks if the dementia patient actually took their medication.
 * Automatically escalates to Caretaker if unconfirmed or forgotten.
 */
class ReminderVerificationActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_IS_TEST = "extra_is_test"
    }

    private var soothingPlayer: SoothingAlarmPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private lateinit var reminderId: String
    private lateinit var title: String
    private var isTest: Boolean = false

    private var shouldSpeakWhenTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureLockscreenFlags()

        reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: "rem_unknown"
        title = intent.getStringExtra(EXTRA_TITLE) ?: "Scheduled Routine"
        isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)

        textToSpeech = TextToSpeech(this, this)
        soothingPlayer = SoothingAlarmPlayer(this)
        soothingPlayer?.playSoothingChime(durationSeconds = 3)

        setContent {
            ReminderVerificationContent(
                reminderTitle = title,
                onConfirmedYes = {
                    handleConfirmedTaken()
                },
                onTappedForgot = {
                    handleForgottenMedication()
                },
                onReplayVoice = {
                    speakCheckInQuestion()
                },
                onChimeFinished = {
                    onChimeCompleted()
                },
                onDismiss = {
                    handleDismissedWithoutConfirmation()
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

    private fun stopAlarmServiceAndFinish() {
        soothingPlayer?.stop()
        try {
            val dismissIntent = Intent(this, ReminderAlarmService::class.java).apply {
                action = ReminderAlarmService.ACTION_DISMISS_ALARM
            }
            startService(dismissIntent)
        } catch (_: Exception) {
        }
        finish()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            textToSpeech?.language = Locale.getDefault()
            textToSpeech?.setSpeechRate(0.72f) // Patient-friendly, calm pace
            textToSpeech?.setPitch(0.96f)
            if (shouldSpeakWhenTtsReady) {
                speakCheckInQuestion()
            }
        }
    }

    private fun onChimeCompleted() {
        if (isTtsReady) {
            speakCheckInQuestion()
        } else {
            shouldSpeakWhenTtsReady = true
        }
    }

    private fun speakCheckInQuestion() {
        if (!isTtsReady || textToSpeech == null) return
        soothingPlayer?.stop()

        val text = "Hi there... Just a gentle check-in. Did you drink your water and take your $title? Please tap Yes, or tap I Forgot."
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioAttributes.USAGE_ALARM)
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "verify_tts_utterance")
    }

    private fun handleConfirmedTaken() {
        soothingPlayer?.stop()
        try {
            val repository = LocalStorageRepository(this)
            val current = repository.getReminders()
            val updated = current.map { r ->
                if (r.id == reminderId) {
                    r.copy(
                        isCompleted = true,
                        verificationStatus = ReminderVerificationStatus.VERIFIED_COMPLETED,
                        verifiedAtTimestamp = System.currentTimeMillis()
                    )
                } else r
            }
            repository.saveReminders(updated)
        } catch (_: Exception) {
        }

        // Positive reinforcement speech
        if (isTtsReady) {
            val praise = "Wonderful! Thank you so much for letting me know. Have a peaceful day."
            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioAttributes.USAGE_ALARM)
            }
            textToSpeech?.speak(praise, TextToSpeech.QUEUE_FLUSH, params, "praise_utterance")
        }

        // Brief delay for audio then finish
        window.decorView.postDelayed({ stopAlarmServiceAndFinish() }, 2500L)
    }

    private fun handleForgottenMedication() {
        soothingPlayer?.stop()
        try {
            val repository = LocalStorageRepository(this)
            val current = repository.getReminders()
            val updated = current.map { r ->
                if (r.id == reminderId) {
                    r.copy(
                        isCompleted = false,
                        verificationStatus = ReminderVerificationStatus.UNVERIFIED_ESCALATED
                    )
                } else r
            }
            repository.saveReminders(updated)

            // Post Escalation Alert to Caretaker Dashboard
            val currentAlerts = repository.getCaretakerAlerts().toMutableList()
            val newAlert = CaretakerAlert(
                id = "alert_missed_${System.currentTimeMillis()}",
                title = "⚠️ Medication Check-in: $title",
                message = "Grandpa John marked 'I Forgot' during his 30-minute check-in. Please assist him with his dose.",
                severity = AlertSeverity.WARNING,
                timestamp = System.currentTimeMillis(),
                alertType = AlertType.MEDICATION_ADHERENCE,
                isDismissed = false
            )
            currentAlerts.add(0, newAlert)
            repository.saveCaretakerAlerts(currentAlerts)
        } catch (_: Exception) {
        }

        // Gentle voice reassurance
        if (isTtsReady) {
            val reassurance = "That is completely okay! Please take your dose with water now, or Sarah can assist you."
            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioAttributes.USAGE_ALARM)
            }
            textToSpeech?.speak(reassurance, TextToSpeech.QUEUE_FLUSH, params, "forgot_utterance")
        }

        window.decorView.postDelayed({ stopAlarmServiceAndFinish() }, 3000L)
    }

    private fun handleDismissedWithoutConfirmation() {
        soothingPlayer?.stop()
        try {
            val repository = LocalStorageRepository(this)
            val current = repository.getReminders()
            val reminder = current.firstOrNull { it.id == reminderId }
            // If still pending verification, escalate to caretaker
            if (reminder?.verificationStatus == ReminderVerificationStatus.PENDING_VERIFICATION) {
                val updated = current.map { r ->
                    if (r.id == reminderId) {
                        r.copy(verificationStatus = ReminderVerificationStatus.UNVERIFIED_ESCALATED)
                    } else r
                }
                repository.saveReminders(updated)

                val currentAlerts = repository.getCaretakerAlerts().toMutableList()
                val newAlert = CaretakerAlert(
                    id = "alert_unconfirmed_${System.currentTimeMillis()}",
                    title = "⚠️ Unconfirmed Medication: $title",
                    message = "Grandpa John did not confirm his 30-minute check-in for $title. Please check in with him.",
                    severity = AlertSeverity.WARNING,
                    timestamp = System.currentTimeMillis(),
                    alertType = AlertType.MEDICATION_ADHERENCE,
                    isDismissed = false
                )
                currentAlerts.add(0, newAlert)
                repository.saveCaretakerAlerts(currentAlerts)
            }
        } catch (_: Exception) {
        }
        stopAlarmServiceAndFinish()
    }

    override fun onDestroy() {
        super.onDestroy()
        soothingPlayer?.stop()
        try {
            val dismissIntent = Intent(this, ReminderAlarmService::class.java).apply {
                action = ReminderAlarmService.ACTION_DISMISS_ALARM
            }
            startService(dismissIntent)
        } catch (_: Exception) {
        }
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {
        }
    }
}

@Composable
fun ReminderVerificationContent(
    reminderTitle: String,
    onConfirmedYes: () -> Unit,
    onTappedForgot: () -> Unit,
    onReplayVoice: () -> Unit,
    onChimeFinished: () -> Unit,
    onDismiss: () -> Unit
) {
    var hasAnswered by remember { mutableStateOf(false) }
    var selectedAnswer by remember { mutableStateOf("") }
    var chimeSecondsLeft by remember { mutableIntStateOf(3) }
    var isChimeActive by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (chimeSecondsLeft > 0) {
            delay(1000L)
            chimeSecondsLeft--
        }
        isChimeActive = false
        onChimeFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "checkinPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
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
            // TOP HEADER: Gentle Care Check-in
            // =========================================================================
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = OrbitalCyan.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, OrbitalCyan)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌸 GENTLE CARE CHECK-IN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrbitalCyan,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pulsing Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(SatelliteGreen.copy(alpha = 0.18f))
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(SpaceNavyCard)
                            .border(2.dp, SatelliteGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "💧", fontSize = 40.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Did you take your\n$reminderTitle?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A quick, friendly follow-up to make sure you drank your water and took your dose comfortably.",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // RESPONSE CARDS / STATUS
            // =========================================================================
            if (!hasAnswered) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SpaceNavySurface),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isChimeActive) {
                            Text(
                                text = "🌸 Soothing chime playing... AI voice in ${chimeSecondsLeft}s",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { chimeSecondsLeft / 3f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = OrbitalCyan,
                                trackColor = Color(0xFF1E293B)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "🤖", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Companion Voice is speaking...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SatelliteGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                isChimeActive = false
                                onReplayVoice()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, OrbitalCyan)
                        ) {
                            Text(
                                text = "🔊 Replay Question",
                                fontSize = 12.sp,
                                color = OrbitalCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SpaceNavySurface),
                    border = BorderStroke(1.dp, SatelliteGreen)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedAnswer == "YES") "🎉 Thank you! Verification Recorded." else "ℹ️ Caregiver Notified for Support.",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedAnswer == "YES") SatelliteGreen else Color(0xFFFB7185)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedAnswer == "YES") "Your adherence is logged to the Caretaker Dashboard." else "Nurse Sarah has been gently notified to help you.",
                            fontSize = 13.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // ACTION BUTTONS: 2 Simple, High-Contrast Choices
            // =========================================================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // BUTTON 1: "YES, ALL DONE!"
                Button(
                    onClick = {
                        if (!hasAnswered) {
                            hasAnswered = true
                            selectedAnswer = "YES"
                            onConfirmedYes()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ActionSuccessGreen),
                    enabled = !hasAnswered
                ) {
                    Text(
                        text = "✅  YES, ALL DONE! 💧",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                // BUTTON 2: "OOPS, I FORGOT / NEED HELP"
                Button(
                    onClick = {
                        if (!hasAnswered) {
                            hasAnswered = true
                            selectedAnswer = "FORGOT"
                            onTappedForgot()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE123C)),
                    enabled = !hasAnswered
                ) {
                    Text(
                        text = "💊  OOPS, I FORGOT! / NEED HELP",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // DISMISS BUTTON
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    enabled = !hasAnswered
                ) {
                    Text(
                        text = "Dismiss Check-in",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
