package com.memorycare.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.model.DifficultyLevel
import com.memorycare.app.model.GameType
import com.memorycare.app.model.UserRole
import com.memorycare.app.ui.components.AccessibleBigButton
import com.memorycare.app.ui.components.SpaceTopBar
import com.memorycare.app.ui.theme.ActionPrimaryBlue
import com.memorycare.app.ui.theme.ActionSuccessGreen
import com.memorycare.app.ui.theme.ActionWarningOrange
import com.memorycare.app.ui.theme.ElderlyBgLight
import com.memorycare.app.ui.theme.ElderlyTextPrimary
import com.memorycare.app.ui.theme.ElderlyTextSecondary
import com.memorycare.app.util.ImageUtils
import com.memorycare.app.util.rememberVoiceAssistant
import com.memorycare.app.viewmodel.AdaptiveAdaptationResult
import com.memorycare.app.viewmodel.MemoryCareViewModel

/**
 * Screen: Family Face Match (Adaptive Memory Practice).
 *
 * Clinical memory reinforcement game that uses caretaker-configured family photos and questions.
 * Features:
 * - Dynamic Adaptive Hardness: Automatically adjusts difficulty based on patient recall accuracy.
 *   - Easy: 2 choices + automatic clue spoken & shown
 *   - Medium: 3 choices + clue on tap
 *   - Hard: 4 choices + pure facial recall
 * - Random character shuffling for every game session.
 * - Caretaker can configure any number of family members.
 */
