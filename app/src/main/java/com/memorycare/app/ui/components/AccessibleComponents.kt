package com.memorycare.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.model.AlertSeverity
import com.memorycare.app.model.CaretakerAlert
import com.memorycare.app.ui.theme.ActionPrimaryBlue
import com.memorycare.app.ui.theme.AlertAmber
import com.memorycare.app.ui.theme.CriticalRed
import com.memorycare.app.ui.theme.DeepSpaceNavy
import com.memorycare.app.ui.theme.ElderlyCardBorder
import com.memorycare.app.ui.theme.ElderlyTextPrimary
import com.memorycare.app.ui.theme.ElderlyTextSecondary
import com.memorycare.app.ui.theme.OrbitalCyan
import com.memorycare.app.ui.theme.SatelliteGreen
import com.memorycare.app.ui.theme.SpaceBorder
import com.memorycare.app.ui.theme.SpaceNavyCard

import androidx.compose.ui.unit.TextUnit

/**
 * Large elderly-accessible button with configurable height and font size.
 */
@Composable
fun AccessibleBigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emoji: String? = null,
    containerColor: Color = ActionPrimaryBlue,
    contentColor: Color = Color.White,
    minHeight: Dp = 64.dp,
    fontSize: TextUnit = 22.sp,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color.LightGray,
            disabledContentColor = Color.DarkGray
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            if (emoji != null) {
                Text(
                    text = emoji,
                    fontSize = (fontSize.value + 6).sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 10.dp)
                )
            }
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Large touch card for main menu actions with prominent icon and subtitle.
 */
@Composable
fun AccessibleActionCard(
    title: String,
    description: String,
    emoji: String,
    badgeText: String? = null,
    badgeColor: Color = ActionPrimaryBlue,
    containerColor: Color = Color.White,
    borderColor: Color = ElderlyCardBorder,
    titleSize: TextUnit = 24.sp,
    descriptionSize: TextUnit = 18.sp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large Emoji Icon Container
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF0F4FA))
                    .border(1.5.dp, Color(0xFFD0D7DE), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 40.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, badgeColor),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    color = ElderlyTextPrimary,
                    fontSize = titleSize,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = (titleSize.value + 6).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = ElderlyTextSecondary,
                    fontSize = descriptionSize,
                    lineHeight = (descriptionSize.value + 6).sp
                )
            }
        }
    }
}

/**
 * Animated Circular Effectiveness Score Ring for Caretaker Dashboard.
 */
@Composable
fun EffectivenessScoreRing(
    scorePercentage: Int,
    subtitle: String = "Cognitive Stability Index",
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    val animatedScore by animateFloatAsState(
        targetValue = scorePercentage.toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "scoreAnimation"
    )

    val ringColor = when {
        scorePercentage >= 80 -> SatelliteGreen
        scorePercentage >= 60 -> AlertAmber
        else -> CriticalRed
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceNavyCard),
        border = BorderStroke(1.5.dp, SpaceBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚡ EFFECTIVENESS SCORE",
                color = OrbitalCyan,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center
            ) {
                // Background Track
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 16.dp.toPx()
                    drawCircle(
                        color = Color(0xFF0F2038),
                        style = Stroke(width = strokeWidth)
                    )
                    // Progress Arc
                    val sweepAngle = (animatedScore / 100f) * 360f
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                ringColor.copy(alpha = 0.6f),
                                ringColor
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${animatedScore.toInt()}%",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = when {
                            scorePercentage >= 80 -> "Optimal 🚀"
                            scorePercentage >= 60 -> "Moderate ⚖️"
                            else -> "Needs Review ⚠️"
                        },
                        color = ringColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = subtitle,
                color = Color(0xFFB0BEC5),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Geofence & Emergency Alert Banner for Caretaker.
 */
@Composable
fun GeofenceAlertBanner(
    alert: CaretakerAlert,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> CriticalRed
        AlertSeverity.WARNING -> AlertAmber
        AlertSeverity.INFO -> ActionPrimaryBlue
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (alert.severity == AlertSeverity.CRITICAL) "🚨" else "⚠️",
                    fontSize = 30.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = alert.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = alert.message,
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.clickable { onDismiss() }
            ) {
                Text(
                    text = "Acknowledge",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
