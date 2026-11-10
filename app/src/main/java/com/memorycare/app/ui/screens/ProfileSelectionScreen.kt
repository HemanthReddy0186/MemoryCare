package com.memorycare.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memorycare.app.model.UserProfile
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
import com.memorycare.app.ui.theme.OrbitalCyan
import com.memorycare.app.viewmodel.MemoryCareViewModel

/**
 * Unified Single Profile Selection Screen:
 * Directly routes to Patient Home (for Patients) or Caretaker Dashboard (for Caretakers)
 * in ONE single tap with zero intermediate steps.
 */
@Composable
fun ProfileSelectionScreen(
    viewModel: MemoryCareViewModel,
    onNavigateToPatientHome: () -> Unit,
    onNavigateToCaretakerDashboard: () -> Unit
) {
    val satelliteStatus by viewModel.satelliteStatus.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<UserProfile?>(null) }

    fun navigateForProfile(profile: UserProfile) {
        viewModel.selectProfile(profile)
        if (profile.defaultRole == UserRole.PATIENT) {
            onNavigateToPatientHome()
        } else {
            onNavigateToCaretakerDashboard()
        }
    }

    Scaffold(
        topBar = {
            SpaceTopBar(
                title = "MemoryCare",
                subtitle = "Select Profile",
                satelliteStatus = satelliteStatus,
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Main Heading
            Text(
                text = "Who Is Using MemoryCare?",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ElderlyTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Tap your profile to start immediately",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = ElderlyTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Clean Profile Cards List
            if (profiles.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color(0xFFD0D7DE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "👤", fontSize = 54.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Profiles Created Yet",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElderlyTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap below to add your profile and begin.",
                            fontSize = 16.sp,
                            color = ElderlyTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                profiles.forEach { profile ->
                    val isSelected = currentProfile?.id == profile.id
                    DirectProfileCard(
                        profile = profile,
                        allProfiles = profiles,
                        isSelected = isSelected,
                        onSelect = {
                            navigateForProfile(profile)
                        },
                        onDelete = {
                            profileToDelete = profile
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large Prominent "Add New Profile" Button (64dp height)
            AccessibleBigButton(
                text = "Add New Profile",
                emoji = "➕",
                onClick = { showCreateDialog = true },
                containerColor = ActionSuccessGreen,
                contentColor = Color.White,
                minHeight = 64.dp,
                fontSize = 24.sp,
                modifier = Modifier.fillMaxWidth()
            )

            if (currentProfile != null && profiles.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                AccessibleBigButton(
                    text = "Continue as ${currentProfile?.name}",
                    emoji = "➡️",
                    onClick = {
                        navigateForProfile(currentProfile!!)
                    },
                    containerColor = ActionPrimaryBlue,
                    contentColor = Color.White,
                    minHeight = 56.dp,
                    fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Telemetry & Project Info Badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = DeepSpaceNavy,
                border = BorderStroke(1.dp, Color(0xFF223F6E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🛰️", fontSize = 24.sp, modifier = Modifier.padding(end = 10.dp))
                    Column {
                        Text(
                            text = "Direct Role Launching Active",
                            color = OrbitalCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Patient profiles open Patient Home • Caretakers open Dashboard.",
                            color = Color(0xFFCFD8DC),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // Add New Profile Dialog
    if (showCreateDialog) {
        SimpleCreateProfileDialog(
            allProfiles = profiles,
            onDismiss = { showCreateDialog = false },
            onCreateSingle = { name, age, role, linkedPatientId ->
                val emoji = if (role == UserRole.PATIENT) "👴" else "👩‍⚕️"
                viewModel.createProfile(name, age, role, emoji, linkedPatientId)
                showCreateDialog = false
                if (role == UserRole.PATIENT) {
                    onNavigateToPatientHome()
                } else {
                    onNavigateToCaretakerDashboard()
                }
            },
            onCreatePair = { patientName, patientAge, caretakerName, caretakerAge, relation ->
                viewModel.createCarePair(patientName, patientAge, caretakerName, caretakerAge, relation)
                showCreateDialog = false
                onNavigateToCaretakerDashboard()
            }
        )
    }

    // Delete Confirmation Dialog
    if (profileToDelete != null) {
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = {
                Text(
                    text = "Delete Profile?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElderlyTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${profileToDelete?.name}\"?",
                    fontSize = 17.sp,
                    color = ElderlyTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = profileToDelete?.id
                        if (id != null) {
                            viewModel.deleteProfile(id)
                        }
                        profileToDelete = null
                    }
                ) {
                    Text(
                        text = "DELETE",
                        color = CriticalRed,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text(
                        text = "CANCEL",
                        color = Color.DarkGray,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

/**
 * Direct Launch Profile Card:
 * - 64dp Avatar Circle with emoji
 * - 26sp+ Large Name
 * - Role Badge ("Patient" vs "Caretaker")
 * - Linked Care Pair visualization ("Supervised by..." or "Caring for...")
 * - 1-Tap Direct Launch Arrow
 */
@Composable
private fun DirectProfileCard(
    profile: UserProfile,
    allProfiles: List<UserProfile>,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val isPatient = profile.defaultRole == UserRole.PATIENT
    val roleColor = if (isPatient) Color(0xFF00838F) else ActionWarningOrange
    val roleBg = if (isPatient) Color(0xFFE0F7FA) else Color(0xFFFFF3E0)

    val linkedNote = if (isPatient) {
        val caretaker = allProfiles.find { it.linkedPatientId == profile.id }
        if (caretaker != null) "Supervised by ${caretaker.name}" else null
    } else {
        val patient = allProfiles.find { it.id == profile.linkedPatientId }
            ?: allProfiles.firstOrNull { it.defaultRole == UserRole.PATIENT }
        if (patient != null) "Caring for ${patient.name}" else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) roleBg else Color.White
        ),
        border = BorderStroke(
            if (isSelected) 3.dp else 2.dp,
            if (isSelected) roleColor else Color(0xFFCFD8DC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large 64dp Avatar Circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(roleColor.copy(alpha = 0.15f))
                    .border(2.5.dp, roleColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.avatarEmoji,
                    fontSize = 36.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Name in Large Text (26sp+) and Role Badge
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ElderlyTextPrimary,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = roleColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, roleColor)
                    ) {
                        Text(
                            text = if (isPatient) "👤 Patient (Age ${profile.age})" else "📊 Caretaker",
                            color = roleColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ActionSuccessGreen
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (linkedNote != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPatient) "🛡️ $linkedNote" else "🩺 $linkedNote",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPatient) Color(0xFF00695C) else Color(0xFFE65100)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Delete Profile Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEBEE))
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Profile",
                    tint = CriticalRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Dialog supporting both:
 * 1. "Care Pair (Patient + Caretaker)" - 1-tap creation of linked patient & caretaker
 * 2. "Single Profile" - Create individual profile with patient assignment for caretakers
 */
@Composable
private fun SimpleCreateProfileDialog(
    allProfiles: List<UserProfile>,
    onDismiss: () -> Unit,
    onCreateSingle: (name: String, age: Int, role: UserRole, linkedPatientId: String?) -> Unit,
    onCreatePair: (patientName: String, patientAge: Int, caretakerName: String, caretakerAge: Int, relation: String) -> Unit
) {
    val patientProfiles = allProfiles.filter { it.defaultRole == UserRole.PATIENT }
    var creationMode by remember { mutableStateOf(0) } // 0 = Care Pair, 1 = Single Profile

    // Care Pair fields
    var pairPatientName by remember { mutableStateOf("Grandpa Robert") }
    var pairPatientAgeText by remember { mutableStateOf("80") }
    var pairCaretakerName by remember { mutableStateOf("Nurse Emily") }
    var pairCaretakerAgeText by remember { mutableStateOf("34") }
    var pairRelation by remember { mutableStateOf("Primary Nurse") }

    // Single Profile fields
    var singleName by remember { mutableStateOf("") }
    var singleAgeText by remember { mutableStateOf("") }
    var singleRole by remember { mutableStateOf(UserRole.PATIENT) }
    var singleLinkedPatientId by remember {
        mutableStateOf(patientProfiles.firstOrNull()?.id)
    }

    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "➕ Add Profiles",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ElderlyTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Mode Selector Tabs (Care Pair vs Single)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable {
                                creationMode = 0
                                errorMsg = null
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (creationMode == 0) ActionPrimaryBlue else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (creationMode == 0) ActionPrimaryBlue else Color(0xFFCBD5E1))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "🤝 Care Pair",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (creationMode == 0) Color.White else Color(0xFF475569)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable {
                                creationMode = 1
                                errorMsg = null
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (creationMode == 1) ActionPrimaryBlue else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (creationMode == 1) ActionPrimaryBlue else Color(0xFFCBD5E1))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "👤 Single",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (creationMode == 1) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (creationMode == 0) {
                    // ============================================
                    // MODE 0: CARE PAIR (PATIENT + CARETAKER)
                    // ============================================
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE0F7FA),
                        border = BorderStroke(1.dp, Color(0xFF00838F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "👴 PATIENT INFORMATION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00838F)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = pairPatientName,
                                onValueChange = { pairPatientName = it; errorMsg = null },
                                label = { Text("Patient Name", fontSize = 14.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = pairPatientAgeText,
                                onValueChange = {
                                    if (it.all { c -> c.isDigit() } && it.length <= 3) {
                                        pairPatientAgeText = it
                                        errorMsg = null
                                    }
                                },
                                label = { Text("Patient Age", fontSize = 14.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF3E0),
                        border = BorderStroke(1.dp, ActionWarningOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "👩‍⚕️ ASSIGNED CARETAKER",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ActionWarningOrange
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = pairCaretakerName,
                                onValueChange = { pairCaretakerName = it; errorMsg = null },
                                label = { Text("Caretaker Name", fontSize = 14.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = pairCaretakerAgeText,
                                    onValueChange = {
                                        if (it.all { c -> c.isDigit() } && it.length <= 3) {
                                            pairCaretakerAgeText = it
                                            errorMsg = null
                                        }
                                    },
                                    label = { Text("Age", fontSize = 14.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(0.4f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = pairRelation,
                                    onValueChange = { pairRelation = it; errorMsg = null },
                                    label = { Text("Relation (e.g. Nurse)", fontSize = 14.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.6f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                } else {
                    // ============================================
                    // MODE 1: SINGLE PROFILE
                    // ============================================
                    Text(
                        text = "Name",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElderlyTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = singleName,
                        onValueChange = { singleName = it; errorMsg = null },
                        placeholder = { Text("e.g. Alice", fontSize = 16.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Age",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElderlyTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = singleAgeText,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() } && it.length <= 3) {
                                singleAgeText = it
                                errorMsg = null
                            }
                        },
                        placeholder = { Text("e.g. 75", fontSize = 16.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Role",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElderlyTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isPatient = singleRole == UserRole.PATIENT
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clickable { singleRole = UserRole.PATIENT },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPatient) Color(0xFFE0F7FA) else Color(0xFFF5F5F5),
                            border = BorderStroke(if (isPatient) 2.5.dp else 1.dp, if (isPatient) Color(0xFF00838F) else Color(0xFFD0D7DE))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "👴", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Patient",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPatient) Color(0xFF00838F) else Color.DarkGray
                                )
                            }
                        }

                        val isCaretaker = singleRole == UserRole.CARETAKER
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clickable { singleRole = UserRole.CARETAKER },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCaretaker) Color(0xFFFFF3E0) else Color(0xFFF5F5F5),
                            border = BorderStroke(if (isCaretaker) 2.5.dp else 1.dp, if (isCaretaker) ActionWarningOrange else Color(0xFFD0D7DE))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "📊", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Caretaker",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCaretaker) ActionWarningOrange else Color.DarkGray
                                )
                            }
                        }
                    }

                    // If Caretaker role chosen, show Assigned Patient selector
                    if (singleRole == UserRole.CARETAKER) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Assign to Patient:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElderlyTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (patientProfiles.isEmpty()) {
                            Text(
                                text = "⚠️ No patients exist yet. Caretaker will be linked once a patient is created.",
                                fontSize = 12.sp,
                                color = CriticalRed
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                patientProfiles.forEach { patient ->
                                    val isTarget = singleLinkedPatientId == patient.id
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { singleLinkedPatientId = patient.id },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isTarget) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                                        border = BorderStroke(if (isTarget) 2.dp else 1.dp, if (isTarget) ActionPrimaryBlue else Color(0xFFCBD5E1))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = patient.avatarEmoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${patient.name} (${patient.age} yrs)",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ElderlyTextPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isTarget) {
                                                Text(text = "✓ Assigned", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ActionPrimaryBlue)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMsg!!,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CriticalRed
                    )
                }
            }
        },
        confirmButton = {
            AccessibleBigButton(
                text = if (creationMode == 0) "Create Care Pair" else "Save & Start",
                emoji = if (creationMode == 0) "🤝" else "🚀",
                onClick = {
                    if (creationMode == 0) {
                        if (pairPatientName.isBlank()) {
                            errorMsg = "Please enter patient name."
                        } else if (pairPatientAgeText.isBlank() || (pairPatientAgeText.toIntOrNull() ?: 0) <= 0) {
                            errorMsg = "Please enter a valid patient age."
                        } else if (pairCaretakerName.isBlank()) {
                            errorMsg = "Please enter caretaker name."
                        } else {
                            val patientAge = pairPatientAgeText.toInt()
                            val caretakerAge = pairCaretakerAgeText.toIntOrNull() ?: 35
                            onCreatePair(
                                pairPatientName.trim(),
                                patientAge,
                                pairCaretakerName.trim(),
                                caretakerAge,
                                pairRelation.trim().ifEmpty { "Caretaker" }
                            )
                        }
                    } else {
                        if (singleName.isBlank()) {
                            errorMsg = "Please enter a name."
                        } else if (singleAgeText.isBlank() || (singleAgeText.toIntOrNull() ?: 0) <= 0) {
                            errorMsg = "Please enter a valid age."
                        } else {
                            onCreateSingle(
                                singleName.trim(),
                                singleAgeText.toInt(),
                                singleRole,
                                if (singleRole == UserRole.CARETAKER) singleLinkedPatientId else null
                            )
                        }
                    }
                },
                containerColor = ActionSuccessGreen,
                minHeight = 52.dp,
                fontSize = 17.sp
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}
