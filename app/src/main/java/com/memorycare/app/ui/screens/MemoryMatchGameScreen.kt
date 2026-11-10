package com.memorycare.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.model.AdaptiveTransition
import com.memorycare.app.model.DifficultyLevel
import com.memorycare.app.model.GameType
import com.memorycare.app.model.MemoryCard
import com.memorycare.app.model.UserRole
import com.memorycare.app.ui.components.AccessibleBigButton
import com.memorycare.app.ui.components.SpaceTopBar
import com.memorycare.app.ui.theme.ActionPrimaryBlue
import com.memorycare.app.ui.theme.ActionSuccessGreen
import com.memorycare.app.ui.theme.CriticalRed
import com.memorycare.app.ui.theme.DeepSpaceNavy
import com.memorycare.app.ui.theme.ElderlyBgLight
import com.memorycare.app.ui.theme.ElderlyTextPrimary
import com.memorycare.app.ui.theme.ElderlyTextSecondary
import com.memorycare.app.ui.theme.MatchedCardBorder
import com.memorycare.app.ui.theme.MatchedCardGreen
import com.memorycare.app.ui.theme.OrbitalCyan
import com.memorycare.app.viewmodel.AdaptiveAdaptationResult
import com.memorycare.app.viewmodel.MemoryCareViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Memory Match Game with FULLY AUTOMATED Adaptive Difficulty parameters:
 * - EASY: 3x4 grid (12 cards), 60 seconds limit, cards show longer (1500ms)
 * - MEDIUM: 4x4 grid (16 cards), 45 seconds limit, normal show time (900ms)
 * - HARD: 4x5 grid (20 cards), 30 seconds limit, fast show time (600ms)
 */
