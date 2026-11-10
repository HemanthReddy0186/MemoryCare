package com.memorycare.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.model.AdaptiveTransition
import com.memorycare.app.model.DifficultyLevel
import com.memorycare.app.model.GameType
import com.memorycare.app.model.UserRole
import com.memorycare.app.ui.components.AccessibleBigButton
import com.memorycare.app.ui.components.SpaceTopBar
import com.memorycare.app.ui.theme.ActionPrimaryBlue
import com.memorycare.app.ui.theme.ActionSuccessGreen
import com.memorycare.app.ui.theme.ActionWarningOrange
import com.memorycare.app.ui.theme.CriticalRed
import com.memorycare.app.ui.theme.DeepSpaceNavy
import com.memorycare.app.ui.theme.ElderlyBgLight
import com.memorycare.app.ui.theme.ElderlyTextPrimary
import com.memorycare.app.ui.theme.ElderlyTextSecondary
import com.memorycare.app.ui.theme.MatchedCardBorder
import com.memorycare.app.ui.theme.OrbitalCyan
import com.memorycare.app.viewmodel.AdaptiveAdaptationResult
import com.memorycare.app.viewmodel.MemoryCareViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class NumberCardItem(
    val id: Int,
    val value: Int,
    val isTappedCorrectly: Boolean = false,
    val isTappedWrong: Boolean = false,
    val isRevealedTemporarily: Boolean = false
)

/**
 * Number Sequence Game with FULLY AUTOMATED Adaptive Difficulty parameters:
 * - EASY: 4 numbers, permanently visible, range 1-6
 * - MEDIUM: 4 numbers, hidden after 3 seconds, range 1-9 (memory recall)
 * - HARD: 6 numbers, hidden after 2 seconds, range 1-12 (memory recall)
 */
