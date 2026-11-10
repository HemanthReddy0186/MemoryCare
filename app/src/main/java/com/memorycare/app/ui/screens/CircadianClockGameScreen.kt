package com.memorycare.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.memorycare.app.util.rememberVoiceAssistant
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.model.CircadianStage
import com.memorycare.app.model.DayRoutineOption
import com.memorycare.app.model.DifficultyLevel
import com.memorycare.app.model.GameType
import com.memorycare.app.ui.components.AccessibleBigButton
import com.memorycare.app.ui.components.SpaceTopBar
import com.memorycare.app.ui.theme.ActionPrimaryBlue
import com.memorycare.app.ui.theme.ActionSuccessGreen
import com.memorycare.app.ui.theme.DeepSpaceNavy
import com.memorycare.app.ui.theme.ElderlyTextPrimary
import com.memorycare.app.ui.theme.ElderlyTextSecondary
import com.memorycare.app.ui.theme.OrbitalCyan
import com.memorycare.app.ui.theme.SpaceNavyCard
import com.memorycare.app.util.SoundscapeSynthesizer
import com.memorycare.app.viewmodel.AdaptiveAdaptationResult
import com.memorycare.app.viewmodel.MemoryCareViewModel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Screen 4: "Journey Through a Day" (Circadian Rhythm & Interactive Clock Orientation)
 * Multi-stage clinical reality orientation therapy game countering Sundowning syndrome
 * and exercising the parieto-frontal Clock Drawing Test (CDT).
 */
