package com.memorycare.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

data class SortItem(
    val id: Int,
    val name: String,
    val emoji: String,
    val categoryId: Int // 0, 1, or 2
)

data class CategoryDefinition(
    val id: Int,
    val title: String,
    val emoji: String,
    val containerColor: Color,
    val borderColor: Color
)

/**
 * Category Sorting Game with FULLY AUTOMATED Adaptive Difficulty parameters:
 * - EASY: 6 items, 2 categories, show category labels
 * - MEDIUM: 8 items, 2 categories, hide labels after 2 seconds
 * - HARD: 10 items, 3 categories (Fruits, Vehicles, Animals), no labels shown
 */
@Composable
fun CategorySortingGameScreen(
    viewModel: MemoryCareViewModel,
    onNavigateBack: () -> Unit
) {
    val satelliteStatus by viewModel.satelliteStatus.collectAsState()
    val scope = rememberCoroutineScope()

    // Automatically load difficulty without manual selection
    val currentDifficulty = remember {
        viewModel.getAdaptiveDifficulty(GameType.CATEGORY_SORTING)
    }

    val categories = remember(currentDifficulty) {
        when (currentDifficulty) {
            DifficultyLevel.EASY, DifficultyLevel.MEDIUM -> listOf(
                CategoryDefinition(0, "FRUITS", "🍎", Color(0xFFE8F5E9), ActionSuccessGreen),
                CategoryDefinition(1, "VEHICLES", "🚗", Color(0xFFE3F2FD), ActionPrimaryBlue)
            )
            DifficultyLevel.HARD -> listOf(
                CategoryDefinition(0, "FRUITS", "🍎", Color(0xFFE8F5E9), ActionSuccessGreen),
                CategoryDefinition(1, "VEHICLES", "🚗", Color(0xFFE3F2FD), ActionPrimaryBlue),
                CategoryDefinition(2, "ANIMALS", "🐶", Color(0xFFFFF3E0), Color(0xFFE65100))
            )
        }
    }

    // Label hiding logic for Medium
    var areLabelsHidden by remember { mutableStateOf(currentDifficulty == DifficultyLevel.HARD) }
    var labelHideCountdown by remember { mutableIntStateOf(if (currentDifficulty == DifficultyLevel.MEDIUM) 2 else 0) }

    val itemsQueue = remember { mutableStateListOf<SortItem>() }
    var currentItemIndex by remember { mutableIntStateOf(0) }

    var totalAttempts by remember { mutableIntStateOf(0) }
    var errorCount by remember { mutableIntStateOf(0) }
    var gameDurationSeconds by remember { mutableIntStateOf(0) }
    var isGameActive by remember { mutableStateOf(true) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var feedbackHint by remember { mutableStateOf<String?>(null) }
    var isProcessingChoice by remember { mutableStateOf(false) }

    var lastActionTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val reactionTimesList = remember { mutableStateListOf<Long>() }

    var calculatedAccuracy by remember { mutableIntStateOf(100) }
    var avgReactionTimeMs by remember { mutableLongStateOf(0L) }

    var adaptationResult by remember { mutableStateOf<AdaptiveAdaptationResult?>(null) }

    fun initGame() {
        val allFruits = listOf(
            SortItem(1, "Apple", "🍎", 0),
            SortItem(2, "Banana", "🍌", 0),
            SortItem(3, "Orange", "🍊", 0),
            SortItem(4, "Strawberry", "🍓", 0)
        )

        val allVehicles = listOf(
            SortItem(5, "Car", "🚗", 1),
            SortItem(6, "Bus", "🚌", 1),
            SortItem(7, "Airplane", "✈️", 1),
            SortItem(8, "Train", "🚆", 1)
        )

        val allAnimals = listOf(
            SortItem(9, "Dog", "🐶", 2),
            SortItem(10, "Cat", "🐱", 2),
            SortItem(11, "Elephant", "🐘", 2),
            SortItem(12, "Bird", "🐦", 2)
        )

        val pool = when (currentDifficulty) {
            DifficultyLevel.EASY -> {
                // 6 items, 2 categories
                (allFruits.shuffled().take(3) + allVehicles.shuffled().take(3)).shuffled()
            }
            DifficultyLevel.MEDIUM -> {
                // 8 items, 2 categories
                (allFruits.shuffled().take(4) + allVehicles.shuffled().take(4)).shuffled()
            }
            DifficultyLevel.HARD -> {
                // 10 items, 3 categories
                (allFruits.shuffled().take(4) + allVehicles.shuffled().take(3) + allAnimals.shuffled().take(3)).shuffled()
            }
        }

        itemsQueue.clear()
        itemsQueue.addAll(pool)
        currentItemIndex = 0

        totalAttempts = 0
        errorCount = 0
        gameDurationSeconds = 0
        labelHideCountdown = if (currentDifficulty == DifficultyLevel.MEDIUM) 2 else 0
        areLabelsHidden = currentDifficulty == DifficultyLevel.HARD
        isGameActive = true
        showGameOverDialog = false
        feedbackHint = null
        isProcessingChoice = false
        reactionTimesList.clear()
        lastActionTimestamp = System.currentTimeMillis()
    }

    LaunchedEffect(Unit) {
        initGame()
    }

    // Label hide countdown for Medium
    LaunchedEffect(isGameActive, labelHideCountdown) {
        if (currentDifficulty == DifficultyLevel.MEDIUM && labelHideCountdown > 0 && isGameActive) {
            delay(1000)
            labelHideCountdown--
            if (labelHideCountdown == 0) {
                areLabelsHidden = true
            }
        }
    }

    // Timer loop
    LaunchedEffect(isGameActive) {
        while (isGameActive) {
            delay(1000)
            gameDurationSeconds++
        }
    }

    val currentItem = itemsQueue.getOrNull(currentItemIndex)

    fun onCategorySelected(category: CategoryDefinition) {
        if (!isGameActive || currentItem == null || isProcessingChoice) return

        val now = System.currentTimeMillis()
        val reactionTime = (now - lastActionTimestamp).coerceIn(400L, 8000L)
        reactionTimesList.add(reactionTime)
        lastActionTimestamp = now

        totalAttempts++

        if (currentItem.categoryId == category.id) {
            feedbackHint = "🌟 Correct! ${currentItem.name} belongs to ${category.title}!"
            isProcessingChoice = true

            scope.launch {
                delay(600)
                if (currentItemIndex + 1 < itemsQueue.size) {
                    currentItemIndex++
                    feedbackHint = null
                    isProcessingChoice = false
                } else {
                    isGameActive = false
                    val totalItems = itemsQueue.size
                    calculatedAccuracy = if (totalAttempts > 0) {
                        ((totalItems.toDouble() / totalAttempts) * 100).toInt().coerceIn(30, 100)
                    } else 100

                    avgReactionTimeMs = if (reactionTimesList.isNotEmpty()) {
                        reactionTimesList.average().toLong()
                    } else 1150L

                    val result = viewModel.recordGameResult(
                        accuracyPercentage = calculatedAccuracy,
                        totalFlips = totalAttempts,
                        reactionTimeMs = avgReactionTimeMs,
                        totalDurationSeconds = gameDurationSeconds,
                        gameType = GameType.CATEGORY_SORTING,
                        currentDifficulty = currentDifficulty
                    )
                    adaptationResult = result

                    delay(400)
                    showGameOverDialog = true
                    isProcessingChoice = false
                }
            }
        } else {
            errorCount++
            val correctCategory = categories.first { it.id == currentItem.categoryId }
            feedbackHint = "💡 ${currentItem.name} ${currentItem.emoji} belongs to ${correctCategory.title} ${correctCategory.emoji}."
        }
    }

    val difficultySubtitle = when (currentDifficulty) {
        DifficultyLevel.EASY -> "6 Items • 2 Categories (Labels Shown)"
        DifficultyLevel.MEDIUM -> "8 Items • 2 Categories (2s Label Preview)"
        DifficultyLevel.HARD -> "10 Items • 3 Categories (No Labels)"
    }

    Scaffold(
        topBar = {
            SpaceTopBar(
                title = "Category Sorting",
                subtitle = difficultySubtitle,
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
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, ActionPrimaryBlue),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Item ${currentItemIndex + 1} of ${itemsQueue.size}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ActionPrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "⏱️ ${gameDurationSeconds}s",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElderlyTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Current Active Item Card (Prominent & Elderly Accessible)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 210.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.5.dp, Color(0xFFD0D7DE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentItem != null) {
                        AnimatedContent(
                            targetState = currentItem,
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                            label = "itemAnimation"
                        ) { item ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = item.emoji,
                                    fontSize = 72.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.name,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ElderlyTextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Which basket does this belong to?",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ElderlyTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Feedback / Guidance Hint
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (feedbackHint?.contains("Correct") == true) Color(0xFFE8F5E9) else if (feedbackHint != null) Color(0xFFFFEBEE) else Color.Transparent,
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (feedbackHint != null) {
                        Text(
                            text = feedbackHint!!,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (feedbackHint?.contains("Correct") == true) ActionSuccessGreen else CriticalRed,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Buttons at the bottom (2 buttons for Easy/Medium, 3 for Hard)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categories.forEach { category ->
                    CategoryTargetButton(
                        category = category,
                        hideLabel = areLabelsHidden,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategorySelected(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Navigation
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
                        text = "🎉 SPLENDID SORTING! 🌟",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ActionSuccessGreen,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "All items classified into proper categories!",
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
                                text = "Cognitive Sorting Accuracy",
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
                                text = "Sorted ${itemsQueue.size} items in ${gameDurationSeconds}s with $errorCount errors",
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
                                text = "Session telemetry synced with Caretaker Dashboard.",
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
                        text = "Sort Again",
                        emoji = "🧺",
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
private fun CategoryTargetButton(
    category: CategoryDefinition,
    hideLabel: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(88.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = category.containerColor),
        border = BorderStroke(2.5.dp, category.borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = category.emoji,
                fontSize = if (hideLabel) 36.sp else 30.sp
            )
            if (!hideLabel) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = category.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = category.borderColor,
                    maxLines = 1
                )
            }
        }
    }
}
