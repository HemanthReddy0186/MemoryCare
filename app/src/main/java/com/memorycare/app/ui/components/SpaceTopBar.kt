package com.memorycare.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.model.SatelliteSyncStatus
import com.memorycare.app.model.UserRole
import com.memorycare.app.ui.theme.CriticalRed
import com.memorycare.app.ui.theme.DeepSpaceNavy
import com.memorycare.app.ui.theme.OrbitalCyan
import com.memorycare.app.ui.theme.SatelliteGreen
import com.memorycare.app.ui.theme.SpaceBorder
import com.memorycare.app.ui.theme.SpaceNavySurface
import com.memorycare.app.ui.theme.StarYellow

import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun SpaceTopBar(
    title: String,
    subtitle: String? = null,
    userRole: UserRole = UserRole.NONE,
    satelliteStatus: SatelliteSyncStatus? = null,
    notificationCount: Int = 0,
    unreadAlertsCount: Int = notificationCount,
    onNotificationClick: (() -> Unit)? = null,
    onAlertBellClick: (() -> Unit)? = onNotificationClick,
    onSignOutClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onSyncClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeNotificationCount = if (unreadAlertsCount > 0) unreadAlertsCount else notificationCount
    val activeNotificationAction = onAlertBellClick ?: onNotificationClick
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DeepSpaceNavy,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Main Top Bar Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Back button (if available) or Satellite Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SpaceNavySurface)
                                .border(1.dp, SpaceBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go Back",
                                tint = OrbitalCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        // Satellite Emblem
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SpaceNavySurface)
                                .border(1.5.dp, OrbitalCyan, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🛰️",
                                fontSize = 22.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                color = OrbitalCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Right: Bell notification icon, Sync, and Role Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Notification Bell Icon with Badge Count
                    if (activeNotificationAction != null) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SpaceNavySurface)
                                .border(1.dp, if (activeNotificationCount > 0) StarYellow else SpaceBorder, RoundedCornerShape(10.dp))
                                .clickable { activeNotificationAction() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts & Notifications",
                                tint = if (activeNotificationCount > 0) StarYellow else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            if (activeNotificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(CriticalRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (activeNotificationCount > 9) "9+" else "$activeNotificationCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (onSyncClick != null) {
                        IconButton(
                            onClick = onSyncClick,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpaceNavySurface)
                                .border(1.dp, SpaceBorder, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Manual Satellite Sync",
                                tint = StarYellow,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (userRole != UserRole.NONE) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (userRole == UserRole.PATIENT) Color(0xFF1B5E20) else Color(0xFF0D47A1),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (userRole == UserRole.PATIENT) SatelliteGreen else OrbitalCyan
                            )
                        ) {
                            Text(
                                text = if (userRole == UserRole.PATIENT) "PATIENT" else "CARETAKER",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (onSignOutClick != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onSignOutClick,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpaceNavySurface)
                                .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out Profile",
                                tint = Color(0xFFFF8A80),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Satellite Status Telemetry Chip Row
            if (satelliteStatus != null) {
                Spacer(modifier = Modifier.height(8.dp))
                SatelliteStatusChip(satelliteStatus = satelliteStatus)
            }
        }
    }
}

@Composable
fun SatelliteStatusChip(
    satelliteStatus: SatelliteSyncStatus,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = SpaceNavySurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Blinking Pulse Indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(if (satelliteStatus.isConnected) SatelliteGreen else Color.Red)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🛰️ ${satelliteStatus.satelliteId} • ${satelliteStatus.groundStation}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "Live Sync (${satelliteStatus.latencyMs}ms)",
                color = OrbitalCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
