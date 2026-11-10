package com.memorycare.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.model.AlertSeverity
import com.memorycare.app.model.AlertType
import com.memorycare.app.model.CaretakerAlert
import com.memorycare.app.model.CaretakerDetails
import com.memorycare.app.model.DifficultyLevel
import com.memorycare.app.model.GameType
import com.memorycare.app.model.PatientInquiry
import com.memorycare.app.model.PatientLocation
import com.memorycare.app.model.UserProfile
import com.memorycare.app.model.UserRole
import com.memorycare.app.model.ReminderVerificationStatus
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.memorycare.app.ui.components.AccessibleBigButton
import com.memorycare.app.ui.components.SpaceTopBar
import com.memorycare.app.ui.theme.ActionPrimaryBlue
import com.memorycare.app.ui.theme.ActionSuccessGreen
import com.memorycare.app.ui.theme.ActionWarningOrange
import com.memorycare.app.ui.theme.CriticalRed
import com.memorycare.app.ui.theme.DeepSpaceNavy
import com.memorycare.app.ui.theme.OrbitalCyan
import com.memorycare.app.ui.theme.SatelliteGreen
import com.memorycare.app.ui.theme.SpaceNavyCard
import com.memorycare.app.ui.theme.SpaceNavySurface
import com.memorycare.app.viewmodel.MemoryCareViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modern Dark Theme Palette for Caretaker Experience
private val DarkDashboardBg = Color(0xFF0F172A)
private val DarkCardSurface = Color(0xFF1E293B)
private val DarkCardBorder = Color(0xFF334155)
private val TextWhitePrimary = Color(0xFFF8FAFC)
private val TextMutedSecondary = Color(0xFF94A3B8)

private val CriticalDarkBg = Color(0xFF2D1215)
private val CriticalBorder = Color(0xFFE53935)
private val WarningDarkBg = Color(0xFF2E1C00)
private val WarningBorder = Color(0xFFFF8F00)
private val InfoDarkBg = Color(0xFF0D253A)
private val InfoBorder = Color(0xFF1E88E5)
private val SuccessDarkBg = Color(0xFF0C2915)
private val SuccessBorder = Color(0xFF43A047)

/**
 * Modern, Professional Caretaker Dashboard Screen:
 * - Status Bar with satellite downlink telemetry & sync time
 * - 3 Key Metric Cards in a Row (Cognitive Score, Games Today, Geofence Safety)
 * - Prioritized Alert Grouping (CRITICAL -> WARNING -> INFO)
 * - Live Geospatial & Adherence Telemetry Cards
 * - Streamlined Quick Action Controls
 */
