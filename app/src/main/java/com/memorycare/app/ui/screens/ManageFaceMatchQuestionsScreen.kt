package com.memorycare.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.model.DifficultyLevel
import com.memorycare.app.model.FaceMatchQuestion
import com.memorycare.app.model.GameType
import com.memorycare.app.model.UserRole
import com.memorycare.app.ui.components.AccessibleBigButton
import com.memorycare.app.ui.components.SpaceTopBar
import com.memorycare.app.ui.theme.ActionPrimaryBlue
import com.memorycare.app.ui.theme.ActionSuccessGreen
import com.memorycare.app.ui.theme.ActionWarningOrange
import com.memorycare.app.ui.theme.CriticalRed
import com.memorycare.app.ui.theme.OrbitalCyan
import com.memorycare.app.util.ImageUtils
import com.memorycare.app.viewmodel.MemoryCareViewModel
import java.util.UUID

private val DarkDashboardBg = Color(0xFF0F172A)
private val DarkCardSurface = Color(0xFF1E293B)
private val DarkCardBorder = Color(0xFF334155)
private val TextWhitePrimary = Color(0xFFF8FAFC)
private val TextMutedSecondary = Color(0xFF94A3B8)

/**
 * Screen: Manage Family Members & Face Match Questions (Caretaker Setup Screen).
 *
 * Adaptive Hardness Architecture:
 * - Caretakers can add as many family members as they want (no rigid difficulty bucket thresholds).
 * - For each family member: photo (from gallery/camera), name, relationship, 3 wrong choices, and memory clue.
 * - During the game, characters are randomly shuffled each session.
 * - Adaptive hardness automatically adjusts choices and clues based on patient performance.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageFaceMatchQuestionsScreen(
    viewModel: MemoryCareViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val satelliteStatus by viewModel.satelliteStatus.collectAsState()
    val allQuestions by viewModel.faceMatchQuestions.collectAsState()
    val difficultyMap by viewModel.difficultyPrefs.collectAsState()
    val adaptiveDifficulty = difficultyMap[GameType.FACE_NAME_ASSOCIATION] ?: DifficultyLevel.EASY

    var searchQuery by remember { mutableStateOf("") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingQuestion by remember { mutableStateOf<FaceMatchQuestion?>(null) }
    var deletingQuestion by remember { mutableStateOf<FaceMatchQuestion?>(null) }

    val filteredQuestions = remember(allQuestions, searchQuery) {
        if (searchQuery.isBlank()) {
            allQuestions
        } else {
            allQuestions.filter {
                it.personName.contains(searchQuery, ignoreCase = true) ||
                it.relationship.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            SpaceTopBar(
                title = "Family Members & Face Match",
                subtitle = "Adaptive Memory Practice Setup",
                userRole = UserRole.CARETAKER,
                satelliteStatus = satelliteStatus,
                onBackClick = onNavigateBack,
                onSyncClick = { viewModel.triggerSatelliteSync() }
            )
        },
        containerColor = DarkDashboardBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))

                    // =========================================================================
                    // 1. ADAPTIVE HARDNESS & STATUS OVERVIEW
                    // =========================================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Total Family Members
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkCardSurface,
                            border = BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "FAMILY POOL",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMutedSecondary
                                    )
                                    Text(text = "👥", fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${allQuestions.size}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OrbitalCyan
                                )
                                Text(
                                    text = "Loved ones configured",
                                    fontSize = 12.sp,
                                    color = TextMutedSecondary
                                )
                            }
                        }

                        // Card 2: Active Adaptive Hardness
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkCardSurface,
                            border = BorderStroke(1.dp, Color(adaptiveDifficulty.colorHex).copy(alpha = 0.6f)),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ADAPTIVE HARDNESS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMutedSecondary
                                    )
                                    Text(text = "🧠", fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(adaptiveDifficulty.colorHex).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(adaptiveDifficulty.colorHex))
                                    ) {
                                        Text(
                                            text = adaptiveDifficulty.label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(adaptiveDifficulty.colorHex),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (adaptiveDifficulty) {
                                        DifficultyLevel.EASY -> "2 Choices • Clues Shown"
                                        DifficultyLevel.MEDIUM -> "3 Choices • Clues on Tap"
                                        DifficultyLevel.HARD -> "4 Choices • Pure Recall"
                                    },
                                    fontSize = 11.sp,
                                    color = TextMutedSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Adaptive Informational Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F2338),
                        border = BorderStroke(1.dp, OrbitalCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "✨", fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                            Column {
                                Text(
                                    text = "Dynamic Adaptive Hardness Active",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrbitalCyan
                                )
                                Text(
                                    text = "You can add as many family members as you want! Each game randomly shuffles the characters and automatically adjusts choices based on patient accuracy.",
                                    fontSize = 12.sp,
                                    color = TextWhitePrimary,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // =========================================================================
                    // 2. SEARCH & ADD FAMILY MEMBER BUTTON
                    // =========================================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Family Members (${allQuestions.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhitePrimary
                        )

                        Button(
                            onClick = {
                                editingQuestion = null
                                showAddEditDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ActionPrimaryBlue),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Add Member", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name or relation (e.g., Sarah, Son)", fontSize = 13.sp, color = TextMutedSecondary) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextMutedSecondary)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // =========================================================================
                // 3. FAMILY MEMBERS QUESTION LIST
                // =========================================================================
                if (filteredQuestions.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkCardSurface,
                            border = BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "📸", fontSize = 42.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No matching family members" else "No family members added yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Add family members with photos from your gallery. The patient will practice recognizing them in the Face Match game.",
                                    fontSize = 13.sp,
                                    color = TextMutedSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        editingQuestion = null
                                        showAddEditDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ActionPrimaryBlue),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(text = "➕ Add First Family Member", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(filteredQuestions, key = { it.id }) { question ->
                        FamilyMemberItemCard(
                            question = question,
                            onEdit = {
                                editingQuestion = question
                                showAddEditDialog = true
                            },
                            onDelete = {
                                deletingQuestion = question
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    val currentProfile by viewModel.currentProfile.collectAsState()
    val activeProfileId = currentProfile?.id ?: "profile_default_patient"

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditFamilyMemberDialog(
            existingQuestion = editingQuestion,
            profileId = activeProfileId,
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedQuestion ->
                if (editingQuestion != null) {
                    viewModel.updateFaceMatchQuestion(updatedQuestion)
                    Toast.makeText(context, "Updated ${updatedQuestion.personName}!", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addFaceMatchQuestion(updatedQuestion)
                    Toast.makeText(context, "Added ${updatedQuestion.personName} to family pool!", Toast.LENGTH_SHORT).show()
                }
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    deletingQuestion?.let { question ->
        AlertDialog(
            onDismissRequest = { deletingQuestion = null },
            title = {
                Text(
                    text = "Delete Family Member?",
                    fontWeight = FontWeight.Bold,
                    color = CriticalRed
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${question.personName}\" (${question.relationship}) from the family pool?",
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFaceMatchQuestion(question.id)
                        Toast.makeText(context, "Removed ${question.personName}", Toast.LENGTH_SHORT).show()
                        deletingQuestion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CriticalRed)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingQuestion = null }) {
                    Text("Cancel")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Question Item Card displaying family member details, photo thumbnail, and action buttons.
 */