@Composable
fun MemoryMatchGameScreen(
    viewModel: MemoryCareViewModel,
    onNavigateBack: () -> Unit
) {
    val satelliteStatus by viewModel.satelliteStatus.collectAsState()
    val scope = rememberCoroutineScope()

    // Automatically load difficulty without manual selection
    val currentDifficulty = remember {
        viewModel.getAdaptiveDifficulty(GameType.MEMORY_MATCH)
    }

    val cards = remember { mutableStateListOf<MemoryCard>() }
    var firstFlippedIndex by remember { mutableStateOf<Int?>(null) }
    var isProcessingFlip by remember { mutableStateOf(false) }

    var flipCount by remember { mutableIntStateOf(0) }
    var matchedPairsCount by remember { mutableIntStateOf(0) }
    var totalPairsGoal by remember { mutableIntStateOf(6) }

    // Time limits based on difficulty
    val totalTimeLimitSeconds = when (currentDifficulty) {
        DifficultyLevel.EASY -> 60
        DifficultyLevel.MEDIUM -> 45
        DifficultyLevel.HARD -> 30
    }
    val flipDisplayDurationMs = when (currentDifficulty) {
        DifficultyLevel.EASY -> 1500L
        DifficultyLevel.MEDIUM -> 900L
        DifficultyLevel.HARD -> 600L
    }

    var remainingTimeSeconds by remember { mutableIntStateOf(totalTimeLimitSeconds) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isGameActive by remember { mutableStateOf(true) }
    var isTimeExpired by remember { mutableStateOf(false) }
    var showGameOverDialog by remember { mutableStateOf(false) }

    var accuracyPercent by remember { mutableIntStateOf(0) }
    var avgReactionTimeMs by remember { mutableLongStateOf(0L) }
    var lastCardFlipTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val reactionTimesList = remember { mutableStateListOf<Long>() }

    var adaptationResult by remember { mutableStateOf<AdaptiveAdaptationResult?>(null) }

    fun finishGame(allMatched: Boolean) {
        isGameActive = false
        val turns = (flipCount / 2).coerceAtLeast(totalPairsGoal)
        val matchedAccuracy = if (allMatched) {
            ((totalPairsGoal.toDouble() / turns) * 100).toInt().coerceIn(40, 100)
        } else {
            ((matchedPairsCount.toDouble() / totalPairsGoal) * 70).toInt().coerceIn(10, 70)
        }
        accuracyPercent = matchedAccuracy

        avgReactionTimeMs = if (reactionTimesList.isNotEmpty()) {
            reactionTimesList.average().toLong()
        } else 1200L

        // Record game result & compute automated difficulty adjustment
        val result = viewModel.recordGameResult(
            accuracyPercentage = accuracyPercent,
            totalFlips = flipCount,
            reactionTimeMs = avgReactionTimeMs,
            totalDurationSeconds = elapsedSeconds,
            gameType = GameType.MEMORY_MATCH,
            currentDifficulty = currentDifficulty
        )
        adaptationResult = result

        scope.launch {
            delay(500)
            showGameOverDialog = true
        }
    }

    fun resetGame() {
        val allPairs = listOf(
            Pair("🍎", "Apple"),
            Pair("☕", "Tea"),
            Pair("🌸", "Flower"),
            Pair("🚗", "Car"),
            Pair("🏠", "House"),
            Pair("🐕", "Dog"),
            Pair("🐱", "Cat"),
            Pair("⭐", "Star"),
            Pair("☀️", "Sun"),
            Pair("🌳", "Tree")
        )

        val pairCount = when (currentDifficulty) {
            DifficultyLevel.EASY -> 6   // 3x4 = 12 cards
            DifficultyLevel.MEDIUM -> 8 // 4x4 = 16 cards
            DifficultyLevel.HARD -> 10  // 4x5 = 20 cards
        }
        totalPairsGoal = pairCount

        val selectedPairs = allPairs.take(pairCount)
        val cardList = mutableListOf<MemoryCard>()
        var idCounter = 0
        selectedPairs.forEachIndexed { pairIndex, (emoji, name) ->
            cardList.add(
                MemoryCard(
                    id = idCounter++,
                    pairId = pairIndex,
                    emoji = emoji,
                    name = name,
                    isFlipped = false,
                    isMatched = false
                )
            )
            cardList.add(
                MemoryCard(
                    id = idCounter++,
                    pairId = pairIndex,
                    emoji = emoji,
                    name = name,
                    isFlipped = false,
                    isMatched = false
                )
            )
        }
        cardList.shuffle()

        cards.clear()
        cards.addAll(cardList)
        firstFlippedIndex = null
        isProcessingFlip = false
        flipCount = 0
        matchedPairsCount = 0
        remainingTimeSeconds = totalTimeLimitSeconds
        elapsedSeconds = 0
        isGameActive = true
        isTimeExpired = false
        showGameOverDialog = false
        reactionTimesList.clear()
        lastCardFlipTime = System.currentTimeMillis()
    }

    LaunchedEffect(Unit) {
        resetGame()
    }

    // Countdown Timer Loop
    LaunchedEffect(isGameActive) {
        while (isGameActive && remainingTimeSeconds > 0) {
            delay(1000)
            remainingTimeSeconds--
            elapsedSeconds++
            if (remainingTimeSeconds == 0) {
                isTimeExpired = true
                finishGame(allMatched = false)
            }
        }
    }

    fun onCardClick(index: Int) {
        if (isProcessingFlip || cards[index].isFlipped || cards[index].isMatched || !isGameActive) return

        val now = System.currentTimeMillis()
        val reactionTime = (now - lastCardFlipTime).coerceIn(400L, 8000L)
        reactionTimesList.add(reactionTime)
        lastCardFlipTime = now

        cards[index] = cards[index].copy(isFlipped = true)
        flipCount++

        if (firstFlippedIndex == null) {
            firstFlippedIndex = index
        } else {
            val firstIndex = firstFlippedIndex!!
            firstFlippedIndex = null

            if (cards[firstIndex].pairId == cards[index].pairId) {
                cards[firstIndex] = cards[firstIndex].copy(isMatched = true)
                cards[index] = cards[index].copy(isMatched = true)
                matchedPairsCount++

                if (matchedPairsCount == totalPairsGoal) {
                    finishGame(allMatched = true)
                }
            } else {
                isProcessingFlip = true
                scope.launch {
                    delay(flipDisplayDurationMs)
                    cards[firstIndex] = cards[firstIndex].copy(isFlipped = false)
                    cards[index] = cards[index].copy(isFlipped = false)
                    isProcessingFlip = false
                }
            }
        }
    }

    val gridColumns = when (currentDifficulty) {
        DifficultyLevel.EASY -> 3
        DifficultyLevel.MEDIUM -> 4
        DifficultyLevel.HARD -> 4
    }

    val matrixLabel = when (currentDifficulty) {
        DifficultyLevel.EASY -> "3x4 Matrix (12 Cards • 60s)"
        DifficultyLevel.MEDIUM -> "4x4 Matrix (16 Cards • 45s)"
        DifficultyLevel.HARD -> "4x5 Matrix (20 Cards • 30s)"
    }

    Scaffold(
        topBar = {
            SpaceTopBar(
                title = "Memory Match",
                subtitle = matrixLabel,
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
                .padding(horizontal = 14.dp, vertical = 6.dp),
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

                // Countdown Timer Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (remainingTimeSeconds <= 10) Color(0xFFFFEBEE) else Color.White,
                    border = BorderStroke(1.5.dp, if (remainingTimeSeconds <= 10) CriticalRed else ActionPrimaryBlue)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱️ Time Left: ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingTimeSeconds <= 10) CriticalRed else ElderlyTextSecondary
                        )
                        Text(
                            text = "${remainingTimeSeconds}s",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (remainingTimeSeconds <= 10) CriticalRed else ActionPrimaryBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Game Progress Stats Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, ActionPrimaryBlue),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⭐ ", fontSize = 14.sp)
                        Text(text = "$matchedPairsCount/$totalPairsGoal Pairs", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ActionSuccessGreen)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color(0xFFD0D7DE)),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔄 $flipCount Flips", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ElderlyTextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Grid Rows
            val chunkedCards = cards.chunked(gridColumns)
            chunkedCards.forEach { rowCards ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowCards.forEach { card ->
                        val index = cards.indexOf(card)
                        Box(modifier = Modifier.weight(1f)) {
                            AdaptiveMemoryGridCardItem(
                                card = card,
                                difficulty = currentDifficulty,
                                onClick = { onCardClick(index) }
                            )
                        }
                    }
                    if (rowCards.size < gridColumns) {
                        repeat(gridColumns - rowCards.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccessibleBigButton(
                    text = "Restart",
                    emoji = "🔄",
                    onClick = { resetGame() },
                    containerColor = Color(0xFF455A64),
                    modifier = Modifier.weight(1f),
                    minHeight = 52.dp
                )
                AccessibleBigButton(
                    text = "Done / Exit",
                    emoji = "🏠",
                    onClick = onNavigateBack,
                    containerColor = ActionPrimaryBlue,
                    modifier = Modifier.weight(1f),
                    minHeight = 52.dp
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
                        text = if (isTimeExpired) "⏱️ TIME'S UP!" else "🎉 FANTASTIC JOB! 🌟",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isTimeExpired) ActionPrimaryBlue else ActionSuccessGreen,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isTimeExpired) "Matched $matchedPairsCount of $totalPairsGoal pairs" else "All $totalPairsGoal memory pairs matched!",
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
                                text = "$accuracyPercent%",
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
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Automated Difficulty Adaptation Feedback Banner
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
                                text = "Telemetry logged to Caretaker Dashboard.",
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
                        emoji = "🎮",
                        onClick = { resetGame() },
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
private fun AdaptiveMemoryGridCardItem(
    card: MemoryCard,
    difficulty: DifficultyLevel,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 180f else 0f,
        animationSpec = tween(350),
        label = "cardFlipRotation"
    )

    val isFrontVisible = rotation > 90f
    val emojiSize = when (difficulty) {
        DifficultyLevel.EASY -> 36.sp
        DifficultyLevel.MEDIUM -> 30.sp
        DifficultyLevel.HARD -> 26.sp
    }
    val textSize = when (difficulty) {
        DifficultyLevel.EASY -> 14.sp
        DifficultyLevel.MEDIUM -> 12.sp
        DifficultyLevel.HARD -> 10.sp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(enabled = !card.isMatched && !card.isFlipped) { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                card.isMatched -> MatchedCardGreen
                card.isFlipped -> Color(0xFFE3F2FD)
                else -> Color(0xFF102A43)
            }
        ),
        border = BorderStroke(
            2.dp,
            when {
                card.isMatched -> MatchedCardBorder
                card.isFlipped -> ActionPrimaryBlue
                else -> Color(0xFF243B53)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (isFrontVisible) {
                        rotationY = 180f
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isFrontVisible) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = card.emoji,
                        fontSize = emojiSize
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = card.name,
                        fontSize = textSize,
                        fontWeight = FontWeight.Bold,
                        color = if (card.isMatched) ActionSuccessGreen else ActionPrimaryBlue,
                        maxLines = 1
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "❓",
                        fontSize = if (difficulty == DifficultyLevel.HARD) 20.sp else 26.sp
                    )
                    Text(
                        text = "FLIP",
                        fontSize = if (difficulty == DifficultyLevel.HARD) 10.sp else 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OrbitalCyan
                    )
                }
            }
        }
    }
}