@Composable
fun CaretakerDashboardScreen(
    viewModel: MemoryCareViewModel,
    onNavigateToProgressAnalytics: () -> Unit,
    onNavigateToAlertCenter: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToManageFaceMatch: () -> Unit,
    onNavigateToProfileSelection: () -> Unit,
    onSignOut: () -> Unit
) {
    val satelliteStatus by viewModel.satelliteStatus.collectAsState()
    val cognitiveScore by viewModel.cognitiveEffectivenessScore.collectAsState()
    val gameResults by viewModel.gameResults.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val isGeofenceEnabled by viewModel.isGeofenceEnabled.collectAsState()
    val patientLocation by viewModel.patientLocation.collectAsState()
    val alertsList by viewModel.alertsList.collectAsState()
    val unreadAlertsCount by viewModel.unreadAlertsCount.collectAsState()
    val caretakerDetails by viewModel.caretakerDetails.collectAsState()
    val patientInquiries by viewModel.patientInquiries.collectAsState()
    val faceMatchQuestions by viewModel.faceMatchQuestions.collectAsState()
    val difficultyMap by viewModel.difficultyPrefs.collectAsState()
    val faceAdaptiveDifficulty = difficultyMap[GameType.FACE_NAME_ASSOCIATION] ?: DifficultyLevel.EASY

    val currentProfile by viewModel.currentProfile.collectAsState()
    val activePatient by viewModel.activePatientProfile.collectAsState()
    val allProfiles by viewModel.profiles.collectAsState()
    val patientProfiles = allProfiles.filter { it.defaultRole == UserRole.PATIENT }
    var showPatientSwitchDialog by remember { mutableStateOf(false) }

    var showEditDetailsDialog by remember { mutableStateOf(false) }
    var editCaretakerName by remember { mutableStateOf("") }
    var editRelationship by remember { mutableStateOf("") }
    var editPhoneNumber by remember { mutableStateOf("") }
    var editFacilityName by remember { mutableStateOf("") }
    var editPatientName by remember { mutableStateOf("") }
    var editPatientAge by remember { mutableStateOf("") }
    var editEmergencyNotes by remember { mutableStateOf("") }
    var editFavoriteMemories by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Filter and group alerts by priority
    val activeAlerts = alertsList.filter { !it.isDismissed }
    val criticalAlerts = activeAlerts.filter {
        it.severity == AlertSeverity.CRITICAL ||
        it.alertType == AlertType.SIGNIFICANT_DROP ||
        it.alertType == AlertType.CONSISTENCY_WARNING ||
        it.alertType == AlertType.SOS
    }
    val warningAlerts = activeAlerts.filter {
        it.severity == AlertSeverity.WARNING &&
        it.alertType != AlertType.SIGNIFICANT_DROP &&
        it.alertType != AlertType.CONSISTENCY_WARNING &&
        it.alertType != AlertType.SOS
    }
    val infoAlerts = activeAlerts.filter {
        it.severity == AlertSeverity.INFO ||
        it.alertType == AlertType.IMPROVEMENT_CELEBRATION ||
        it.alertType == AlertType.GENERAL
    }

    // Counts for Games Played Breakdown
    val memoryGameCount = gameResults.count { it.gameType == GameType.MEMORY_MATCH }
    val sequenceGameCount = gameResults.count { it.gameType == GameType.NUMBER_SEQUENCE }
    val sortingGameCount = gameResults.count { it.gameType == GameType.DAILY_ROUTINE || it.gameType == GameType.CATEGORY_SORTING }
    val faceGameCount = gameResults.count { it.gameType == GameType.FACE_NAME_ASSOCIATION }

    val context = LocalContext.current
    val completedReminders = reminders.count { it.isCompleted }
    val totalReminders = reminders.size
    val verifiedReminders = reminders.count { it.safeVerificationStatus == ReminderVerificationStatus.VERIFIED_COMPLETED }
    val pendingVerificationReminders = reminders.count { it.safeVerificationStatus == ReminderVerificationStatus.PENDING_VERIFICATION }
    val unverifiedReminders = reminders.filter { it.safeVerificationStatus == ReminderVerificationStatus.UNVERIFIED_ESCALATED }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SpaceTopBar(
                title = "Caretaker Central",
                subtitle = "Cognitive & Safety Telemetry",
                userRole = UserRole.CARETAKER,
                satelliteStatus = satelliteStatus,
                unreadAlertsCount = unreadAlertsCount,
                onAlertBellClick = onNavigateToAlertCenter,
                onSignOutClick = {
                    viewModel.signOutProfile()
                    onSignOut()
                },
                onBackClick = onNavigateToProfileSelection,
                onSyncClick = { viewModel.triggerSatelliteSync() }
            )
        },
        containerColor = DarkDashboardBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =========================================================================
            // 1. TOP TELEMETRY STATUS BAR
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCardSurface,
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (satelliteStatus.isConnected) SatelliteGreen else CriticalRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🛰️ ${satelliteStatus.satelliteId} • ${if (satelliteStatus.isConnected) "LIVE" else "OFFLINE"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrbitalCyan
                        )
                    }

                    Text(
                        text = "Updated: ${satelliteStatus.lastSyncTimeFormatted}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMutedSecondary
                    )
                }
            }

            // =========================================================================
            // 1B. ACTIVE PATIENT CARE BANNER & SWITCHER
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF132338),
                border = BorderStroke(1.5.dp, OrbitalCyan.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(OrbitalCyan.copy(alpha = 0.15f))
                                .border(1.5.dp, OrbitalCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activePatient?.avatarEmoji ?: "👴",
                                fontSize = 24.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MONITORING PATIENT",
                                    color = OrbitalCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SatelliteGreen.copy(alpha = 0.2f),
                                    border = BorderStroke(0.5.dp, SatelliteGreen)
                                ) {
                                    Text(
                                        text = "SYNCED",
                                        color = SatelliteGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${activePatient?.name ?: caretakerDetails.patientName} (${activePatient?.age ?: caretakerDetails.patientAge} yrs)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhitePrimary
                            )
                            Text(
                                text = "Caretaker: ${currentProfile?.name ?: caretakerDetails.caretakerName} • Live Data Stream",
                                fontSize = 12.sp,
                                color = TextMutedSecondary
                            )
                        }
                    }

                    if (patientProfiles.size > 1) {
                        Button(
                            onClick = { showPatientSwitchDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Switch",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrbitalCyan
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 2. KEY METRICS ROW (3 CARDS IN A ROW)
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Effectiveness Score
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "COGNITIVE SCORE",
                    value = if (gameResults.isEmpty()) "0%" else "$cognitiveScore%",
                    valueColor = when {
                        gameResults.isEmpty() -> TextMutedSecondary
                        cognitiveScore >= 75 -> SatelliteGreen
                        cognitiveScore >= 50 -> ActionWarningOrange
                        else -> CriticalRed
                    },
                    subtext = when {
                        gameResults.isEmpty() -> "No Games Yet"
                        cognitiveScore >= 75 -> "Optimal State"
                        cognitiveScore >= 50 -> "Moderate"
                        else -> "Requires Care"
                    },
                    showScoreRing = true,
                    scorePercentage = if (gameResults.isEmpty()) 0 else cognitiveScore
                )

                // Card 2: Games Played Today
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "GAMES PLAYED",
                    value = "${gameResults.size}",
                    valueColor = OrbitalCyan,
                    subtext = "${memoryGameCount}M • ${sequenceGameCount}★ • ${sortingGameCount}☕ • ${faceGameCount}👥",
                    iconEmoji = "🧠"
                )

                // Card 3: Geofence Status
                val isBreach = patientLocation.isGeofenceBreached
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "GEOFENCE",
                    value = if (isBreach) "BREACH" else "SAFE",
                    valueColor = if (isBreach) CriticalRed else SatelliteGreen,
                    subtext = if (isBreach) "Outside Zone!" else "${patientLocation.distanceToBoundaryMeters}m to edge",
                    iconEmoji = if (isBreach) "🚨" else "🛡️",
                    isAlert = isBreach
                )
            }

            // =========================================================================
            // 3. PRIORITIZED ALERTS SECTION (CRITICAL -> WARNING -> INFO)
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alerts",
                        tint = if (criticalAlerts.isNotEmpty()) CriticalBorder else TextWhitePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Active Alerts & Insights",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhitePrimary
                    )
                    if (activeAlerts.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (criticalAlerts.isNotEmpty()) CriticalBorder else WarningBorder
                        ) {
                            Text(
                                text = "${activeAlerts.size}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "VIEW ALL ➔",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrbitalCyan,
                    modifier = Modifier.clickable { onNavigateToAlertCenter() }
                )
            }

            if (activeAlerts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkCardSurface,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✅", fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
                        Column {
                            Text(
                                text = "All Clear • No Active Alerts",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SatelliteGreen
                            )
                            Text(
                                text = "Patient telemetry is stable with no cognitive or safety anomalies.",
                                fontSize = 13.sp,
                                color = TextMutedSecondary
                            )
                        }
                    }
                }
            } else {
                // A. CRITICAL ALERTS (Red / Dark Red)
                criticalAlerts.forEach { alert ->
                    PriorityAlertCard(
                        alert = alert,
                        headerTitle = "CRITICAL ALERT",
                        headerEmoji = "🚨",
                        bgColor = CriticalDarkBg,
                        borderColor = CriticalBorder,
                        badgeColor = CriticalBorder,
                        onDismiss = { viewModel.dismissAlert(alert.id) },
                        onOpen = onNavigateToAlertCenter
                    )
                }

                // B. WARNING ALERTS (Orange / Dark Orange)
                warningAlerts.forEach { alert ->
                    PriorityAlertCard(
                        alert = alert,
                        headerTitle = "WARNING",
                        headerEmoji = "⚠️",
                        bgColor = WarningDarkBg,
                        borderColor = WarningBorder,
                        badgeColor = WarningBorder,
                        onDismiss = { viewModel.dismissAlert(alert.id) },
                        onOpen = onNavigateToAlertCenter
                    )
                }

                // C. INFO / CELEBRATION ALERTS (Blue / Dark Blue)
                infoAlerts.forEach { alert ->
                    PriorityAlertCard(
                        alert = alert,
                        headerTitle = if (alert.alertType == AlertType.IMPROVEMENT_CELEBRATION) "CELEBRATION" else "SYSTEM INFO",
                        headerEmoji = if (alert.alertType == AlertType.IMPROVEMENT_CELEBRATION) "🎉" else "ℹ️",
                        bgColor = if (alert.alertType == AlertType.IMPROVEMENT_CELEBRATION) SuccessDarkBg else InfoDarkBg,
                        borderColor = if (alert.alertType == AlertType.IMPROVEMENT_CELEBRATION) SuccessBorder else InfoBorder,
                        badgeColor = if (alert.alertType == AlertType.IMPROVEMENT_CELEBRATION) SuccessBorder else InfoBorder,
                        onDismiss = { viewModel.dismissAlert(alert.id) },
                        onOpen = onNavigateToAlertCenter
                    )
                }
            }

            // =========================================================================
            // 4. GEOFENCE & SAFETY TELEMETRY CARD
            // =========================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                border = BorderStroke(1.dp, if (patientLocation.isGeofenceBreached) CriticalBorder else DarkCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📍", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Geospatial Safety Perimeter",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                                Text(
                                    text = patientLocation.zoneDescription,
                                    fontSize = 12.sp,
                                    color = TextMutedSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isGeofenceEnabled,
                            onCheckedChange = { viewModel.setGeofenceEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ActionSuccessGreen,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Tracking Status Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (patientLocation.isSimulationRunning) Color(0xFF1E3A8A) else Color(0xFF064E3B).copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, if (patientLocation.isSimulationRunning) OrbitalCyan else SatelliteGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = if (patientLocation.isSimulationRunning) "🚶‍♂️" else "🛰️", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (patientLocation.isSimulationRunning) {
                                        "Live Walk Demo: Step ${patientLocation.simulationStep}/${patientLocation.totalSimulationSteps}"
                                    } else {
                                        "Autonomous GPS: Active (No button needed)"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                            }
                            Text(
                                text = "${patientLocation.satellitesInView} Sats Online",
                                fontSize = 10.sp,
                                color = OrbitalCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "COORDINATES",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMutedSecondary
                                )
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.4f", patientLocation.latitude)}°N, ${String.format(java.util.Locale.US, "%.4f", patientLocation.longitude)}°E",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OrbitalCyan
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "CENTER DISTANCE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMutedSecondary
                                )
                                Text(
                                    text = "${patientLocation.distanceFromCenterMeters.toInt()} Meters",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "BOUNDARY STATUS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMutedSecondary
                                )
                                Text(
                                    text = if (patientLocation.isGeofenceBreached) "+${patientLocation.distanceToBoundaryMeters}m Outside" else "${patientLocation.distanceToBoundaryMeters}m Inside",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (patientLocation.isGeofenceBreached) CriticalRed else SatelliteGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Real-Time Detected Street/Area Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (patientLocation.isGeofenceBreached) Color(0xFF450A0A) else Color(0xFF0F172A),
                        border = BorderStroke(1.dp, if (patientLocation.isGeofenceBreached) CriticalRed.copy(alpha = 0.6f) else Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (patientLocation.isGeofenceBreached) "🚨" else "📌", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (patientLocation.isGeofenceBreached) "REAL-TIME BREACH LOCATION (NEAR YOU)" else "CURRENT DETECTED AREA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (patientLocation.isGeofenceBreached) CriticalRed else OrbitalCyan
                                )
                                Text(
                                    text = patientLocation.locationName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhitePrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Interactive Visual Map Snippet
                    GeofenceRadarVisual(
                        patientLocation = patientLocation,
                        isBreach = patientLocation.isGeofenceBreached
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Hackathon Live Walk Simulation Button
                    Button(
                        onClick = {
                            if (patientLocation.isSimulationRunning) {
                                viewModel.stopLiveWalkSimulation()
                            } else {
                                viewModel.startLiveWalkSimulation()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (patientLocation.isSimulationRunning) ActionWarningOrange else ActionPrimaryBlue
                        )
                    ) {
                        Text(
                            text = if (patientLocation.isSimulationRunning) "⏹️ Stop Live Walk Demo" else "▶️ Start Live Walk Demo (SIH Hackathon)",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Controls: Home Base Calibration & Quick Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setHomeBaseToCurrentLocation() },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Text(text = "🎯 Set Home Base", fontSize = 11.sp, color = TextWhitePrimary, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { viewModel.simulateGeofenceBreach() },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))
                        ) {
                            Text(text = "⚠️ Instant Breach", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.resetPatientLocationToSafe() },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14532D))
                        ) {
                            Text(text = "🛡️ Reset Safe", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // =========================================================================
            // 5. MEDICINE & REMINDERS ADHERENCE STRIP
            // =========================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💊", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Daily Medicine & Schedule",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                                Text(
                                    text = "$completedReminders of $totalReminders Tasks Completed Today",
                                    fontSize = 13.sp,
                                    color = TextMutedSecondary
                                )
                            }
                        }

                        Button(
                            onClick = onNavigateToReminders,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ActionPrimaryBlue),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(text = "View", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val adherenceRatio = if (totalReminders > 0) completedReminders.toFloat() / totalReminders else 0f
                    LinearProgressIndicator(
                        progress = { adherenceRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = SatelliteGreen,
                        trackColor = Color(0xFF0F172A),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Adherence Verification Breakdown Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF064E3B),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "🟢 Verified: $verifiedReminders",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SatelliteGreen,
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF78350F),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Text(
                                text = "🟡 30m Check: $pendingVerificationReminders",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE047),
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (unverifiedReminders.isNotEmpty()) Color(0xFF7F1D1D) else Color(0xFF1E293B),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "🔴 Alert: ${unverifiedReminders.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (unverifiedReminders.isNotEmpty()) CriticalRed else Color(0xFF94A3B8),
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Escalated Unverified Adherence Cards
                    if (unverifiedReminders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        unverifiedReminders.forEach { reminder ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                                border = BorderStroke(1.dp, CriticalRed),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🚨", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Unverified: ${reminder.title}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Dismissed at ${reminder.timeFormatted} but not confirmed in 30-min check-in.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFECDD3)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${caretakerDetails.phoneNumber}"))
                                                context.startActivity(intent)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CriticalRed),
                                            modifier = Modifier.weight(1f).height(34.dp)
                                        ) {
                                            Text("📞 Call Patient", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { viewModel.caretakerManualVerifyReminder(reminder.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ActionSuccessGreen),
                                            modifier = Modifier.weight(1f).height(34.dp)
                                        ) {
                                            Text("✓ Mark Verified", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val hasOverlay = remember {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Settings.canDrawOverlays(context)
                        } else true
                    }

                    if (!hasOverlay) {
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
                                    text = "Required for alarms & check-ins to pop up over the Home Screen or other apps.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE2E8F0),
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                                ) {
                                    Text(
                                        text = "🔓 Grant 'Appear on top'",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerTestReminderAlarm(secondsFromNow = 5) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, OrbitalCyan)
                        ) {
                            Text(
                                text = "⚡ Test 5s Alarm",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrbitalCyan
                            )
                        }

                        Button(
                            onClick = { viewModel.triggerDemoVerificationCheckIn(secondsFromNow = 15) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B)),
                            border = BorderStroke(1.dp, SatelliteGreen)
                        ) {
                            Text(
                                text = "🌸 Demo 15s Check-in",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SatelliteGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 6. EDITABLE CARETAKER & PATIENT CARE PLAN DETAILS
            // =========================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "👩‍⚕️", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Caretaker & Patient Profile",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                                Text(
                                    text = "Synced live with AI Voice Companion",
                                    fontSize = 12.sp,
                                    color = OrbitalCyan
                                )
                            }
                        }

                        Button(
                            onClick = {
                                editCaretakerName = caretakerDetails.caretakerName
                                editRelationship = caretakerDetails.relationship
                                editPhoneNumber = caretakerDetails.phoneNumber
                                editFacilityName = caretakerDetails.facilityOrHomeName
                                editPatientName = caretakerDetails.patientName
                                editPatientAge = caretakerDetails.patientAge.toString()
                                editEmergencyNotes = caretakerDetails.emergencyContactNotes
                                editFavoriteMemories = caretakerDetails.favoriteMemories
                                showEditDetailsDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ActionPrimaryBlue),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(text = "✏️ Edit Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2-Column Info Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "CARETAKER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMutedSecondary
                                )
                                Text(
                                    text = caretakerDetails.caretakerName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                                Text(
                                    text = caretakerDetails.relationship,
                                    fontSize = 12.sp,
                                    color = TextMutedSecondary
                                )
                                Text(
                                    text = "📞 ${caretakerDetails.phoneNumber}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OrbitalCyan
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "PATIENT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMutedSecondary
                                )
                                Text(
                                    text = caretakerDetails.patientName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                                Text(
                                    text = "Age: ${caretakerDetails.patientAge} years",
                                    fontSize = 12.sp,
                                    color = TextMutedSecondary
                                )
                                Text(
                                    text = "🏡 ${caretakerDetails.facilityOrHomeName}",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = TextMutedSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "📝 CARE NOTE & MEMORIES FOR AI COMPANION:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = caretakerDetails.favoriteMemories,
                                fontSize = 12.sp,
                                color = TextWhitePrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 7. PATIENT VOICE INQUIRIES & QUESTIONS FEED
            // =========================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💬", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Patient Inquiries Feed (${patientInquiries.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhitePrimary
                            )
                        }
                        Text(
                            text = "Live Stream",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SatelliteGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Questions asked by patient to AI Companion • Repetitive queries flagged",
                        fontSize = 12.sp,
                        color = TextMutedSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (patientInquiries.isEmpty()) {
                        Text(
                            text = "No questions asked yet today. Waiting for patient voice interaction.",
                            fontSize = 13.sp,
                            color = TextMutedSecondary
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            patientInquiries.take(6).forEach { inquiry ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (inquiry.isRepetitive) Color(0xFF2E1C00) else Color(0xFF0F172A),
                                    border = BorderStroke(
                                        1.dp,
                                        if (inquiry.isRepetitive) WarningBorder else Color(0xFF334155)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "\"${inquiry.question}\"",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextWhitePrimary
                                                )
                                                if (inquiry.isRepetitive) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFE65100)
                                                    ) {
                                                        Text(
                                                            text = "🔄 REPETITIVE",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Category: ${inquiry.category} • ${inquiry.timestampFormatted}",
                                                fontSize = 11.sp,
                                                color = TextMutedSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 8. QUICK ACTION BUTTONS
            // =========================================================================
            Text(
                text = "Caretaker Operations",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhitePrimary
            )

            // Family Face Match Questions Manager Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkCardSurface,
                border = BorderStroke(1.dp, Color(0xFF7E57C2).copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToManageFaceMatch() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF7E57C2).copy(alpha = 0.18f))
                            .border(1.dp, Color(0xFF7E57C2).copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👥", fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Family Members & Face Match",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhitePrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (faceMatchQuestions.isEmpty()) {
                                "Tap to add photos & memory clues"
                            } else {
                                "${faceMatchQuestions.size} Family ${if (faceMatchQuestions.size == 1) "Member" else "Members"} • Photos & Clues"
                            },
                            fontSize = 12.sp,
                            color = TextMutedSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onNavigateToManageFaceMatch,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(text = "Manage", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Action 1: View Progress Analytics
                Button(
                    onClick = onNavigateToProgressAnalytics,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ActionPrimaryBlue)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📈", fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
                        Text(text = "Progress Graphs", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Action 2: Alert Center
                Button(
                    onClick = onNavigateToAlertCenter,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔔", fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
                        Text(text = "Alert Center (${unreadAlertsCount})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhitePrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Switch Profile and Sign Out Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToProfileSelection,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text(text = "👤 Switch Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhitePrimary)
                }

                Button(
                    onClick = {
                        viewModel.signOutProfile()
                        onSignOut()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                ) {
                    Text(text = "🚪 Sign Out", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhitePrimary)
                }
            }
        }
    }

    // Dialog for Editing Caretaker & Patient Care Plan Details
    if (showEditDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showEditDetailsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "✏️", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Caretaker & Patient Details",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Changes are saved to local database and immediately synced to the AI Voice Companion.",
                        fontSize = 13.sp,
                        color = Color(0xFF555555)
                    )

                    OutlinedTextField(
                        value = editCaretakerName,
                        onValueChange = { editCaretakerName = it },
                        label = { Text("Caretaker Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editRelationship,
                        onValueChange = { editRelationship = it },
                        label = { Text("Caretaker Role / Relationship") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPhoneNumber,
                        onValueChange = { editPhoneNumber = it },
                        label = { Text("Contact Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPatientName,
                        onValueChange = { editPatientName = it },
                        label = { Text("Patient Full / Preferred Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPatientAge,
                        onValueChange = { editPatientAge = it },
                        label = { Text("Patient Age") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editFacilityName,
                        onValueChange = { editFacilityName = it },
                        label = { Text("Facility / Residence Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editEmergencyNotes,
                        onValueChange = { editEmergencyNotes = it },
                        label = { Text("Emergency Contact Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editFavoriteMemories,
                        onValueChange = { editFavoriteMemories = it },
                        label = { Text("Favorite Memories (for AI Companion)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCaretakerDetails(
                            CaretakerDetails(
                                caretakerName = editCaretakerName.ifBlank { "Nurse Sarah" },
                                relationship = editRelationship.ifBlank { "Primary Care Nurse" },
                                phoneNumber = editPhoneNumber.ifBlank { "+91 98765 43210" },
                                facilityOrHomeName = editFacilityName.ifBlank { "Guwahati Residential Care Unit" },
                                patientName = editPatientName.ifBlank { "Grandpa John" },
                                patientAge = editPatientAge.toIntOrNull() ?: 78,
                                emergencyContactNotes = editEmergencyNotes.ifBlank { "Calls every morning at 8:00 AM." },
                                favoriteMemories = editFavoriteMemories.ifBlank { "Loves watercolor painting and morning tea." }
                            )
                        )
                        showEditDetailsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActionPrimaryBlue)
                ) {
                    Text(text = "Save & Sync to AI Companion", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDetailsDialog = false }) {
                    Text(text = "Cancel")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Switch Patient Dialog
    if (showPatientSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showPatientSwitchDialog = false },
            title = {
                Text(
                    text = "Select Patient to Monitor",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    patientProfiles.forEach { patient ->
                        val isCurrent = activePatient?.id == patient.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.switchActivePatient(patient)
                                    showPatientSwitchDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                if (isCurrent) 2.dp else 1.dp,
                                if (isCurrent) ActionPrimaryBlue else Color(0xFFCBD5E1)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0F7FA)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = patient.avatarEmoji, fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = patient.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Age ${patient.age} • Patient",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                if (isCurrent) {
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ActionPrimaryBlue
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPatientSwitchDialog = false }) {
                    Text(text = "CANCEL", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Metric Card for the Top 3-in-a-Row Dashboard Summary:
 * Shows large metric values (32sp), mini gauge or icon, label (12sp uppercase), and dynamic colors.
 */
@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color,
    subtext: String,
    iconEmoji: String? = null,
    showScoreRing: Boolean = false,
    scorePercentage: Int = 0,
    isAlert: Boolean = false
) {
    Card(
        modifier = modifier.height(124.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert) CriticalDarkBg else DarkCardSurface
        ),
        border = BorderStroke(
            1.dp,
            if (isAlert) CriticalBorder else DarkCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Label + Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMutedSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (showScoreRing) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { scorePercentage / 100f },
                            modifier = Modifier.size(18.dp),
                            color = valueColor,
                            trackColor = Color(0xFF0F172A),
                            strokeWidth = 2.5.dp
                        )
                    }
                } else if (iconEmoji != null) {
                    Text(text = iconEmoji, fontSize = 13.sp)
                }
            }

            // Big Metric Value (30-32sp)
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Bottom Subtitle
            Text(
                text = subtext,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isAlert) Color(0xFFFF8A80) else TextMutedSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Modern Priority Alert Card:
 * Clean, structured banner with priority badge, title (16sp), message (14sp), and action buttons.
 */
@Composable
private fun PriorityAlertCard(
    alert: CaretakerAlert,
    headerTitle: String,
    headerEmoji: String,
    bgColor: Color,
    borderColor: Color,
    badgeColor: Color,
    onDismiss: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row with Tag and Dismiss Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor
                    ) {
                        Text(
                            text = "$headerEmoji $headerTitle",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(alert.timestamp)),
                        fontSize = 12.sp,
                        color = TextMutedSecondary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextMutedSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Alert Title & Message
            Text(
                text = alert.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhitePrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = alert.message,
                fontSize = 14.sp,
                color = Color(0xFFE2E8F0),
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(text = "Investigate ➔", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * High-Impact Interactive Radar Map Snippet:
 * Renders concentric 50m, 100m, 150m rings, a glowing 200m safe perimeter circle,
 * center care residence, live pulsing patient beacon with vector line, and real-time breach telemetry.
 */
@Composable
fun GeofenceRadarVisual(
    patientLocation: PatientLocation,
    isBreach: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "geofenceRadar")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseRadiusScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadiusScale"
    )

    // Rotating 360-degree radar scanner sweep beam
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    // Smooth physics interpolation for distance ratio and bearing
    // Glides fluidly between real GPS coordinate fixes and simulation steps
    val safeRadius = patientLocation.safeRadiusMeters.coerceAtLeast(50f)
    val rawDistanceRatio = (patientLocation.distanceFromCenterMeters / safeRadius).coerceIn(0.06f, 1.35f)
    val animatedDistanceRatio by animateFloatAsState(
        targetValue = rawDistanceRatio,
        animationSpec = tween(durationMillis = 900, easing = LinearOutSlowInEasing),
        label = "animatedDistanceRatio"
    )
    val animatedBearing by animateFloatAsState(
        targetValue = patientLocation.bearingDegrees,
        animationSpec = tween(durationMillis = 900, easing = LinearOutSlowInEasing),
        label = "animatedBearing"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070D1E)),
        border = BorderStroke(1.dp, if (isBreach) CriticalRed.copy(alpha = 0.7f) else Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val maxRadius = (size.height / 2f) * 0.82f

                // 1. Grid Crosshairs
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = 1.dp.toPx()
                )

                // 2. Rotating Radar Scanner Sweep Beam
                val sweepRad = Math.toRadians((sweepAngle - 90.0)).toFloat()
                val sweepEnd = Offset(
                    x = centerX + maxRadius * kotlin.math.cos(sweepRad),
                    y = centerY + maxRadius * kotlin.math.sin(sweepRad)
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            (if (isBreach) CriticalRed else OrbitalCyan).copy(alpha = 0.45f),
                            (if (isBreach) CriticalRed else OrbitalCyan).copy(alpha = 0.05f)
                        ),
                        start = Offset(centerX, centerY),
                        end = sweepEnd
                    ),
                    start = Offset(centerX, centerY),
                    end = sweepEnd,
                    strokeWidth = 1.8.dp.toPx()
                )

                // 3. Concentric range rings: 50m (0.33x) and 100m (0.67x)
                val ringFractions = listOf(0.333f, 0.666f)
                ringFractions.forEach { fraction ->
                    drawCircle(
                        color = Color(0xFF162544),
                        radius = maxRadius * fraction,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // 4. 150m Safe Boundary Perimeter Circle
                val boundaryColor = if (isBreach) CriticalRed else SatelliteGreen
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

                // Perimeter background fill
                drawCircle(
                    color = if (isBreach) CriticalRed.copy(alpha = 0.08f) else SatelliteGreen.copy(alpha = 0.05f),
                    radius = maxRadius,
                    center = Offset(centerX, centerY)
                )

                // Perimeter stroke
                drawCircle(
                    color = boundaryColor.copy(alpha = if (isBreach) pulseAlpha else 0.85f),
                    radius = maxRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(
                        width = if (isBreach) 2.5.dp.toPx() else 2.dp.toPx(),
                        pathEffect = dashEffect
                    )
                )

                // 5. Center Landmark (Care Facility Home Base)
                drawCircle(
                    color = OrbitalCyan.copy(alpha = 0.3f),
                    radius = 12.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = ActionPrimaryBlue,
                    radius = 6.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(centerX, centerY)
                )

                // 6. Dynamic Smooth Patient Position Calculation
                val angleRad = Math.toRadians((animatedBearing - 90.0)).toFloat()
                val patientOffset = Offset(
                    x = centerX + (maxRadius * animatedDistanceRatio * kotlin.math.cos(angleRad)).coerceIn(-maxRadius * 1.3f, maxRadius * 1.3f),
                    y = centerY + (maxRadius * animatedDistanceRatio * kotlin.math.sin(angleRad)).coerceIn(-maxRadius * 1.3f, maxRadius * 1.3f)
                )

                // 7. Vector line connecting Home to Patient
                val vectorDash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                drawLine(
                    color = if (isBreach) CriticalRed.copy(alpha = 0.7f) else OrbitalCyan.copy(alpha = 0.5f),
                    start = Offset(centerX, centerY),
                    end = patientOffset,
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = vectorDash
                )

                // 8. Pulsing Patient Radar Beacon
                val beaconColor = if (isBreach) CriticalRed else SatelliteGreen
                drawCircle(
                    color = beaconColor.copy(alpha = pulseAlpha * 0.4f),
                    radius = 16.dp.toPx() * pulseRadiusScale,
                    center = patientOffset
                )
                drawCircle(
                    color = beaconColor,
                    radius = 6.dp.toPx(),
                    center = patientOffset
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = patientOffset
                )
            }

            // Radar HUD Overlays
            // Top Left: Satellite Telemetry Tag
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isBreach) CriticalRed else SatelliteGreen)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "RADAR: ${patientLocation.safeRadiusMeters.toInt()}M SAFE PERIMETER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBreach) CriticalRed else OrbitalCyan
                )
            }

            // Top Right: Live Distance & Status Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isBreach) Color(0xFF450A0A) else Color(0xFF064E3B),
                border = BorderStroke(1.dp, if (isBreach) CriticalRed else SatelliteGreen),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = if (isBreach) "🚨 OUTSIDE (+${patientLocation.distanceToBoundaryMeters}m)" else "🟢 SAFE (${patientLocation.distanceToBoundaryMeters}m inside)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            // Center Landmark Label
            Text(
                text = "🏡 Care Home Base",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 28.dp)
            )

            // Bottom Left: Patient Pin Indicator Label
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👤 ${patientLocation.locationName} (${patientLocation.distanceFromCenterMeters.toInt()}m)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isBreach) CriticalRed else Color(0xFFE2E8F0)
                    )
                }
            }

            // Bottom Right: Boundary Scale & Active Mode
            Text(
                text = "${patientLocation.trackingMode} • 150m Circle",
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF64748B),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
    }
}