@Composable
fun CircadianClockGameScreen(
    viewModel: MemoryCareViewModel,
    onNavigateBack: () -> Unit
) {
    val difficultyMap by viewModel.difficultyPrefs.collectAsState()
    val currentDifficulty = difficultyMap[GameType.NUMBER_SEQUENCE] ?: DifficultyLevel.EASY
    val userRole by viewModel.userRole.collectAsState()
    val satelliteStatus by viewModel.satelliteStatus.collectAsState()

    val voiceAssistant = rememberVoiceAssistant()
    val haptics = LocalHapticFeedback.current
    val soundSynthesizer = remember { SoundscapeSynthesizer() }

    DisposableEffect(Unit) {
        onDispose {
            soundSynthesizer.stop()
        }
    }

    // Circadian stages for the day's journey (4 stages)
    val stages = remember(currentDifficulty) { getCircadianDayStages(currentDifficulty) }
    var currentStageIndex by remember(currentDifficulty) { mutableIntStateOf(0) }
    val currentStage = stages.getOrElse(currentStageIndex) { stages.first() }

    // Interactive clock state (1 to 12)
    var selectedHour by remember(currentStageIndex) { mutableIntStateOf(12) }
    var clockMatched by remember(currentStageIndex) { mutableStateOf(false) }
    var selectedRoutineOption by remember(currentStageIndex) { mutableStateOf<DayRoutineOption?>(null) }
    var stageCompleted by remember(currentStageIndex) { mutableStateOf(false) }

    // Scoring & Telemetry
    var totalClockAttempts by remember { mutableIntStateOf(0) }
    var correctClockAttempts by remember { mutableIntStateOf(0) }
    var totalRoutineAttempts by remember { mutableIntStateOf(0) }
    var correctRoutineAttempts by remember { mutableIntStateOf(0) }
    val gameStartTime = remember { mutableLongStateOf(System.currentTimeMillis()) }

    var isSessionCompleted by remember { mutableStateOf(false) }
    var adaptationResult by remember { mutableStateOf<AdaptiveAdaptationResult?>(null) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    // Idle hint timer (gives voice prompt after 12 seconds of hesitation)
    var secondsIdle by remember(currentStageIndex) { mutableIntStateOf(0) }
    LaunchedEffect(currentStageIndex, clockMatched, selectedRoutineOption) {
        secondsIdle = 0
        while (!stageCompleted) {
            delay(1000L)
            secondsIdle++
            if (secondsIdle == 12 && !clockMatched) {
                voiceAssistant.speak("Look at the clock. Try pointing the hand to number ${currentStage.targetHour}.")
            }
        }
    }

    // On stage load: speak ambient question
    LaunchedEffect(currentStageIndex) {
        voiceAssistant.speak(currentStage.promptQuestion)
    }

    Scaffold(
        topBar = {
            SpaceTopBar(
                title = "Journey Through a Day",
                subtitle = "Circadian Rhythm • ${currentDifficulty.label}",
                userRole = userRole,
                satelliteStatus = satelliteStatus,
                onBackClick = {
                    voiceAssistant.stop()
                    soundSynthesizer.stop()
                    onNavigateBack()
                },
                onSyncClick = { viewModel.triggerSatelliteSync() }
            )
        },
        containerColor = DeepSpaceNavy
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stage Progress Bar (1 of 4, 2 of 4...)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceNavyCard),
                border = BorderStroke(1.dp, OrbitalCyan.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stage ${currentStageIndex + 1} of ${stages.size}: ${currentStage.title}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(currentDifficulty.colorHex).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(currentDifficulty.colorHex))
                        ) {
                            Text(
                                text = currentStage.timeLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(currentDifficulty.colorHex),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (currentStageIndex + (if (stageCompleted) 1f else if (clockMatched) 0.5f else 0.1f)) / stages.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = OrbitalCyan,
                        trackColor = Color(0xFF1E293B)
                    )
                }
            }

            // Window of the World: Dynamic Sky & Sun/Moon Horizon
            AnimatedContent(
                targetState = currentStage,
                transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(400)) },
                label = "skyWindowAnimation"
            ) { stage ->
                SkyWindowView(stage = stage)
            }

            // Interactive Analog Clock with Mechanical Ticks & Drag
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceNavyCard),
                border = BorderStroke(1.5.dp, if (clockMatched) ActionSuccessGreen else OrbitalCyan.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (!clockMatched) "Set Clock Hour Hand to ${stageHourPrompt(currentStage)}" else "Hour Confirmed: ${currentStage.targetHour}:00 ${currentStage.targetPeriod} ✓",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (clockMatched) ActionSuccessGreen else Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (!clockMatched) "Tap or drag clock hand to number ${currentStage.targetHour}" else "Great! Now choose the routine that fits this hour below.",
                        fontSize = 12.sp,
                        color = if (clockMatched) Color(0xFF94A3B8) else Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Circular Analog Clock Face
                    AnalogClockFace(
                        targetHour = currentStage.targetHour,
                        selectedHour = selectedHour,
                        isMatched = clockMatched,
                        difficulty = currentDifficulty,
                        onHourChanged = { newHour ->
                            if (!clockMatched) {
                                if (selectedHour != newHour) {
                                    selectedHour = newHour
                                    soundSynthesizer.playClockTick()
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                    totalClockAttempts++
                                    if (newHour == currentStage.targetHour) {
                                        clockMatched = true
                                        correctClockAttempts++
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        voiceAssistant.speak("Perfect! The clock points to ${currentStage.targetHour}. What do we do at this time?")
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Daily Living Routine Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceNavyCard),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "What activity belongs to this hour of the day?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Tap the daily living routine that fits best",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    currentStage.routineOptions.forEach { option ->
                        val isSelected = selectedRoutineOption?.id == option.id
                        val isCorrect = option.isCorrect

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isSelected && isCorrect -> Color(0xFF064E3B)
                                isSelected && !isCorrect -> Color(0xFF7F1D1D)
                                else -> Color(0xFF0F172A)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = when {
                                    isSelected && isCorrect -> ActionSuccessGreen
                                    isSelected && !isCorrect -> Color(0xFFEF4444)
                                    else -> Color(0xFF334155)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (!stageCompleted) {
                                        totalRoutineAttempts++
                                        selectedRoutineOption = option
                                        if (option.isCorrect) {
                                            correctRoutineAttempts++
                                            stageCompleted = true
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            voiceAssistant.speak(currentStage.celebrationVoice)

                                            // Auto-advance or complete session after gentle delay
                                            if (currentStageIndex == stages.size - 1) {
                                                // Completed all 4 stages of the day!
                                                val durationSeconds = ((System.currentTimeMillis() - gameStartTime.longValue) / 1000L).toInt().coerceAtLeast(10)
                                                val totalPossible = (stages.size * 2)
                                                val totalEarned = correctClockAttempts + correctRoutineAttempts
                                                val accuracy = if (totalPossible > 0) {
                                                    ((totalEarned.toFloat() / totalPossible.toFloat()) * 100).toInt().coerceIn(40, 100)
                                                } else 100

                                                adaptationResult = viewModel.recordGameResult(
                                                    accuracyPercentage = accuracy,
                                                    totalFlips = totalClockAttempts + totalRoutineAttempts,
                                                    reactionTimeMs = (durationSeconds * 1000L / stages.size),
                                                    totalDurationSeconds = durationSeconds,
                                                    gameType = GameType.NUMBER_SEQUENCE,
                                                    currentDifficulty = currentDifficulty
                                                )
                                                isSessionCompleted = true
                                                showCompletionDialog = true
                                            }
                                        } else {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            voiceAssistant.speak(option.explanation)
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = option.emoji, fontSize = 26.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = option.explanation,
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                if (isSelected && isCorrect) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Correct",
                                        tint = ActionSuccessGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Next Stage Advance Button
                    if (stageCompleted && currentStageIndex < stages.size - 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        AccessibleBigButton(
                            text = "Next: ${stages[currentStageIndex + 1].title} ➔",
                            emoji = stages[currentStageIndex + 1].ambientSoundEmoji,
                            onClick = {
                                currentStageIndex++
                            },
                            containerColor = ActionSuccessGreen,
                            contentColor = Color.White,
                            minHeight = 46.dp,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Session Completion & Adaptive Promotion Dialog
    if (showCompletionDialog) {
        val result = adaptationResult
        AlertDialog(
            onDismissRequest = { /* Require button action */ },
            shape = RoundedCornerShape(20.dp),
            containerColor = SpaceNavyCard,
            title = {
                Text(
                    text = "A Full Day Beautifully Grounded! 🌅🌙",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "You walked through all 4 stages of the day—morning dawn, bright noon, sunset chai, and peaceful bedtime.",
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )

                    // Adaptive Transition Banner
                    result?.let { adapt ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E3A5F),
                            border = BorderStroke(1.dp, Color(adapt.newDifficulty.colorHex)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = adapt.transition.emoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = adapt.transition.message,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(adapt.newDifficulty.colorHex)
                                    )
                                    Text(
                                        text = "Calibrated for next session: ${adapt.newDifficulty.label}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                AccessibleBigButton(
                    text = "Journey Again ☀️",
                    emoji = "🔄",
                    onClick = {
                        showCompletionDialog = false
                        currentStageIndex = 0
                        totalClockAttempts = 0
                        correctClockAttempts = 0
                        totalRoutineAttempts = 0
                        correctRoutineAttempts = 0
                        gameStartTime.longValue = System.currentTimeMillis()
                    },
                    containerColor = ActionSuccessGreen,
                    contentColor = Color.White,
                    minHeight = 44.dp,
                    fontSize = 13.sp
                )
            },
            dismissButton = {
                AccessibleBigButton(
                    text = "Back to Home",
                    emoji = "🏠",
                    onClick = {
                        showCompletionDialog = false
                        onNavigateBack()
                    },
                    containerColor = Color(0xFF334155),
                    contentColor = Color.White,
                    minHeight = 44.dp,
                    fontSize = 12.sp
                )
            }
        )
    }
}

/**
 * Animated Sky Window rendering realistic daylight, sun/moon position, and horizon gradients.
 */
@Composable
private fun SkyWindowView(stage: CircadianStage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.5.dp, Color(0xFF475569))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        stage.skyGradientHexes.map { Color(it) }
                    )
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Sun or Moon position
                val cx = w * stage.sunNormalizedX
                val cy = h * stage.sunNormalizedY

                if (!stage.isNight) {
                    // Radiant Sun
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFEE58), Color(0xFFFFA000).copy(alpha = 0.4f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = 42.dp.toPx()
                        ),
                        radius = 42.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color(0xFFFFF9C4),
                        radius = 16.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                } else {
                    // Gentle Crescent Moon
                    drawCircle(
                        color = Color(0xFFFFF9C4),
                        radius = 16.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color(stage.skyGradientHexes.first()),
                        radius = 13.dp.toPx(),
                        center = Offset(cx - 5.dp.toPx(), cy - 4.dp.toPx())
                    )

                    // Ambient twinkling stars
                    val stars = listOf(
                        Offset(w * 0.15f, h * 0.25f),
                        Offset(w * 0.35f, h * 0.18f),
                        Offset(w * 0.75f, h * 0.28f),
                        Offset(w * 0.85f, h * 0.40f)
                    )
                    stars.forEach { pt ->
                        drawCircle(
                            color = Color.White.copy(alpha = 0.7f),
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    }
                }

                // Distant silhouetted courtyard trees / roofline
                drawLine(
                    color = Color(0xFF0F172A).copy(alpha = 0.65f),
                    start = Offset(0f, h),
                    end = Offset(w, h),
                    strokeWidth = 24.dp.toPx()
                )
            }

            // Window Header overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${stage.ambientSoundEmoji} ${stage.title}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Window to the World",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Interactive Analog Clock with large numbers, mechanical clicking, and rotatable hour hand.
 */
@Composable
private fun AnalogClockFace(
    targetHour: Int,
    selectedHour: Int,
    isMatched: Boolean,
    difficulty: DifficultyLevel,
    onHourChanged: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .size(240.dp)
            .pointerInput(isMatched) {
                if (!isMatched) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchOffset = change.position
                            val angleRad = atan2((touchOffset.y - center.y).toDouble(), (touchOffset.x - center.x).toDouble())
                            var deg = Math.toDegrees(angleRad) + 90.0
                            if (deg < 0) deg += 360.0

                            // Convert degree (0 to 360) to nearest hour (1 to 12)
                            val hourFraction = (deg / 30.0).roundToInt()
                            val hour = if (hourFraction == 0) 12 else hourFraction.coerceIn(1, 12)
                            onHourChanged(hour)
                        }
                    )
                }
            }
            .pointerInput(isMatched) {
                if (!isMatched) {
                    detectTapGestures { tapOffset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val angleRad = atan2((tapOffset.y - center.y).toDouble(), (tapOffset.x - center.x).toDouble())
                        var deg = Math.toDegrees(angleRad) + 90.0
                        if (deg < 0) deg += 360.0

                        val hourFraction = (deg / 30.0).roundToInt()
                        val hour = if (hourFraction == 0) 12 else hourFraction.coerceIn(1, 12)
                        onHourChanged(hour)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f - 8.dp.toPx()

            // Outer dial rim
            drawCircle(
                color = Color(0xFF0F172A),
                radius = radius,
                center = center
            )
            drawCircle(
                color = if (isMatched) ActionSuccessGreen else Color(0xFF38BDF8),
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
            )

            // Hour tick marks (1 to 12)
            for (h in 1..12) {
                val angleDeg = (h * 30.0) - 90.0
                val rad = Math.toRadians(angleDeg)
                val isMajor = (h % 3 == 0)

                val tickLen = if (isMajor) 12.dp.toPx() else 7.dp.toPx()
                val outerX = center.x + (radius - 6.dp.toPx()) * cos(rad).toFloat()
                val outerY = center.y + (radius - 6.dp.toPx()) * sin(rad).toFloat()
                val innerX = center.x + (radius - 6.dp.toPx() - tickLen) * cos(rad).toFloat()
                val innerY = center.y + (radius - 6.dp.toPx() - tickLen) * sin(rad).toFloat()

                drawLine(
                    color = if (h == targetHour) Color(0xFFFFD54F) else Color(0xFF64748B),
                    start = Offset(innerX, innerY),
                    end = Offset(outerX, outerY),
                    strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx()
                )
            }

            // Draw Hour Hand pointing to selectedHour
            val hourAngleDeg = (selectedHour * 30.0) - 90.0
            val hourRad = Math.toRadians(hourAngleDeg)
            val handLength = radius * 0.62f
            val handEndX = center.x + handLength * cos(hourRad).toFloat()
            val handEndY = center.y + handLength * sin(hourRad).toFloat()

            // Hand shadow / glow
            drawLine(
                color = if (isMatched) ActionSuccessGreen.copy(alpha = 0.5f) else Color(0xFF38BDF8).copy(alpha = 0.4f),
                start = center,
                end = Offset(handEndX, handEndY),
                strokeWidth = 10.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Core hand
            drawLine(
                color = if (isMatched) ActionSuccessGreen else Color.White,
                start = center,
                end = Offset(handEndX, handEndY),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center brass pin
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 7.dp.toPx(),
                center = center
            )
        }

        // Render Clock Numbers (1 to 12) as accessible touch targets
        val clockRadiusDp = 92.dp
        for (h in 1..12) {
            val angleDeg = (h * 30.0) - 90.0
            val rad = Math.toRadians(angleDeg)
            val xOff = (clockRadiusDp.value * cos(rad)).dp
            val yOff = (clockRadiusDp.value * sin(rad)).dp

            val isTarget = (h == targetHour)
            val isSelected = (h == selectedHour)

            Box(
                modifier = Modifier
                    .offset(x = xOff, y = yOff)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isTarget && isMatched -> ActionSuccessGreen
                            isTarget && !isMatched -> Color(0xFFF59E0B).copy(alpha = 0.25f)
                            isSelected -> Color(0xFF0284C7)
                            else -> Color.Transparent
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onHourChanged(h)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$h",
                    fontSize = if (h % 3 == 0) 15.sp else 13.sp,
                    fontWeight = if (isTarget || isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                    color = when {
                        isTarget && isMatched -> Color.White
                        isTarget && !isMatched -> Color(0xFFFFD54F)
                        isSelected -> Color.White
                        else -> Color(0xFF94A3B8)
                    }
                )
            }
        }
    }
}

private fun stageHourPrompt(stage: CircadianStage): String {
    return "${stage.targetHour}:00 ${stage.targetPeriod}"
}

/**
 * Predefined 4-Stage 24-Hour Circadian Stages.
 */
private fun getCircadianDayStages(difficulty: DifficultyLevel): List<CircadianStage> {
    return listOf(
        // Stage 1: Morning Dawn
        CircadianStage(
            stageId = "stage_morning",
            title = "Morning Dawn",
            timeLabel = "8:00 AM",
            targetHour = 8,
            targetPeriod = "AM",
            sunNormalizedX = 0.20f,
            sunNormalizedY = 0.35f,
            isNight = false,
            skyGradientHexes = listOf(0xFFFF8A80, 0xFFFFD54F, 0xFFFFE082),
            promptQuestion = "The golden morning sun is rising over the courtyard trees. Set the clock hand to 8:00 AM.",
            routineOptions = listOf(
                DayRoutineOption(
                    id = "m_tea",
                    title = "Drink Warm Water & Breakfast",
                    emoji = "🥣",
                    isCorrect = true,
                    explanation = "A warm breakfast and glass of water start the morning with health."
                ),
                DayRoutineOption(
                    id = "m_walk",
                    title = "Morning Verandah Stroll",
                    emoji = "🚶",
                    isCorrect = false,
                    explanation = "A lovely morning activity, but let us enjoy our morning tea and breakfast first."
                ),
                DayRoutineOption(
                    id = "m_night",
                    title = "Turn Off All Lights to Sleep",
                    emoji = "🌙",
                    isCorrect = false,
                    explanation = "We sleep at night. The morning sun has just risen!"
                )
            ),
            celebrationVoice = "Wonderful! 8:00 in the morning. Breakfast is served with fresh morning light.",
            ambientSoundEmoji = "🌅"
        ),
        // Stage 2: Midday Lunch
        CircadianStage(
            stageId = "stage_midday",
            title = "Midday Sun",
            timeLabel = "1:00 PM",
            targetHour = 1,
            targetPeriod = "PM",
            sunNormalizedX = 0.50f,
            sunNormalizedY = 0.20f,
            isNight = false,
            skyGradientHexes = listOf(0xFF0288D1, 0xFF29B6F6, 0xFF81D4FA),
            promptQuestion = "The sun is high overhead in the sky at midday. Move the clock hand to 1:00 PM.",
            routineOptions = listOf(
                DayRoutineOption(
                    id = "n_lunch",
                    title = "Warm Dal, Vegetables & Steaming Rice",
                    emoji = "🍲",
                    isCorrect = true,
                    explanation = "At 1:00 PM noon, the family gathers for a nourishing lunch."
                ),
                DayRoutineOption(
                    id = "n_sun",
                    title = "Look for the Morning Sunrise",
                    emoji = "🌅",
                    isCorrect = false,
                    explanation = "Sunrise happened hours ago at dawn; the sun is now shining at noon."
                ),
                DayRoutineOption(
                    id = "n_milk",
                    title = "Drink Turmeric Bedtime Milk",
                    emoji = "🥛",
                    isCorrect = false,
                    explanation = "Warm bedtime milk is for late evening before sleep."
                )
            ),
            celebrationVoice = "Excellent! 1:00 in the afternoon. Time to sit down and enjoy warm lunch.",
            ambientSoundEmoji = "☀️"
        ),
        // Stage 3: Sunset Chai Time (Sundowning Anchor)
        CircadianStage(
            stageId = "stage_sunset",
            title = "Sunset Chai Time",
            timeLabel = "5:00 PM",
            targetHour = 5,
            targetPeriod = "PM",
            sunNormalizedX = 0.82f,
            sunNormalizedY = 0.40f,
            isNight = false,
            skyGradientHexes = listOf(0xFFE65100, 0xFFC2185B, 0xFF4A148C),
            promptQuestion = "The sun is setting into orange twilight. Set the clock hand to 5:00 PM.",
            routineOptions = listOf(
                DayRoutineOption(
                    id = "s_chai",
                    title = "Hot Cardamom Chai & Biscuits",
                    emoji = "🫖",
                    isCorrect = true,
                    explanation = "At 5:00 PM, the kettle whistles for soothing evening chai with family."
                ),
                DayRoutineOption(
                    id = "s_brush",
                    title = "Morning Teeth Brushing",
                    emoji = "🪥",
                    isCorrect = false,
                    explanation = "We brush our teeth in the morning. Evening is for tea and relaxation."
                ),
                DayRoutineOption(
                    id = "s_noon",
                    title = "Heavy Midday Meal",
                    emoji = "🍛",
                    isCorrect = false,
                    explanation = "Lunch was at 1:00 PM. Sunset is time for gentle snacks and tea."
                )
            ),
            celebrationVoice = "Splendid! 5:00 PM sunset. The warm cup of chai brings comfort to the evening.",
            ambientSoundEmoji = "🌇"
        ),
        // Stage 4: Night Bedtime & Rest
        CircadianStage(
            stageId = "stage_night",
            title = "Night Rest",
            timeLabel = "10:00 PM",
            targetHour = 10,
            targetPeriod = "PM",
            sunNormalizedX = 0.70f,
            sunNormalizedY = 0.25f,
            isNight = true,
            skyGradientHexes = listOf(0xFF0A1128, 0xFF1E1B4B, 0xFF312E81),
            promptQuestion = "The crescent moon is out and stars are twinkling. Set the clock hand to 10:00 PM.",
            routineOptions = listOf(
                DayRoutineOption(
                    id = "b_sleep",
                    title = "Lock Front Door & Turn on Night Lamp",
                    emoji = "🔒",
                    isCorrect = true,
                    explanation = "At 10:00 PM, everything is locked, safe, and peaceful for a sound sleep."
                ),
                DayRoutineOption(
                    id = "b_walk",
                    title = "Step Out for a Morning Walk",
                    emoji = "👟",
                    isCorrect = false,
                    explanation = "It is dark outside! Morning walks are when the sun rises tomorrow."
                ),
                DayRoutineOption(
                    id = "b_noon",
                    title = "Afternoon Gardening",
                    emoji = "🪴",
                    isCorrect = false,
                    explanation = "Plants are resting in the moonlight. Time for you to sleep as well."
                )
            ),
            celebrationVoice = "Peaceful! 10:00 PM at night. Everything is safe and locked. Rest well.",
            ambientSoundEmoji = "🌙"
        )
    )
}