@Composable
fun NumberSequenceGameScreen(
    viewModel: MemoryCareViewModel,
    onNavigateBack: () -> Unit
) {
    val satelliteStatus by viewModel.satelliteStatus.collectAsState()
    val scope = rememberCoroutineScope()

    // Automatically load difficulty without manual selection
    val currentDifficulty = remember {
        viewModel.getAdaptiveDifficulty(GameType.NUMBER_SEQUENCE)
    }

    val numberCards = remember { mutableStateListOf<NumberCardItem>() }
    var sortedTargets by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTargetIndex by remember { mutableIntStateOf(0) }

    // Memory recall hide parameters
    val shouldHideNumbers = currentDifficulty != DifficultyLevel.EASY
    val previewSeconds = when (currentDifficulty) {
        DifficultyLevel.EASY -> 0
        DifficultyLevel.MEDIUM -> 3
        DifficultyLevel.HARD -> 2
    }
    var previewCountdown by remember { mutableIntStateOf(previewSeconds) }
    var areNumbersHidden by remember { mutableStateOf(false) }

    var totalAttempts by remember { mutableIntStateOf(0) }
    var errorCount by remember { mutableIntStateOf(0) }
    var gameDurationSeconds by remember { mutableIntStateOf(0) }
    var isGameActive by remember { mutableStateOf(true) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    var lastActionTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val reactionTimesList = remember { mutableStateListOf<Long>() }

    var calculatedAccuracy by remember { mutableIntStateOf(100) }
    var avgReactionTimeMs by remember { mutableLongStateOf(0L) }

    var adaptationResult by remember { mutableStateOf<AdaptiveAdaptationResult?>(null) }

    fun initGame() {
        val (count, maxRange) = when (currentDifficulty) {
            DifficultyLevel.EASY -> Pair(4, 6)   // 4 numbers, range 1-6
            DifficultyLevel.MEDIUM -> Pair(4, 9) // 4 numbers, range 1-9
            DifficultyLevel.HARD -> Pair(6, 12)  // 6 numbers, range 1-12
        }

        val generatedNumbers = mutableSetOf<Int>()
        while (generatedNumbers.size < count) {
            generatedNumbers.add(Random.nextInt(1, maxRange + 1))
        }

        val sortedList = generatedNumbers.toList().sorted()
        sortedTargets = sortedList
        currentTargetIndex = 0

        val shuffledItems = generatedNumbers.shuffled().mapIndexed { index, value ->
            NumberCardItem(id = index, value = value)
        }

        numberCards.clear()
        numberCards.addAll(shuffledItems)

        totalAttempts = 0
        errorCount = 0
        gameDurationSeconds = 0
        previewCountdown = previewSeconds
        areNumbersHidden = false
        isGameActive = true
        showGameOverDialog = false
        feedbackMessage = if (shouldHideNumbers) "👀 Memorize number positions ($previewCountdown s)!" else "Find and tap the lowest number."
        reactionTimesList.clear()
        lastActionTimestamp = System.currentTimeMillis()
    }

    LaunchedEffect(Unit) {
        initGame()
    }

    // Preview Countdown for Medium/Hard memory recall
    LaunchedEffect(isGameActive, previewCountdown) {
        if (shouldHideNumbers && previewCountdown > 0 && isGameActive) {
            delay(1000)
            previewCountdown--
            if (previewCountdown == 0) {
                areNumbersHidden = true
                feedbackMessage = "🧠 Numbers hidden! Tap from SMALLEST to LARGEST from memory."
            } else {
                feedbackMessage = "👀 Memorize number positions ($previewCountdown s)!"
            }
        }
    }

    // Timer loop
    LaunchedEffect(isGameActive, areNumbersHidden) {
        while (isGameActive && (!shouldHideNumbers || areNumbersHidden)) {
            delay(1000)
            gameDurationSeconds++
        }
    }

    fun onNumberCardClicked(card: NumberCardItem) {
        if (!isGameActive || card.isTappedCorrectly || (shouldHideNumbers && !areNumbersHidden)) return

        val now = System.currentTimeMillis()
        val reactionTime = (now - lastActionTimestamp).coerceIn(400L, 8000L)
        reactionTimesList.add(reactionTime)
        lastActionTimestamp = now

        totalAttempts++
        val expectedTarget = sortedTargets.getOrNull(currentTargetIndex) ?: return

        if (card.value == expectedTarget) {
            val cardIdx = numberCards.indexOfFirst { it.id == card.id }
            if (cardIdx != -1) {
                numberCards[cardIdx] = numberCards[cardIdx].copy(isTappedCorrectly = true, isTappedWrong = false)
            }
            currentTargetIndex++
            feedbackMessage = "🌟 Correct! Number ${card.value}."

            if (currentTargetIndex >= sortedTargets.size) {
                isGameActive = false
                val totalSteps = sortedTargets.size
                calculatedAccuracy = if (totalAttempts > 0) {
                    ((totalSteps.toDouble() / totalAttempts) * 100).toInt().coerceIn(35, 100)
                } else 100

                avgReactionTimeMs = if (reactionTimesList.isNotEmpty()) {
                    reactionTimesList.average().toLong()
                } else 1100L

                val result = viewModel.recordGameResult(
                    accuracyPercentage = calculatedAccuracy,
                    totalFlips = totalAttempts,
                    reactionTimeMs = avgReactionTimeMs,
                    totalDurationSeconds = gameDurationSeconds,
                    gameType = GameType.NUMBER_SEQUENCE,
                    currentDifficulty = currentDifficulty
                )
                adaptationResult = result

                scope.launch {
                    delay(500)
                    showGameOverDialog = true
                }
            }
        } else {
            errorCount++
            feedbackMessage = "💡 Next smallest number is $expectedTarget"
            val cardIdx = numberCards.indexOfFirst { it.id == card.id }
            if (cardIdx != -1) {
                numberCards[cardIdx] = numberCards[cardIdx].copy(isTappedWrong = true, isRevealedTemporarily = true)
                scope.launch {
                    delay(900)
                    if (cardIdx < numberCards.size) {
                        numberCards[cardIdx] = numberCards[cardIdx].copy(isTappedWrong = false, isRevealedTemporarily = false)
                    }
                }
            }
        }
    }

    val columnsCount = if (currentDifficulty == DifficultyLevel.HARD) 3 else 2
    val difficultyDesc = when (currentDifficulty) {
        DifficultyLevel.EASY -> "4 Numbers (Range 1-6 • Permanent)"
        DifficultyLevel.MEDIUM -> "4 Numbers (Range 1-9 • 3s Memory)"
        DifficultyLevel.HARD -> "6 Numbers (Range 1-12 • 2s Memory)"
    }

    Scaffold(
        topBar = {
            SpaceTopBar(
                title = "Number Sequence",
                subtitle = difficultyDesc,
                userRole = UserRole.PATIENT,
                satelliteStatus = satelliteStatus,
                onBackClick = onNavigateBack,
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Difficulty Display Badge (No manual selection UI)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(currentDifficulty.colorHex).copy(alpha = 0.15f),
                    border = BorderStroke(1.5.dp, Color(currentDifficulty.colorHex))
                ) {
                    Text(
                        text = "⭐ Difficulty: ${currentDifficulty.label}",
                        color = Color(currentDifficulty.colorHex),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFD0D7DE))
                ) {
                    Text(
                        text = "⏱️ ${gameDurationSeconds}s",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ActionPrimaryBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Game Instructions Card (High Accessibility)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, ActionPrimaryBlue.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔢 Tap numbers from SMALLEST to LARGEST",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ActionPrimaryBlue,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = feedbackMessage ?: "Find and tap the lowest number.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (feedbackMessage?.contains("Correct") == true) ActionSuccessGreen else if (feedbackMessage?.contains("Next") == true) CriticalRed else ElderlyTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sequence Step Progress Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                sortedTargets.forEachIndexed { idx, value ->
                    val isDone = idx < currentTargetIndex
                    val isCurrent = idx == currentTargetIndex
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isDone -> ActionSuccessGreen
                            isCurrent -> ActionWarningOrange
                            else -> Color(0xFFE0E0E0)
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = if (isDone) "✓ $value" else "$value",
                            color = if (isDone || isCurrent) Color.White else Color.DarkGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Big Number Cards Grid
            val chunkedNumberCards = numberCards.chunked(columnsCount)
            chunkedNumberCards.forEach { rowCards ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    rowCards.forEach { card ->
                        Box(modifier = Modifier.weight(1f)) {
                            NumberCardButton(
                                card = card,
                                areNumbersHidden = areNumbersHidden,
                                onClick = { onNumberCardClicked(card) }
                            )
                        }
                    }
                    if (rowCards.size < columnsCount) {
                        repeat(columnsCount - rowCards.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccessibleBigButton(
                    text = "Restart",
                    emoji = "🔄",
                    onClick = { initGame() },
                    containerColor = Color(0xFF455A64),
                    modifier = Modifier.weight(1f),
                    minHeight = 56.dp
                )
                AccessibleBigButton(
                    text = "Done / Exit",
                    emoji = "🏠",
                    onClick = onNavigateBack,
                    containerColor = ActionPrimaryBlue,
                    modifier = Modifier.weight(1f),
                    minHeight = 56.dp
                )
            }
        }
    }

    // Celebration & Adaptation Result Dialog
    if (showGameOverDialog) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 PERFECT SEQUENCE! 🌟",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ActionSuccessGreen,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "All numbers ordered ascending successfully!",
                        fontSize = 16.sp,
                        color = ElderlyTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFE8F5E9),
                        border = BorderStroke(2.dp, MatchedCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Cognitive Accuracy",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ActionSuccessGreen
                            )
                            Text(
                                text = "$calculatedAccuracy%",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ActionSuccessGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Avg Reaction Time: ${avgReactionTimeMs}ms",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = ElderlyTextPrimary
                            )
                            Text(
                                text = "Time: ${gameDurationSeconds}s • Errors: $errorCount",
                                fontSize = 13.sp,
                                color = ElderlyTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Adaptive Difficulty Adjustment Banner
                    adaptationResult?.let { adapt ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (adapt.transition) {
                                AdaptiveTransition.PROMOTED -> Color(0xFFEDE7F6)
                                AdaptiveTransition.DEMOTED -> Color(0xFFE3F2FD)
                                AdaptiveTransition.MAINTAINED -> Color(0xFFFFF8E1)
                            },
                            border = BorderStroke(
                                1.5.dp,
                                when (adapt.transition) {
                                    AdaptiveTransition.PROMOTED -> Color(0xFF7B1FA2)
                                    AdaptiveTransition.DEMOTED -> ActionPrimaryBlue
                                    AdaptiveTransition.MAINTAINED -> Color(0xFFFFA000)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = adapt.transition.emoji, fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                                Column {
                                    Text(
                                        text = "Next Session: ${adapt.newDifficulty.label}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ElderlyTextPrimary
                                    )
                                    Text(
                                        text = adapt.transition.message,
                                        fontSize = 12.sp,
                                        color = ElderlyTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DeepSpaceNavy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🛰️", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = "Telemetry saved & synced with Caretaker Dashboard.",
                                color = OrbitalCyan,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccessibleBigButton(
                        text = "Play Another Round",
                        emoji = "🔢",
                        onClick = { initGame() },
                        containerColor = ActionSuccessGreen,
                        minHeight = 56.dp
                    )
                    AccessibleBigButton(
                        text = "Return to Home",
                        emoji = "🏠",
                        onClick = onNavigateBack,
                        containerColor = ActionPrimaryBlue,
                        minHeight = 56.dp
                    )
                }
            }
        )
    }
}

@Composable
private fun NumberCardButton(
    card: NumberCardItem,
    areNumbersHidden: Boolean,
    onClick: () -> Unit
) {
    val showHidden = areNumbersHidden && !card.isTappedCorrectly && !card.isRevealedTemporarily

    val bgColor by animateColorAsState(
        targetValue = when {
            card.isTappedCorrectly -> ActionSuccessGreen
            card.isTappedWrong -> CriticalRed
            showHidden -> Color(0xFF102A43)
            else -> Color.White
        },
        animationSpec = tween(300),
        label = "bgColor"
    )

    val contentColor = when {
        card.isTappedCorrectly || card.isTappedWrong -> Color.White
        showHidden -> OrbitalCyan
        else -> DeepSpaceNavy
    }
    val borderColor = when {
        card.isTappedCorrectly -> ActionSuccessGreen
        card.isTappedWrong -> CriticalRed
        showHidden -> Color(0xFF243B53)
        else -> ActionPrimaryBlue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(enabled = !card.isTappedCorrectly) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(3.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (card.isTappedCorrectly) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Correct",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${card.value}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            } else if (showHidden) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "❓",
                        fontSize = 28.sp
                    )
                    Text(
                        text = "TAP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OrbitalCyan
                    )
                }
            } else {
                Text(
                    text = "${card.value}",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor
                )
            }
        }
    }
}
