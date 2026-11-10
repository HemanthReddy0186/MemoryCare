package com.memorycare.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import com.memorycare.app.util.rememberVoiceAssistant
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.model.DifficultyLevel
import com.memorycare.app.model.GameType
import com.memorycare.app.model.SoundscapeChoice
import com.memorycare.app.model.SoundscapeItem
import com.memorycare.app.model.SoundscapeProfile
import com.memorycare.app.ui.components.AccessibleBigButton
import com.memorycare.app.ui.components.SpaceTopBar
import com.memorycare.app.ui.theme.ActionPrimaryBlue
import com.memorycare.app.ui.theme.ActionSuccessGreen
import com.memorycare.app.ui.theme.CriticalRed
import com.memorycare.app.ui.theme.DeepSpaceNavy
import com.memorycare.app.ui.theme.OrbitalCyan
import com.memorycare.app.ui.theme.SpaceNavyCard
import com.memorycare.app.util.SoundscapeSynthesizer
import com.memorycare.app.viewmodel.AdaptiveAdaptationResult
import com.memorycare.app.viewmodel.MemoryCareViewModel

/**
 * Screen 5: "Echoes of Life" (Nostalgia Soundscapes & Auditory Reminiscence Therapy)
 * Synthesizes comforting environmental acoustic memories (Temple Bells, Rain, Cooker Whistles, Trains, Sparrows)
 * to stimulate preserved temporal acoustic memory and alleviate agitation.
 */
