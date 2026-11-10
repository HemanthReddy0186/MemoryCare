package com.memorycare.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.model.AlertSeverity
import com.memorycare.app.model.CaretakerAlert
import com.memorycare.app.viewmodel.MemoryCareViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen D3: Ultra-Simple, Crash-Proof Alert Center Screen.
 * Uses a single vertical-scrolling Column with no nested scroll parents,
 * safe try-catch item wrappers, and direct state-driven dismissal.
 */
@Composable
fun AlertCenterScreen(
    viewModel: MemoryCareViewModel,
    onNavigateBack: () -> Unit
) {
    val alertsList by viewModel.alertsList.collectAsState()
    val activeAlerts = alertsList.filter { !it.isDismissed }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Row: Title and Active Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔔 Alert Center",
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${activeAlerts.size} active",
                    fontSize = 16.sp,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dismiss All Button (Single, top-right aligned)
            if (activeAlerts.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { viewModel.dismissAllAlerts() }
                    ) {
                        Text(
                            text = "Dismiss All",
                            color = Color(0xFFFF5252),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Alerts List
            if (activeAlerts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1A1A2E),
                    border = BorderStroke(1.dp, Color(0xFF2E3856)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✅ No alerts to show",
                            fontSize = 18.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All patient activities and telemetry are normal.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                activeAlerts.forEach { alert ->
                    AlertCard(
                        alert = alert,
                        onDismiss = { viewModel.dismissAlert(alert.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Back Button
            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A1A2E)
                ),
                border = BorderStroke(1.dp, Color(0xFF3B4874))
            ) {
                Text(
                    text = "← Back to Dashboard",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Clean, Color-Coded Alert Card:
 * - CRITICAL: Dark Red (#4A0000) with 🚨 icon
 * - WARNING: Dark Amber (#4A3A00) with ⚠️ icon
 * - INFO / GENERAL: Dark Blue (#001A4A) with ℹ️ icon
 * - ✕ Dismiss button on each item
 */
@Composable
private fun AlertCard(
    alert: CaretakerAlert,
    onDismiss: () -> Unit
) {
    val (bgColor, borderColor, priorityIcon) = when (alert.severity) {
        AlertSeverity.CRITICAL -> Triple(Color(0xFF4A0000), Color(0xFFE53935), "🚨")
        AlertSeverity.WARNING -> Triple(Color(0xFF4A3A00), Color(0xFFFF8F00), "⚠️")
        AlertSeverity.INFO -> Triple(Color(0xFF001A4A), Color(0xFF1E88E5), "ℹ️")
    }

    val formattedTime = try {
        if (alert.timestamp > 0) {
            SimpleDateFormat("hh:mm a · dd MMM", Locale.getDefault()).format(Date(alert.timestamp))
        } else {
            "Recent"
        }
    } catch (_: Exception) {
        "Recent"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$priorityIcon ${alert.title.ifBlank { "Alert" }}",
                    fontSize = 17.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        text = "✕",
                        color = Color.LightGray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = alert.message.ifBlank { "No additional message." },
                fontSize = 14.sp,
                color = Color(0xFFE0E0E0),
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formattedTime,
                fontSize = 12.sp,
                color = Color(0xFFB0BEC5)
            )
        }
    }
}
