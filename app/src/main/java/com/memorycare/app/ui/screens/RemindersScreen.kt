package com.memorycare.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.memorycare.app.model.ReminderVerificationStatus
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.model.Reminder
import com.memorycare.app.model.UserRole
import com.memorycare.app.ui.components.AccessibleBigButton
import com.memorycare.app.ui.components.SpaceTopBar
import com.memorycare.app.ui.theme.ActionPrimaryBlue
import com.memorycare.app.ui.theme.ActionSuccessGreen
import com.memorycare.app.ui.theme.CriticalRed
import com.memorycare.app.ui.theme.DeepSpaceNavy
import com.memorycare.app.ui.theme.ElderlyBgLight
import com.memorycare.app.ui.theme.ElderlyCardBorder
import com.memorycare.app.ui.theme.ElderlyTextPrimary
import com.memorycare.app.ui.theme.ElderlyTextSecondary
import com.memorycare.app.ui.theme.MatchedCardBorder
import com.memorycare.app.ui.theme.MatchedCardGreen
import com.memorycare.app.ui.theme.OrbitalCyan
import com.memorycare.app.util.rememberVoiceAssistant
import com.memorycare.app.viewmodel.MemoryCareViewModel
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.StrokeCap
import com.memorycare.app.ui.theme.SatelliteGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Filter tabs for Caretaker and Patient schedule views.
 */
enum class ReminderTabFilter(val label: String, val emoji: String) {
    ALL("All", "📋"),
    UPCOMING("Upcoming", "⏳"),
    COMPLETED("Completed Today", "✅")
}

