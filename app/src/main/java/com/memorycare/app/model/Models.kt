package com.memorycare.app.model

/**
 * User roles supported by MemoryCare.
 */
enum class UserRole {
    NONE,
    PATIENT,
    CARETAKER
}

/**
 * Local user profile supporting personalized patient / caretaker experiences.
 */
data class UserProfile(
    val id: String,
    val name: String,
    val age: Int,
    val defaultRole: UserRole,
    val avatarEmoji: String = if (defaultRole == UserRole.PATIENT) "👴" else "👩‍⚕️",
    val linkedPatientId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Game types supported in the cognitive stimulation suite.
 */
enum class GameType(val displayName: String, val emoji: String, val description: String, val colorHex: Long) {
    MEMORY_MATCH("Memory Match", "🧠", "Find matching pairs in the matrix", 0xFF1976D2),
    NUMBER_SEQUENCE("Journey Through a Day", "☀️", "Match the sun, clock & daily life rhythms", 0xFFE65100),
    DAILY_ROUTINE("Echoes of Life", "🎶", "Listen to comforting sounds & nostalgic memories", 0xFF00838F),
    CATEGORY_SORTING("Echoes of Life", "🎶", "Listen to comforting sounds & nostalgic memories", 0xFF00838F),
    FACE_NAME_ASSOCIATION("Face-Name Association", "👥", "Identify family, friends & loved ones", 0xFF6A1B9A)
}

/**
 * An option for everyday routines corresponding to a specific hour of the day.
 */
data class DayRoutineOption(
    val id: String,
    val title: String,
    val emoji: String,
    val isCorrect: Boolean,
    val explanation: String
)

/**
 * A chronological circadian stage representing a time of day (Dawn, Noon, Sunset, Night).
 */
data class CircadianStage(
    val stageId: String,
    val title: String,
    val timeLabel: String,
    val targetHour: Int, // 1 to 12
    val targetPeriod: String, // "AM" or "PM"
    val sunNormalizedX: Float, // 0.0f .. 1.0f across sky
    val sunNormalizedY: Float, // 0.0f .. 1.0f
    val isNight: Boolean = false,
    val skyGradientHexes: List<Long>,
    val promptQuestion: String,
    val routineOptions: List<DayRoutineOption>,
    val celebrationVoice: String,
    val ambientSoundEmoji: String = "🌤️"
)

/**
 * Types of acoustic profiles synthesized natively by the audio engine.
 */
enum class SoundscapeProfile {
    TEMPLE_BELL,
    MONSOON_RAIN,
    PRESSURE_COOKER,
    STEAM_TRAIN,
    MORNING_SPARROWS
}

/**
 * A selectable picture card choice for soundscape identification.
 */
data class SoundscapeChoice(
    val id: String,
    val title: String,
    val emoji: String,
    val subtitle: String,
    val isCorrect: Boolean
)

/**
 * A soundscape item representing a therapeutic environmental acoustic memory.
 */
data class SoundscapeItem(
    val id: String,
    val title: String,
    val emoji: String,
    val category: String,
    val soundProfile: SoundscapeProfile,
    val choices: List<SoundscapeChoice>,
    val reminiscenceStory: String,
    val voicePrompt: String
)

/**
 * Profile of a loved one, family member, or caretaker for Face-Name Association memory recall.
 */
data class FamilyMemberProfile(
    val id: String,
    val name: String,
    val relation: String,
    val relationshipCategory: String,
    val memoryClue: String,
    val avatarEmoji: String,
    val photoColorHex: Long = 0xFF6A1B9A,
    val voicePrompt: String
)

/**
 * Adaptive difficulty levels for games (Level 1, Level 2, Level 3).
 */
enum class DifficultyLevel(val levelNumber: Int, val label: String, val colorHex: Long) {
    EASY(1, "EASY", 0xFF2E7D32),
    MEDIUM(2, "MEDIUM", 0xFFE65100),
    HARD(3, "HARD", 0xFF7B1FA2)
}

/**
 * Result of automated difficulty adaptation after a game session.
 */
enum class AdaptiveTransition(val message: String, val emoji: String) {
    PROMOTED("Outstanding score! Difficulty advanced to next level.", "🚀"),
    DEMOTED("Difficulty calibrated down for comfortable reinforcement.", "👍"),
    MAINTAINED("Consistent score. Level maintained for steady practice.", "⭐")
}

/**
 * Represents a single memory card in the matching grid.
 */
data class MemoryCard(
    val id: Int,
    val pairId: Int,
    val emoji: String,
    val name: String,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)

/**
 * Historical record of a completed memory game session.
 */
data class GameResult(
    val id: String,
    val timestamp: Long,
    val accuracyPercentage: Int,
    val totalFlips: Int = 0,
    val reactionTimeMs: Long,
    val totalDurationSeconds: Int,
    val gameType: GameType = GameType.MEMORY_MATCH,
    val difficulty: DifficultyLevel = DifficultyLevel.EASY
)

/**
 * Closed-loop adherence verification states for dementia patient medication and routine reminders.
 */
enum class ReminderVerificationStatus {
    SCHEDULED,             // Active / upcoming
    PENDING_VERIFICATION,  // Patient tapped "Done/Stop" -> 30-min verification check is pending
    VERIFIED_COMPLETED,    // Confirmed at 30-min check-in (or manually verified by caretaker)
    UNVERIFIED_ESCALATED   // 30 mins elapsed without confirmation or patient tapped "Forgot" -> Caretaker alert
}

/**
 * Scheduled reminder for elderly patient (medication, nutrition, hydration).
 */
data class Reminder(
    val id: String,
    val title: String,
    val description: String,
    val timeFormatted: String,
    val category: String, // e.g. "Medicine", "Meal", "Wellness"
    val iconEmoji: String,
    val isCompleted: Boolean = false,
    val audioPromptText: String,
    val verificationStatus: ReminderVerificationStatus? = ReminderVerificationStatus.SCHEDULED,
    val completedAtTimestamp: Long? = 0L,
    val verifiedAtTimestamp: Long? = 0L
) {
    val safeVerificationStatus: ReminderVerificationStatus
        get() = verificationStatus ?: ReminderVerificationStatus.SCHEDULED

    val safeCompletedAt: Long
        get() = completedAtTimestamp ?: 0L

    val safeVerifiedAt: Long
        get() = verifiedAtTimestamp ?: 0L
}

/**
 * Geospatial telemetry for patient safety monitoring with NER focus.
 */
data class PatientLocation(
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val zoneDescription: String,
    val isGeofenceBreached: Boolean = false,
    val distanceToBoundaryMeters: Int = 35,
    val distanceFromCenterMeters: Float = 115f,
    val safeRadiusMeters: Float = 150f,
    val bearingDegrees: Float = 45f,
    val trackingMode: String = "Autonomous GPS", // "Autonomous GPS", "Live Walk Demo", "Stationary"
    val satellitesInView: Int = 14,
    val isSimulationRunning: Boolean = false,
    val simulationStep: Int = 0,
    val totalSimulationSteps: Int = 7
)

/**
 * Space-technology telemetry status (NavIC/ISRO satellite downlink).
 */
data class SatelliteSyncStatus(
    val satelliteId: String,
    val groundStation: String,
    val isConnected: Boolean,
    val signalStrengthPercentage: Int,
    val latencyMs: Int,
    val lastSyncTimeFormatted: String,
    val downlinkSpeed: String
)

/**
 * Alert category for caretaker notifications.
 */
enum class AlertType(val iconEmoji: String, val displayName: String) {
    SIGNIFICANT_DROP("⚠️", "Significant Drop"),
    IMPROVEMENT_CELEBRATION("🎉", "Improvement Celebration"),
    CONSISTENCY_WARNING("🚨", "Consistency Warning"),
    DIFFICULTY_TOO_HARD("💡", "Difficulty Recommendation"),
    MEDICATION_ADHERENCE("💊", "Medication Adherence"),
    GEOFENCE("🚨", "Geofence Safety"),
    SOS("🆘", "Emergency SOS"),
    GENERAL("ℹ️", "System Notification")
}

/**
 * Caretaker notification / alert for urgent safety events and cognitive performance triggers.
 */
data class CaretakerAlert(
    val id: String,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: Long,
    val alertType: AlertType = AlertType.GENERAL,
    val gameType: GameType? = null,
    val isDismissed: Boolean = false
)

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Caretaker contact information, patient details, and care plan notes.
 * Fully editable and synced directly to the AI Voice Companion and Caretaker Dashboard.
 */
data class CaretakerDetails(
    val caretakerName: String = "Nurse Sarah",
    val relationship: String = "Primary Care Nurse",
    val phoneNumber: String = "+91 98765 43210",
    val facilityOrHomeName: String = "Guwahati Residential Care Unit",
    val patientName: String = "Grandpa John",
    val patientAge: Int = 78,
    val emergencyContactNotes: String = "Calls every morning at 8:00 AM. In case of distress, reach Sarah directly.",
    val favoriteMemories: String = "Loves morning chamomile tea, watercolor painting with granddaughter Sarah, and walks in the garden."
)

/**
 * Logged voice inquiry or repetitive question asked by the patient to the AI Companion.
 * Streamed directly to Caretaker Central for orientation and mood monitoring.
 */
data class PatientInquiry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val question: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timestampFormatted: String,
    val category: String = "General", // e.g. "Orientation", "Meals", "Medication", "Family", "Emotional"
    val isRepetitive: Boolean = false
)

/**
 * Caretaker-configured question for the Family Face Match game.
 * Supports custom photos from gallery/camera, person name, relationship, difficulty, and 3 wrong options.
 */
data class FaceMatchQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val personName: String,
    val relationship: String,
    val difficulty: DifficultyLevel = DifficultyLevel.EASY,
    val wrongOptions: List<String> = listOf("Neighbor Alex", "Uncle Peter", "Nurse Jenny"),
    val imagePath: String? = null,
    val avatarEmoji: String = "👤",
    val memoryClue: String = "",
    val dateCreated: Long = System.currentTimeMillis()
)