@Composable
private fun FamilyMemberItemCard(
    question: FaceMatchQuestion,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = remember(question.id, question.imagePath) {
        ImageUtils.decodeRotatedBitmap(question.imagePath, 160, 160)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Photo Preview (from internal file or fallback emoji)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(2.dp, if (question.imagePath != null) ActionSuccessGreen else OrbitalCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = question.personName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(text = question.avatarEmoji.ifEmpty { "👤" }, fontSize = 28.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = question.personName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhitePrimary
                            )
                            if (question.imagePath != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0C2915),
                                    border = BorderStroke(1.dp, ActionSuccessGreen)
                                ) {
                                    Text(
                                        text = "📸 PHOTO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ActionSuccessGreen,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = question.relationship,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = OrbitalCyan
                        )
                    }
                }

                // Edit and Delete Buttons
                Row {
                    Button(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(text = "✏️ Edit", fontSize = 11.sp, color = TextWhitePrimary)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = onDelete,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(text = "🗑️", fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Wrong choices preview
            Text(
                text = "Wrong Choices (for 3 or 4-option rounds):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMutedSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                question.wrongOptions.forEach { wrong ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(
                            text = "❌ $wrong",
                            fontSize = 11.sp,
                            color = TextMutedSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (question.memoryClue.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "💡 Clue: \"${question.memoryClue}\"",
                    fontSize = 11.sp,
                    color = Color(0xFFFDD835),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Dialog for adding or editing a family member in the adaptive pool.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEditFamilyMemberDialog(
    existingQuestion: FaceMatchQuestion?,
    profileId: String = "default",
    onDismiss: () -> Unit,
    onSave: (FaceMatchQuestion) -> Unit
) {
    val context = LocalContext.current

    var personName by remember { mutableStateOf(existingQuestion?.personName ?: "") }
    var relationship by remember { mutableStateOf(existingQuestion?.relationship ?: "") }
    var wrongOption1 by remember { mutableStateOf(existingQuestion?.wrongOptions?.getOrNull(0) ?: "") }
    var wrongOption2 by remember { mutableStateOf(existingQuestion?.wrongOptions?.getOrNull(1) ?: "") }
    var wrongOption3 by remember { mutableStateOf(existingQuestion?.wrongOptions?.getOrNull(2) ?: "") }
    var memoryClue by remember { mutableStateOf(existingQuestion?.memoryClue ?: "") }
    var savedImagePath by remember { mutableStateOf(existingQuestion?.imagePath) }
    var avatarEmoji by remember { mutableStateOf(existingQuestion?.avatarEmoji ?: "👧") }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            val path = ImageUtils.saveUriToInternalStorage(context, sourceUri, profileId)
            if (path != null) {
                savedImagePath = path
                Toast.makeText(context, "Photo loaded from gallery!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to load photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera Capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        bmp?.let { capturedBitmap ->
            val path = ImageUtils.saveBitmapToInternalStorage(context, capturedBitmap, profileId)
            if (path != null) {
                savedImagePath = path
                Toast.makeText(context, "Photo captured successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val previewBitmap = remember(savedImagePath) {
        ImageUtils.decodeRotatedBitmap(savedImagePath, 300, 300)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingQuestion != null) "Edit Family Member & Photo" else "Add Family Member",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Photo Preview & Picker Section
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .border(
                                    2.5.dp,
                                    if (savedImagePath != null) ActionSuccessGreen else ActionPrimaryBlue,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = "Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(text = avatarEmoji, fontSize = 44.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (savedImagePath != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0C2915),
                                border = BorderStroke(1.dp, ActionSuccessGreen)
                            ) {
                                Text(
                                    text = "✅ Custom Family Photo Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ActionSuccessGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = ActionPrimaryBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Text(text = "🖼️ Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = { cameraLauncher.launch(null) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Text(text = "📸 Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhitePrimary)
                            }
                        }

                        if (savedImagePath != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = { savedImagePath = null },
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(text = "Remove photo & use emoji", fontSize = 11.sp, color = CriticalRed)
                            }
                        }
                    }
                }

                // If no photo, choose Avatar Emoji
                if (savedImagePath == null) {
                    Text(text = "Or choose avatar icon:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("👧", "👦", "👨‍💼", "👩", "👵", "👴", "👩‍⚕️", "🩺", "👶").forEach { emoji ->
                            Surface(
                                shape = CircleShape,
                                color = if (avatarEmoji == emoji) ActionPrimaryBlue.copy(alpha = 0.2f) else Color(0xFFF0F0F0),
                                border = BorderStroke(
                                    if (avatarEmoji == emoji) 2.dp else 1.dp,
                                    if (avatarEmoji == emoji) ActionPrimaryBlue else Color(0xFFCCCCCC)
                                ),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable { avatarEmoji = emoji }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }

                // Person Name (Correct answer)
                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Person's Name (Correct Answer)") },
                    placeholder = { Text("e.g. Sarah, David, Grandma Mary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Relationship
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship to Patient") },
                    placeholder = { Text("e.g. Granddaughter, Eldest Son, Sister") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 3 Wrong Options (Distractor names)
                Text(
                    text = "3 Wrong Options (Used for Medium & Hard rounds):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                OutlinedTextField(
                    value = wrongOption1,
                    onValueChange = { wrongOption1 = it },
                    label = { Text("Wrong Option 1") },
                    placeholder = { Text("e.g. Emily") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = wrongOption2,
                    onValueChange = { wrongOption2 = it },
                    label = { Text("Wrong Option 2") },
                    placeholder = { Text("e.g. Jessica") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = wrongOption3,
                    onValueChange = { wrongOption3 = it },
                    label = { Text("Wrong Option 3") },
                    placeholder = { Text("e.g. Chloe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Memory Clue
                OutlinedTextField(
                    value = memoryClue,
                    onValueChange = { memoryClue = it },
                    label = { Text("Memory Clue / Hint") },
                    placeholder = { Text("e.g. Loves painting watercolors with you") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (personName.isBlank() || relationship.isBlank()) {
                        Toast.makeText(context, "Please enter Person Name and Relationship", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val wrongList = listOf(
                        wrongOption1.ifBlank { "Aunt Emily" },
                        wrongOption2.ifBlank { "Neighbor Peter" },
                        wrongOption3.ifBlank { "Cousin Clara" }
                    )
                    val question = FaceMatchQuestion(
                        id = existingQuestion?.id ?: UUID.randomUUID().toString(),
                        personName = personName.trim(),
                        relationship = relationship.trim(),
                        difficulty = existingQuestion?.difficulty ?: DifficultyLevel.EASY,
                        wrongOptions = wrongList,
                        imagePath = savedImagePath,
                        avatarEmoji = avatarEmoji,
                        memoryClue = memoryClue.trim(),
                        dateCreated = existingQuestion?.dateCreated ?: System.currentTimeMillis()
                    )
                    onSave(question)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ActionPrimaryBlue)
            ) {
                Text(
                    text = if (existingQuestion != null) "Save Changes" else "Add to Family Pool",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}