/**
 * Screen E: Daily Reminders Screen with Full Android Text-To-Speech (TTS) Voice Assistant.
 * Speaks reminders aloud in clear, elder-friendly audio pace, features audio wave animations,
 * and allows 1-tap "Read All Schedule Aloud".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindersScreen(
    viewModel: MemoryCareViewModel,
    onNavigateBack: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()
    val satelliteStatus by viewModel.satelliteStatus.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    val context = LocalContext.current
    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }

    val voiceAssistant = rememberVoiceAssistant()
    val isSpeaking by voiceAssistant.isSpeaking
    val currentSpokenText by voiceAssistant.currentlySpeakingText

    var activeAudioPrompt by remember { mutableStateOf<Reminder?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Reminder?>(null) }
    var selectedTab by remember { mutableStateOf(ReminderTabFilter.ALL) }

    val completedCount = reminders.count { it.isCompleted }
    val upcomingCount = reminders.count { !it.isCompleted }
    val totalCount = reminders.size
    val filteredReminders = when (selectedTab) {
        ReminderTabFilter.ALL -> reminders
        ReminderTabFilter.UPCOMING -> reminders.filter { !it.isCompleted }
        ReminderTabFilter.COMPLETED -> reminders.filter { it.isCompleted }
    }

    // Sound wave pulse animation when assistant is speaking
    val infiniteTransition = rememberInfiniteTransition(label = "pulseVoice")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        topBar = {
            SpaceTopBar(
                title = if (userRole == UserRole.CARETAKER) "Patient Schedule & Reminders" else "Daily Reminders",
                subtitle = if (userRole == UserRole.CARETAKER) "Daily Routine Management & Adherence Sync" else "Voice Assistant & Medicine Schedule",
                userRole = userRole,
                satelliteStatus = satelliteStatus,
                onBackClick = {
                    voiceAssistant.stop()
                    onNavigateBack()
                },
                onSyncClick = { viewModel.triggerSatelliteSync() }
            )
        },
        containerColor = ElderlyBgLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Caretaker Daily Adherence & Progress Card
            if (userRole == UserRole.CARETAKER) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.5.dp, OrbitalCyan.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Routine Adherence",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                val todayDateStr = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date())
                                Text(
                                    text = "📅 $todayDateStr",
                                    fontSize = 12.sp,
                                    color = OrbitalCyan
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (completedCount == totalCount && totalCount > 0) Color(0xFF065F46) else Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = "$completedCount / $totalCount Done",
                                    color = if (completedCount == totalCount && totalCount > 0) SatelliteGreen else Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = SatelliteGreen,
                            trackColor = Color(0xFF1E293B),
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "Total", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    Text(text = "$totalCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF064E3B),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "Completed", fontSize = 11.sp, color = SatelliteGreen)
                                    Text(text = "$completedCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SatelliteGreen)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF78350F),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "Upcoming", fontSize = 11.sp, color = Color(0xFFFDE047))
                                    Text(text = "$upcomingCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
                                }
                            }
                        }
                    }
                }

                // Caretaker Schedule Operations Strip
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    border = BorderStroke(1.5.dp, Color(0xFF81C784))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "👩‍⚕️", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Caretaker Schedule Operations",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = "Tasks sync live with AI Voice Companion & Patient view",
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AccessibleBigButton(
                                text = "Add Daily Task",
                                emoji = "➕",
                                onClick = { showAddTaskDialog = true },
                                containerColor = ActionPrimaryBlue,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                minHeight = 44.dp,
                                fontSize = 13.sp
                            )
                            AccessibleBigButton(
                                text = "Reset Defaults",
                                emoji = "🔄",
                                onClick = { viewModel.resetDefaultReminders() },
                                containerColor = Color(0xFFECEFF1),
                                contentColor = Color(0xFF37474F),
                                modifier = Modifier.weight(0.8f),
                                minHeight = 44.dp,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        AccessibleBigButton(
                            text = "⚡ Test 5-Sec Alarm (Lock Phone Now)",
                            emoji = "🚨",
                            onClick = { viewModel.triggerTestReminderAlarm(secondsFromNow = 5) },
                            containerColor = Color(0xFF0F172A),
                            contentColor = OrbitalCyan,
                            modifier = Modifier.fillMaxWidth(),
                            minHeight = 44.dp,
                            fontSize = 13.sp
                        )

                        if (!hasOverlayPermission) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "⚠️ Enable 'Appear on top' permission",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Allows full-screen alarms to pop up over other apps or the home screen.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE2E8F0),
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AccessibleBigButton(
                                        text = "Grant Overlay Permission",
                                        emoji = "🔓",
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                val intent = Intent(
                                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    Uri.parse("package:${context.packageName}")
                                                )
                                                context.startActivity(intent)
                                            }
                                        },
                                        containerColor = Color(0xFFD97706),
                                        contentColor = Color.White,
                                        modifier = Modifier.fillMaxWidth(),
                                        minHeight = 36.dp,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Helper Banner with One-Tap "Read Today's Schedule Aloud" (Patient Role)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSpeaking) Color(0xFFE8F5E9) else Color(0xFFE8EAF6)
                    ),
                    border = BorderStroke(
                        2.dp,
                        if (isSpeaking) ActionSuccessGreen else Color(0xFFC5CAE9)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(if (isSpeaking) pulseScale else 1f)
                                    .clip(CircleShape)
                                    .background(if (isSpeaking) ActionSuccessGreen else ActionPrimaryBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isSpeaking) "🔊" else "🗣️",
                                    fontSize = 24.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isSpeaking) "Voice Assistant is Speaking..." else "Voice Assistant Ready",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSpeaking) ActionSuccessGreen else ActionPrimaryBlue
                                )
                                Text(
                                    text = "Tap any speaker button to hear it read aloud in clear speech.",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ElderlyTextSecondary,
                                    lineHeight = 19.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action buttons: "Read All Reminders" or "Stop Voice"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isSpeaking) {
                                AccessibleBigButton(
                                    text = "Stop Speaking",
                                    emoji = "⏹️",
                                    onClick = {
                                        voiceAssistant.stop()
                                        activeAudioPrompt = null
                                    },
                                    containerColor = CriticalRed,
                                    contentColor = Color.White,
                                    modifier = Modifier.fillMaxWidth(),
                                    minHeight = 48.dp
                                )
                            } else {
                                AccessibleBigButton(
                                    text = "Read Today's Schedule Aloud",
                                    emoji = "🔊",
                                    onClick = {
                                        val uncompleted = reminders.filter { !it.isCompleted }
                                        val speechText = if (uncompleted.isNotEmpty()) {
                                            "Hello! Here is your daily schedule. " + uncompleted.joinToString(". ") {
                                                "At ${it.timeFormatted}, ${it.audioPromptText}"
                                            }
                                        } else {
                                            "Wonderful job! All your daily reminders for today are already marked as completed."
                                        }
                                        voiceAssistant.speak(speechText)
                                    },
                                    containerColor = ActionPrimaryBlue,
                                    contentColor = Color.White,
                                    modifier = Modifier.fillMaxWidth(),
                                    minHeight = 48.dp
                                )
                            }
                        }
                    }
                }
            }

            // Filter Tabs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    Triple(ReminderTabFilter.ALL, "All ($totalCount)", "📋"),
                    Triple(ReminderTabFilter.UPCOMING, "Upcoming ($upcomingCount)", "⏳"),
                    Triple(ReminderTabFilter.COMPLETED, "Completed ($completedCount)", "✅")
                )

                tabs.forEach { (tab, label, emoji) ->
                    val isSelected = selectedTab == tab
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ActionPrimaryBlue else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) ActionPrimaryBlue else ElderlyCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = tab }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = emoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else ElderlyTextPrimary
                            )
                        }
                    }
                }
            }

            // Reminders List or Empty State Card
            if (filteredReminders.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, ElderlyCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (selectedTab) {
                                ReminderTabFilter.COMPLETED -> "No tasks completed yet today"
                                ReminderTabFilter.UPCOMING -> "All scheduled tasks completed! 🎉"
                                else -> "No reminders scheduled"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElderlyTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when (selectedTab) {
                                ReminderTabFilter.COMPLETED -> "Active reminders marked as done today will appear here with completion timestamps."
                                ReminderTabFilter.UPCOMING -> "Every routine for today has been checked off successfully."
                                else -> "Tap '+ Add Daily Task' above to schedule medicine or routines."
                            },
                            fontSize = 13.sp,
                            color = ElderlyTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                filteredReminders.forEach { reminder ->
                    val isThisSpeaking = isSpeaking && activeAudioPrompt?.id == reminder.id
                    ReminderItemCard(
                        reminder = reminder,
                        isSpeaking = isThisSpeaking,
                        pulseScale = if (isThisSpeaking) pulseScale else 1f,
                        userRole = userRole,
                        onToggleDone = { viewModel.toggleReminder(reminder.id) },
                        onPlayAudioPrompt = {
                            activeAudioPrompt = reminder
                            voiceAssistant.speak(reminder.audioPromptText)
                        },
                        onDelete = { taskToDelete = reminder }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Navigation Button
            AccessibleBigButton(
                text = if (userRole == UserRole.CARETAKER) "Back to Caretaker Dashboard" else "Back to Home",
                emoji = if (userRole == UserRole.CARETAKER) "⬅️" else "🏠",
                onClick = {
                    voiceAssistant.stop()
                    onNavigateBack()
                },
                containerColor = if (userRole == UserRole.CARETAKER) DeepSpaceNavy else Color(0xFF455A64),
                minHeight = 56.dp
            )
        }
    }

    // Voice / Audio Prompt TTS Interactive Dialog
    activeAudioPrompt?.let { rem ->
        AlertDialog(
            onDismissRequest = {
                voiceAssistant.stop()
                activeAudioPrompt = null
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🔊", fontSize = 26.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Voice Assistant",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ActionPrimaryBlue
                        )
                        Text(
                            text = if (isSpeaking) "Speaking aloud now..." else "Spoken clearly",
                            fontSize = 13.sp,
                            color = ActionSuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            text = {
                Column {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFE3F2FD),
                        border = BorderStroke(1.5.dp, ActionPrimaryBlue.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"${rem.audioPromptText}\"",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0D47A1),
                            modifier = Modifier.padding(18.dp),
                            lineHeight = 29.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Scheduled for: ${rem.timeFormatted} (${rem.category})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ElderlyTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Voice Controls (Replay / Stop)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccessibleBigButton(
                            text = "Replay Voice",
                            emoji = "🔄",
                            onClick = {
                                voiceAssistant.speak(rem.audioPromptText)
                            },
                            containerColor = ActionPrimaryBlue,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f),
                            minHeight = 44.dp
                        )

                        if (isSpeaking) {
                            AccessibleBigButton(
                                text = "Stop",
                                emoji = "⏹️",
                                onClick = {
                                    voiceAssistant.stop()
                                },
                                containerColor = CriticalRed,
                                contentColor = Color.White,
                                modifier = Modifier.weight(0.8f),
                                minHeight = 44.dp
                            )
                        }
                    }
                }
            },
                    confirmButton = {
                        AccessibleBigButton(
                            text = "I UNDERSTOOD",
                            emoji = "✓",
                            onClick = {
                                voiceAssistant.stop()
                                activeAudioPrompt = null
                            },
                            containerColor = ActionSuccessGreen,
                            contentColor = Color.White,
                            minHeight = 48.dp
                        )
                    }
                )
            }

            // Caretaker Add Task Dialog
            if (showAddTaskDialog) {
                AddScheduleTaskDialog(
                    onDismiss = { showAddTaskDialog = false },
                    onAdd = { title, desc, time, cat, emo, prompt ->
                        viewModel.addReminder(
                            title = title,
                            description = desc,
                            timeFormatted = time,
                            category = cat,
                            iconEmoji = emo,
                            audioPromptText = prompt
                        )
                        showAddTaskDialog = false
                    }
                )
            }

            // Deletion Confirmation Dialog
            taskToDelete?.let { task ->
                AlertDialog(
                    onDismissRequest = { taskToDelete = null },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    title = {
                        Text(
                            text = "Delete Schedule Task?",
                            fontWeight = FontWeight.Bold,
                            color = CriticalRed,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to remove \"${task.title}\" from the schedule? The AI Voice Companion will immediately update its knowledge.",
                            fontSize = 14.sp,
                            color = ElderlyTextPrimary
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteReminder(task.id)
                                taskToDelete = null
                            }
                        ) {
                            Text(text = "Delete", color = CriticalRed, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { taskToDelete = null }) {
                            Text(text = "Cancel", color = ElderlyTextSecondary)
                        }
                    }
                )
            }
}

@Composable
private fun ReminderItemCard(
    reminder: Reminder,
    isSpeaking: Boolean,
    pulseScale: Float,
    userRole: UserRole = UserRole.PATIENT,
    onToggleDone: () -> Unit,
    onPlayAudioPrompt: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val isPatient = userRole == UserRole.PATIENT
    val titleSize = if (isPatient) 24.sp else 18.sp
    val descSize = if (isPatient) 18.sp else 14.sp
    val checkboxSize = if (isPatient) 64.dp else 48.dp
    val speakerSize = if (isPatient) 52.dp else 42.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleDone() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isCompleted) MatchedCardGreen else Color.White
        ),
        border = BorderStroke(
            if (isSpeaking) 3.dp else 2.5.dp,
            if (isSpeaking) ActionSuccessGreen else if (reminder.isCompleted) MatchedCardBorder else ElderlyCardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSpeaking) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category & Time Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (reminder.isCompleted) ActionSuccessGreen else ActionPrimaryBlue
                    ) {
                        Text(
                            text = reminder.timeFormatted,
                            color = Color.White,
                            fontSize = if (isPatient) 16.sp else 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFECEFF1)
                    ) {
                        Text(
                            text = reminder.category.uppercase(),
                            color = ElderlyTextSecondary,
                            fontSize = if (isPatient) 13.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Verification Adherence Badge
                    val status = reminder.safeVerificationStatus
                    if (reminder.isCompleted || status != ReminderVerificationStatus.SCHEDULED) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val (badgeText, badgeBg, badgeTextColor) = when (status) {
                            ReminderVerificationStatus.VERIFIED_COMPLETED -> Triple("🟢 Verified", Color(0xFFD1FAE5), Color(0xFF065F46))
                            ReminderVerificationStatus.PENDING_VERIFICATION -> Triple("🟡 30m Check", Color(0xFFFEF3C7), Color(0xFF92400E))
                            ReminderVerificationStatus.UNVERIFIED_ESCALATED -> Triple("🔴 Missed", Color(0xFFFFE4E6), Color(0xFF9F1239))
                            else -> Triple("✓ Done", Color(0xFFD1FAE5), Color(0xFF065F46))
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeTextColor,
                                fontSize = if (isPatient) 12.sp else 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Action Controls: Delete (Caretaker only) + Audio Read Aloud Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (userRole == UserRole.CARETAKER) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEBEE))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Task",
                                tint = CriticalRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Audio Read Aloud Button (Large Accessible Target)
                    Surface(
                        shape = CircleShape,
                        color = if (isSpeaking) ActionSuccessGreen else Color(0xFFE3F2FD),
                        border = BorderStroke(1.5.dp, if (isSpeaking) ActionSuccessGreen else ActionPrimaryBlue),
                        modifier = Modifier
                            .size(speakerSize)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .clickable { onPlayAudioPrompt() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read Reminder Aloud with Voice Assistant",
                                tint = if (isSpeaking) Color.White else ActionPrimaryBlue,
                                modifier = Modifier.size(if (isPatient) 28.dp else 22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Row with Checkbox and Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Accessible Checkbox (Scaled by role)
                Box(
                    modifier = Modifier
                        .size(checkboxSize)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (reminder.isCompleted) ActionSuccessGreen else Color(0xFFF0F4FA))
                        .border(
                            2.5.dp,
                            if (reminder.isCompleted) ActionSuccessGreen else ActionPrimaryBlue,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onToggleDone() },
                    contentAlignment = Alignment.Center
                ) {
                    if (reminder.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(if (isPatient) 40.dp else 28.dp)
                        )
                    } else {
                        Text(
                            text = reminder.iconEmoji,
                            fontSize = if (isPatient) 32.sp else 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        fontSize = titleSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (reminder.isCompleted) Color(0xFF2E7D32) else ElderlyTextPrimary,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        lineHeight = (titleSize.value + 6).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = reminder.description,
                        fontSize = descSize,
                        fontWeight = FontWeight.Medium,
                        color = ElderlyTextSecondary,
                        lineHeight = (descSize.value + 6).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Completion Status Tag with live completion time
            val completedTimeText = if (reminder.isCompleted) {
                if (reminder.safeCompletedAt > 0L) {
                    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(reminder.safeCompletedAt))
                    "✓ Completed today at $timeStr • Synced with Caretaker"
                } else {
                    "✓ Completed today for ${reminder.timeFormatted} • Synced with Caretaker"
                }
            } else {
                "⏳ Tap checkbox to mark as completed"
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (reminder.isCompleted) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
            ) {
                Text(
                    text = completedTimeText,
                    color = if (reminder.isCompleted) ActionSuccessGreen else Color(0xFFE65100),
                    fontSize = if (isPatient) 15.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp)
                )
            }
        }
    }
}

/**
 * Dialog allowing Caretakers to add new daily schedule operations.
 * Automatically propagates to AI Voice Companion and Patient screens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddScheduleTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, description: String, time: String, category: String, emoji: String, audioPrompt: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var timeFormatted by remember { mutableStateOf("09:00 AM") }
    var category by remember { mutableStateOf("Medicine") }
    var description by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("💊") }
    var audioPrompt by remember { mutableStateOf("") }

    val categories = listOf(
        Triple("Medicine", "💊", "Time to take your scheduled medication with fresh water."),
        Triple("Meal", "🍲", "It is meal time. Enjoy your fresh warm food and drink water."),
        Triple("Wellness", "🚶", "Time for your gentle walk and relaxing fresh air."),
        Triple("Hydration", "💧", "Please take a relaxing pause to drink a refreshing glass of water."),
        Triple("Checkup", "🩺", "Time for your health and vitals check.")
    )

    val timePresets = listOf("08:00 AM", "12:30 PM", "04:30 PM", "08:00 PM")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "➕", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Daily Schedule Task",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = ActionPrimaryBlue
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Schedule operations sync live with the AI Companion & Patient screens.",
                    fontSize = 13.sp,
                    color = ElderlyTextSecondary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Afternoon Blood Sugar Check") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = timeFormatted,
                    onValueChange = { timeFormatted = it },
                    label = { Text("Scheduled Time *") },
                    placeholder = { Text("e.g. 02:30 PM") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick time preset chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    timePresets.forEach { preset ->
                        val isPresetSelected = timeFormatted == preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPresetSelected) ActionPrimaryBlue else Color(0xFFECEFF1),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { timeFormatted = preset }
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPresetSelected) Color.White else Color(0xFF37474F),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Select Category:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElderlyTextPrimary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { (cat, emo, prompt) ->
                        val isSelected = category == cat
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) ActionPrimaryBlue else Color(0xFFF0F4F8),
                            border = BorderStroke(1.dp, if (isSelected) ActionPrimaryBlue else Color(0xFFCFD8DC)),
                            modifier = Modifier.clickable {
                                category = cat
                                emoji = emo
                                if (audioPrompt.isBlank()) {
                                    audioPrompt = prompt
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = emo, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF37474F)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Instructions") },
                    placeholder = { Text("e.g. Check glucose reading before tea.") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = audioPrompt,
                    onValueChange = { audioPrompt = it },
                    label = { Text("Voice Assistant Prompt (Optional)") },
                    placeholder = { Text("What AI speaks when reminding patient") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AccessibleBigButton(
                text = "Save Task",
                emoji = "✓",
                onClick = {
                    if (title.isNotBlank() && timeFormatted.isNotBlank()) {
                        val finalPrompt = if (audioPrompt.isNotBlank()) audioPrompt else "It is time for your $title."
                        onAdd(title, description, timeFormatted, category, emoji, finalPrompt)
                    }
                },
                containerColor = ActionSuccessGreen,
                contentColor = Color.White,
                minHeight = 44.dp,
                fontSize = 14.sp
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = ElderlyTextSecondary)
            }
        }
    )
}