@Composable
fun EchoesOfLifeGameScreen(
    viewModel: MemoryCareViewModel,
    onNavigateBack: () -> Unit
) {
    val difficultyMap by viewModel.difficultyPrefs.collectAsState()
    val currentDifficulty = difficultyMap[GameType.DAILY_ROUTINE] ?: DifficultyLevel.EASY
    val userRole by viewModel.userRole.collectAsState()
    val satelliteStatus by viewModel.satelliteStatus.collectAsState()

    val voiceAssistant = rememberVoiceAssistant()
    val haptics = LocalHapticFeedback.current
    val soundSynthesizer = remember { SoundscapeSynthesizer() }

    DisposableEffect(Unit) {
        onDispose {
            soundSynthesizer.stop()
            voiceAssistant.stop()
        }
    }

    // 4 soundscapes per session
    val catalog = remember(currentDifficulty) { getSoundscapeCatalog(currentDifficulty) }
    var currentSoundIndex by remember(currentDifficulty) { mutableIntStateOf(0) }
    val currentSound = catalog.getOrElse(currentSoundIndex) { catalog.first() }

    // State tracking
    var isSoundPlaying by remember { mutableStateOf(false) }
    var selectedChoice by remember(currentSoundIndex) { mutableStateOf<SoundscapeChoice?>(null) }
    var isCorrectMatch by remember(currentSoundIndex) { mutableStateOf(false) }
    var totalAttempts by remember { mutableIntStateOf(0) }
    var correctAttempts by remember { mutableIntStateOf(0) }
    val sessionStartTime = remember { mutableLongStateOf(System.currentTimeMillis()) }

    var isSessionCompleted by remember { mutableStateOf(false) }
    var adaptationResult by remember { mutableStateOf<AdaptiveAdaptationResult?>(null) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    // Pulsing acoustic wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "soundWave")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale"
    )

    // Play sound function
    fun playCurrentSound() {
        isSoundPlaying = true
        soundSynthesizer.playSoundscape(currentSound.soundProfile) {
            isSoundPlaying = false
        }
    }

    // On soundscape change, play the soundscape automatically
    LaunchedEffect(currentSoundIndex) {
        playCurrentSound()
        voiceAssistant.speak(currentSound.voicePrompt)
    }

    Scaffold(
        topBar = {
            SpaceTopBar(
                title = "Echoes of Life",
                subtitle = "Soundscapes & Reminiscence • ${currentDifficulty.label}",
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
            // Session Progress Bar (1 of 4, 2 of 4...)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceNavyCard),
                border = BorderStroke(1.dp, Color(0xFF00ACC1).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Soundscape ${currentSoundIndex + 1} of ${catalog.size}: ${currentSound.category}",
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
                                text = currentDifficulty.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(currentDifficulty.colorHex),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (currentSoundIndex + (if (isCorrectMatch) 1f else 0.2f)) / catalog.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF00ACC1),
                        trackColor = Color(0xFF1E293B)
                    )
                }
            }

            // Acoustic Waveform Player Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF071426)),
                border = BorderStroke(1.5.dp, if (isCorrectMatch) ActionSuccessGreen else Color(0xFF00ACC1).copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Concentric Animated Acoustic Waves
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val baseRadius = 55.dp.toPx()

                        if (isSoundPlaying) {
                            // Expanding outer ripples
                            drawCircle(
                                color = Color(0xFF00ACC1).copy(alpha = 0.18f),
                                radius = baseRadius * waveScale * 1.3f,
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = Color(0xFFFFD54F).copy(alpha = 0.25f),
                                radius = baseRadius * waveScale,
                                center = Offset(cx, cy)
                            )
                        }

                        // Core acoustic glowing disc
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF0284C7), Color(0xFF082F49)),
                                center = Offset(cx, cy),
                                radius = baseRadius
                            ),
                            radius = baseRadius,
                            center = Offset(cx, cy)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isCorrectMatch) currentSound.emoji else if (isSoundPlaying) "🎶" else "🎧",
                            fontSize = 42.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isSoundPlaying) "Playing Sound..." else "Tap Below to Listen",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSoundPlaying) Color(0xFFFFD54F) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Audio Replay Button
            AccessibleBigButton(
                text = if (isSoundPlaying) "Listening to Soundscape..." else "Play Sound Again 🔊",
                emoji = if (isSoundPlaying) "🎶" else "🔊",
                onClick = {
                    voiceAssistant.stop()
                    playCurrentSound()
                },
                containerColor = if (isSoundPlaying) Color(0xFF0E3A5A) else Color(0xFF1E293B),
                contentColor = if (isSoundPlaying) Color(0xFFFFD54F) else OrbitalCyan,
                minHeight = 44.dp,
                fontSize = 13.sp
            )

            // Prompt Title
            Text(
                text = "What comforting memory does this sound bring?",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            // Selectable Sound Picture Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentSound.choices.forEach { choice ->
                    val isSelected = selectedChoice?.id == choice.id
                    val isChoiceCorrect = choice.isCorrect

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isSelected && isChoiceCorrect -> Color(0xFF064E3B)
                            isSelected && !isChoiceCorrect -> Color(0xFF7F1D1D)
                            else -> SpaceNavyCard
                        },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = when {
                                isSelected && isChoiceCorrect -> ActionSuccessGreen
                                isSelected && !isChoiceCorrect -> Color(0xFFEF4444)
                                else -> Color(0xFF334155)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!isCorrectMatch) {
                                    totalAttempts++
                                    selectedChoice = choice

                                    if (choice.isCorrect) {
                                        correctAttempts++
                                        isCorrectMatch = true
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        voiceAssistant.speak(currentSound.reminiscenceStory)

                                        if (currentSoundIndex == catalog.size - 1) {
                                            // Session completed!
                                            val durationSeconds = ((System.currentTimeMillis() - sessionStartTime.longValue) / 1000L).toInt().coerceAtLeast(10)
                                            val accuracy = if (totalAttempts > 0) {
                                                ((correctAttempts.toFloat() / totalAttempts.toFloat()) * 100).toInt().coerceIn(40, 100)
                                            } else 100

                                            adaptationResult = viewModel.recordGameResult(
                                                accuracyPercentage = accuracy,
                                                totalFlips = totalAttempts,
                                                reactionTimeMs = (durationSeconds * 1000L / catalog.size),
                                                totalDurationSeconds = durationSeconds,
                                                gameType = GameType.DAILY_ROUTINE,
                                                currentDifficulty = currentDifficulty
                                            )
                                            isSessionCompleted = true
                                            showCompletionDialog = true
                                        }
                                    } else {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        voiceAssistant.speak("Listen closely to the gentle acoustic tone once more.")
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = choice.emoji, fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = choice.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = choice.subtitle,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            if (isSelected && isChoiceCorrect) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Correct",
                                    tint = ActionSuccessGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Expanding Reminiscence Story Card (Upon Match)
            AnimatedVisibility(
                visible = isCorrectMatch,
                enter = fadeIn(tween(400)) + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.85f)),
                    border = BorderStroke(1.5.dp, ActionSuccessGreen)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📖 Memory of Life", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD54F))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentSound.reminiscenceStory,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color.White
                        )

                        if (currentSoundIndex < catalog.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                            AccessibleBigButton(
                                text = "Next Soundscape (${currentSoundIndex + 2} of ${catalog.size}) ➔",
                                emoji = "🎶",
                                onClick = {
                                    voiceAssistant.stop()
                                    soundSynthesizer.stop()
                                    currentSoundIndex++
                                },
                                containerColor = ActionSuccessGreen,
                                contentColor = Color.White,
                                minHeight = 44.dp,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Session Completion & Adaptive Difficulty Dialog
    if (showCompletionDialog) {
        val result = adaptationResult
        AlertDialog(
            onDismissRequest = { /* Require action */ },
            shape = RoundedCornerShape(20.dp),
            containerColor = SpaceNavyCard,
            title = {
                Text(
                    text = "Acoustic Journey Complete! 🎶🔔",
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
                        text = "You listened to and identified all 4 comforting environmental memories—temple bells, courtyard rain, kitchen cooker, and countryside train.",
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
                    text = "Listen to More Memories 🎶",
                    emoji = "🔄",
                    onClick = {
                        showCompletionDialog = false
                        currentSoundIndex = 0
                        totalAttempts = 0
                        correctAttempts = 0
                        sessionStartTime.longValue = System.currentTimeMillis()
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
 * Predefined therapeutic soundscape catalog for Auditory Reminiscence Therapy.
 */
private fun getSoundscapeCatalog(difficulty: DifficultyLevel): List<SoundscapeItem> {
    return listOf(
        // Sound 1: Temple Brass Bell
        SoundscapeItem(
            id = "sound_temple_bell",
            title = "Sacred Temple Brass Bell",
            emoji = "🔔",
            category = "Morning / Devotional",
            soundProfile = SoundscapeProfile.TEMPLE_BELL,
            choices = listOf(
                SoundscapeChoice("c_bell", "Sacred Temple Brass Bell", "🔔", "Morning bell ringing at dawn prayers", isCorrect = true),
                SoundscapeChoice("c_rain", "Monsoon Courtyard Rain", "🌧️", "Raindrops tapping on the roof", isCorrect = false),
                SoundscapeChoice("c_train", "Countryside Steam Train", "🚂", "Railway journey on the tracks", isCorrect = false)
            ),
            reminiscenceStory = "That is the sacred temple bell! Walking to morning prayers at sunrise, smelling fresh jasmine garlands and incense.",
            voicePrompt = "Listen to this peaceful morning sound. What brings this comforting chime?"
        ),
        // Sound 2: Monsoon Courtyard Rain
        SoundscapeItem(
            id = "sound_rain",
            title = "Monsoon Courtyard Rain",
            emoji = "🌧️",
            category = "Nature & Weather",
            soundProfile = SoundscapeProfile.MONSOON_RAIN,
            choices = listOf(
                SoundscapeChoice("c_rain2", "Monsoon Courtyard Rain", "🌧️", "Raindrops tapping on the tin roof", isCorrect = true),
                SoundscapeChoice("c_cooker2", "Kitchen Pressure Cooker", "🫖", "Steam whistle on the stove", isCorrect = false),
                SoundscapeChoice("c_sparrows2", "Morning Garden Birds", "🐦", "Chirping sparrows on the trees", isCorrect = false)
            ),
            reminiscenceStory = "That is the soothing monsoon rain! Sitting on the covered verandah with a hot blanket and tea, watching the courtyard turn fresh and green.",
            voicePrompt = "Listen to the rhythmic patter. What soothing weather is this?"
        ),
        // Sound 3: Kitchen Pressure Cooker
        SoundscapeItem(
            id = "sound_cooker",
            title = "Kitchen Pressure Cooker",
            emoji = "🫖",
            category = "Home & Kitchen",
            soundProfile = SoundscapeProfile.PRESSURE_COOKER,
            choices = listOf(
                SoundscapeChoice("c_cooker3", "Kitchen Pressure Cooker", "🫖", "Steam whistle preparing fresh lunch", isCorrect = true),
                SoundscapeChoice("c_bell3", "Temple Brass Bell", "🔔", "Morning temple chime", isCorrect = false),
                SoundscapeChoice("c_train3", "Countryside Steam Train", "🚂", "Train horn passing far away", isCorrect = false)
            ),
            reminiscenceStory = "That is the kitchen pressure cooker whistle! The aroma of warm dal, turmeric, and cumin filling the house as lunch is lovingly prepared.",
            voicePrompt = "Listen to this familiar household sound. What is whistling on the stove?"
        ),
        // Sound 4: Countryside Steam Train
        SoundscapeItem(
            id = "sound_train",
            title = "Countryside Steam Train",
            emoji = "🚂",
            category = "Travel & Journeys",
            soundProfile = SoundscapeProfile.STEAM_TRAIN,
            choices = listOf(
                SoundscapeChoice("c_train4", "Countryside Steam Train", "🚂", "Railway journey horn and rhythmic chug", isCorrect = true),
                SoundscapeChoice("c_rain4", "Monsoon Courtyard Rain", "🌧️", "Raindrops tapping outside", isCorrect = false),
                SoundscapeChoice("c_bell4", "Sacred Temple Brass Bell", "🔔", "Morning temple prayer", isCorrect = false)
            ),
            reminiscenceStory = "That is the steam train! Nostalgic family journeys across the green fields, feeling the cool breeze through the train window.",
            voicePrompt = "Listen to this moving journey sound. What is traveling on the tracks?"
        )
    )
}