@Composable
fun FaceNameAssociationGameScreen(
    viewModel: MemoryCareViewModel,
    onNavigateBack: () -> Unit
) {
    val voiceAssistant = rememberVoiceAssistant()
    val satelliteStatus by viewModel.satelliteStatus.collectAsState()
    val difficultyMap by viewModel.difficultyPrefs.collectAsState()
    val adaptiveDifficulty = difficultyMap[GameType.FACE_NAME_ASSOCIATION] ?: DifficultyLevel.EASY
    val allQuestions by viewModel.faceMatchQuestions.collectAsState()

    // Gentle fallback if caretaker has not added any family members yet
    if (allQuestions.isEmpty()) {
        Scaffold(
            topBar = {
                SpaceTopBar(
                    title = "Family Face Match",
                    subtitle = "Loved Ones Memory Game",
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFF3E0),
                    border = BorderStroke(2.dp, ActionWarningOrange),
                    modifier = Modifier.size(110.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "📸", fontSize = 54.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "No Family Photos Yet",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ElderlyTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your caretaker is setting up your family album.\nPlease check back soon!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = ElderlyTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                AccessibleBigButton(
                    text = "Back to Home",
                    emoji = "🏠",
                    onClick = onNavigateBack,
                    containerColor = ActionPrimaryBlue,
                    contentColor = Color.White,
                    minHeight = 56.dp,
                    fontSize = 18.sp
                )
            }
        }
        return
    }

    // Key to trigger reshuffling when playing again
    var gameSessionKey by remember { mutableIntStateOf(0) }

    // RANDOMLY SHUFFLE the characters for each game session
    val sessionQuestions = remember(allQuestions, gameSessionKey) {
        allQuestions.shuffled().take(8.coerceAtMost(allQuestions.size))
    }
    val totalRounds = sessionQuestions.size

    // Number of options based on ADAPTIVE HARDNESS
    val optionsCount = when (adaptiveDifficulty) {
        DifficultyLevel.EASY -> 2
        DifficultyLevel.MEDIUM -> 3
        DifficultyLevel.HARD -> 4
    }

    var roundIndex by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var selectedOptionName by remember { mutableStateOf<String?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isAnswerEvaluated by remember { mutableStateOf(false) }
    var showClueManually by remember { mutableStateOf(false) }
    var gameStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var roundStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var totalReactionTimeMs by remember { mutableLongStateOf(0L) }
    var adaptationResult by remember { mutableStateOf<AdaptiveAdaptationResult?>(null) }
    var showGameOverDialog by remember { mutableStateOf(false) }

    val currentQuestion = sessionQuestions.getOrNull(roundIndex) ?: sessionQuestions.first()

    // Randomly shuffle choices for current question (1 correct answer + random distractors)
    val currentChoices = remember(roundIndex, currentQuestion, adaptiveDifficulty) {
        val distractors = currentQuestion.wrongOptions.shuffled().take((optionsCount - 1).coerceAtMost(currentQuestion.wrongOptions.size))
        (distractors + currentQuestion.personName).shuffled()
    }

    // Load photo if available (memory safe and auto-oriented via ImageUtils)
    val currentBitmap = remember(currentQuestion.id, currentQuestion.imagePath) {
        ImageUtils.decodeRotatedBitmap(currentQuestion.imagePath, 600, 600)
    }

    // Announce target when round starts
    LaunchedEffect(roundIndex, gameSessionKey) {
        selectedOptionName = null
        feedbackMessage = null
        isAnswerEvaluated = false
        showClueManually = adaptiveDifficulty == DifficultyLevel.EASY
        roundStartTime = System.currentTimeMillis()

        if (voiceAssistant.isReady.value) {
            voiceAssistant.speak("Who is this lovely person? Take your time.")
        }
    }

    fun handleOptionSelected(selectedName: String) {
        if (isAnswerEvaluated) return
        selectedOptionName = selectedName
        isAnswerEvaluated = true

        val reactionTime = System.currentTimeMillis() - roundStartTime
        totalReactionTimeMs += reactionTime

        val isCorrect = selectedName.equals(currentQuestion.personName, ignoreCase = true)
        if (isCorrect) {
            correctCount++
            feedbackMessage = "That's right! ${currentQuestion.personName} is your ${currentQuestion.relationship}! 🌟"
            voiceAssistant.speak("That's right! ${currentQuestion.personName} is your ${currentQuestion.relationship}!")
        } else {
            feedbackMessage = "This is ${currentQuestion.personName}, your ${currentQuestion.relationship}."
            voiceAssistant.speak("This is ${currentQuestion.personName}, your ${currentQuestion.relationship}.")
        }
    }

    fun proceedToNextRound() {
        if (roundIndex + 1 < totalRounds) {
            roundIndex++
        } else {
            // Game Complete - record results with adaptive progression
            val totalDurationSeconds = ((System.currentTimeMillis() - gameStartTime) / 1000).toInt().coerceAtLeast(10)
            val avgReactionTimeMs = if (totalRounds > 0) totalReactionTimeMs / totalRounds else 1200L
            val accuracy = ((correctCount.toFloat() / totalRounds) * 100).toInt()

            val result = viewModel.recordGameResult(
                accuracyPercentage = accuracy,
                totalFlips = 0,
                reactionTimeMs = avgReactionTimeMs,
                totalDurationSeconds = totalDurationSeconds,
                gameType = GameType.FACE_NAME_ASSOCIATION,
                currentDifficulty = adaptiveDifficulty
            )
            adaptationResult = result
            showGameOverDialog = true
        }
    }

    Scaffold(
        topBar = {
            SpaceTopBar(
                title = "Family Face Match",
                subtitle = "Loved Ones Memory Game",
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
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Adaptive Difficulty Badge & Round Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(adaptiveDifficulty.colorHex).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(adaptiveDifficulty.colorHex))
                ) {
                    Text(
                        text = "🧠 ADAPTIVE • ${adaptiveDifficulty.label}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(adaptiveDifficulty.colorHex),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = "Question ${roundIndex + 1} of $totalRounds",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ElderlyTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (roundIndex.toFloat()) / totalRounds },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = ActionPrimaryBlue,
                trackColor = Color(0xFFCFD8DC)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Portrait Card of the Loved One
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, Color(adaptiveDifficulty.colorHex)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Portrait Frame (Photo or Avatar Emoji)
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(Color(adaptiveDifficulty.colorHex).copy(alpha = 0.12f))
                            .border(3.dp, Color(adaptiveDifficulty.colorHex), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentBitmap != null) {
                            Image(
                                bitmap = currentBitmap.asImageBitmap(),
                                contentDescription = currentQuestion.personName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = currentQuestion.avatarEmoji.ifEmpty { "👤" },
                                fontSize = 68.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Who is this lovely person?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElderlyTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Relationship category tag
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEDE7F6)
                    ) {
                        Text(
                            text = "❤️ Loved One in Your Life",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A148C),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Memory Clue Box (Adaptive: auto-shown on Easy, tap-to-reveal on Medium/Hard)
                    if (currentQuestion.memoryClue.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        if (showClueManually || adaptiveDifficulty == DifficultyLevel.EASY) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFF9C4),
                                border = BorderStroke(1.dp, Color(0xFFFBC02D)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "💡", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                                    Text(
                                        text = currentQuestion.memoryClue,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF5D4037),
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        } else {
                            AccessibleBigButton(
                                text = "Tap for Memory Clue",
                                emoji = "💡",
                                onClick = {
                                    showClueManually = true
                                    voiceAssistant.speak(currentQuestion.memoryClue)
                                },
                                containerColor = Color(0xFFFFF59D),
                                contentColor = Color(0xFF5D4037),
                                minHeight = 44.dp,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Voice Audio Clue Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE3F2FD),
                            border = BorderStroke(1.dp, ActionPrimaryBlue),
                            modifier = Modifier.clickable {
                                voiceAssistant.speak(currentQuestion.memoryClue)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🔊", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Hear Clue Aloud",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0D47A1)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Options List (Choices count adapts dynamically: 2 on Easy, 3 on Medium, 4 on Hard)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                currentChoices.forEach { nameOption ->
                    val isSelected = selectedOptionName == nameOption
                    val isTarget = nameOption.equals(currentQuestion.personName, ignoreCase = true)

                    val containerColor by animateColorAsState(
                        targetValue = when {
                            isAnswerEvaluated && isTarget -> Color(0xFFE8F5E9)
                            isAnswerEvaluated && isSelected && !isTarget -> Color(0xFFFFF3E0)
                            else -> Color.White
                        },
                        animationSpec = tween(400),
                        label = "optionBg"
                    )

                    val borderColor by animateColorAsState(
                        targetValue = when {
                            isAnswerEvaluated && isTarget -> ActionSuccessGreen
                            isAnswerEvaluated && isSelected && !isTarget -> ActionWarningOrange
                            else -> Color(0xFFCFD8DC)
                        },
                        animationSpec = tween(400),
                        label = "optionBorder"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isAnswerEvaluated) {
                                handleOptionSelected(nameOption)
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        border = BorderStroke(2.dp, borderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isAnswerEvaluated && isTarget) ActionSuccessGreen.copy(alpha = 0.15f) else Color(0xFFECEFF1),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (isAnswerEvaluated && isTarget) "✅" else "👤",
                                            fontSize = 20.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Text(
                                    text = nameOption,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ElderlyTextPrimary
                                )
                            }

                            if (isAnswerEvaluated && isTarget) {
                                Text(
                                    text = "Correct! ✅",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ActionSuccessGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Post-Selection Feedback & Next Question Button
            AnimatedVisibility(visible = isAnswerEvaluated) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    feedbackMessage?.let { msg ->
                        val isCorrect = selectedOptionName.equals(currentQuestion.personName, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            border = BorderStroke(
                                1.5.dp,
                                if (isCorrect) ActionSuccessGreen else ActionWarningOrange
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = msg,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElderlyTextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Relationship: ${currentQuestion.relationship}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ElderlyTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AccessibleBigButton(
                        text = if (roundIndex + 1 < totalRounds) "NEXT PERSON ➔" else "FINISH GAME 🏆",
                        emoji = if (roundIndex + 1 < totalRounds) "➡️" else "🏆",
                        onClick = { proceedToNextRound() },
                        containerColor = ActionPrimaryBlue,
                        contentColor = Color.White,
                        minHeight = 56.dp,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }

    // Celebratory Game Completion Dialog with Dynamic Adaptive Progression
    if (showGameOverDialog) {
        AlertDialog(
            onDismissRequest = { /* Require action */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎉", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Memory Practice Complete!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0D47A1)
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val finalAccuracy = if (totalRounds > 0) ((correctCount.toFloat() / totalRounds) * 100).toInt() else 100

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFE3F2FD),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$finalAccuracy%",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = ActionPrimaryBlue
                            )
                            Text(
                                text = "Family Recognition Accuracy",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElderlyTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$correctCount of $totalRounds loved ones recognized correctly",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ActionSuccessGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Adaptive Progression Insight
                    adaptationResult?.let { result ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFF8E1),
                            border = BorderStroke(1.dp, Color(0xFFFFB300)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = result.transition.emoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Adaptive Hardness Progress:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                    Text(
                                        text = result.transition.message,
                                        fontSize = 13.sp,
                                        color = ElderlyTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Next session level: ${result.newDifficulty.label}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(result.newDifficulty.colorHex)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                AccessibleBigButton(
                    text = "Back to Home",
                    emoji = "🏠",
                    onClick = {
                        showGameOverDialog = false
                        onNavigateBack()
                    },
                    containerColor = ActionPrimaryBlue,
                    contentColor = Color.White,
                    minHeight = 48.dp,
                    fontSize = 16.sp
                )
            },
            dismissButton = {
                AccessibleBigButton(
                    text = "Play Again (Reshuffle)",
                    emoji = "🔄",
                    onClick = {
                        showGameOverDialog = false
                        roundIndex = 0
                        correctCount = 0
                        selectedOptionName = null
                        feedbackMessage = null
                        isAnswerEvaluated = false
                        gameStartTime = System.currentTimeMillis()
                        roundStartTime = System.currentTimeMillis()
                        totalReactionTimeMs = 0L
                        // Increment session key to trigger a complete random reshuffle of characters
                        gameSessionKey++
                    },
                    containerColor = Color(0xFFECEFF1),
                    contentColor = ElderlyTextPrimary,
                    minHeight = 48.dp,
                    fontSize = 15.sp
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
