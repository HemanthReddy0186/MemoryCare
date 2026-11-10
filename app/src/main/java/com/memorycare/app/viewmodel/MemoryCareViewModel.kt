package com.memorycare.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.memorycare.app.alarm.ReminderAlarmScheduler
import com.memorycare.app.data.LocalStorageRepository
import com.memorycare.app.data.ProfileDataStore
import com.memorycare.app.model.AdaptiveTransition
import com.memorycare.app.model.AlertSeverity
import com.memorycare.app.model.AlertType
import com.memorycare.app.model.CaretakerAlert
import com.memorycare.app.model.CaretakerDetails
import com.memorycare.app.model.DifficultyLevel
import com.memorycare.app.model.FaceMatchQuestion
import com.memorycare.app.model.GameResult
import com.memorycare.app.model.GameType
import com.memorycare.app.model.PatientInquiry
import com.memorycare.app.model.PatientLocation
import com.memorycare.app.model.Reminder
import com.memorycare.app.model.ReminderVerificationStatus
import com.memorycare.app.model.SatelliteSyncStatus
import com.memorycare.app.model.UserProfile
import com.memorycare.app.model.UserRole
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DemoWaypoint(
    val stepNumber: Int,
    val label: String,
    val zoneName: String,
    val distanceMeters: Float,
    val bearingDegrees: Float,
    val latOffset: Double,
    val lngOffset: Double
)

data class AdaptiveAdaptationResult(
    val previousDifficulty: DifficultyLevel,
    val newDifficulty: DifficultyLevel,
    val transition: AdaptiveTransition
)

/**
 * Single SharedViewModel holding live reactive application state across all screens.
 */
class MemoryCareViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalStorageRepository(application)
    private val profileDataStore = ProfileDataStore(application)
    private val reminderAlarmScheduler = ReminderAlarmScheduler(application)

    // DataStore Profiles State
    val profiles: StateFlow<List<UserProfile>> = profileDataStore.profilesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    val currentProfile: StateFlow<UserProfile?> = _currentProfile.asStateFlow()

    // Active Patient profile whose metrics, games, alerts, and geofence are active
    private val _activePatientProfile = MutableStateFlow<UserProfile?>(null)
    val activePatientProfile: StateFlow<UserProfile?> = _activePatientProfile.asStateFlow()

    private val _userRole = MutableStateFlow(repository.getUserRole())
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _gameResults = MutableStateFlow<List<GameResult>>(emptyList())
    val gameResults: StateFlow<List<GameResult>> = _gameResults.asStateFlow()

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    private val _isGeofenceEnabled = MutableStateFlow(true)
    val isGeofenceEnabled: StateFlow<Boolean> = _isGeofenceEnabled.asStateFlow()

    // Geofence & Telemetry Core Parameters
    var homeBaseLatitude = 12.9716
        private set
    var homeBaseLongitude = 77.5946
        private set
    var homeBaseAddress = "Calibrating Safe Perimeter..."
        private set
    var isHomeBaseCalibrated = false
        private set
    val safeGeofenceRadiusMeters = 150f

    private var liveWalkJob: Job? = null
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    private val demoWaypoints = listOf(
        DemoWaypoint(
            stepNumber = 1,
            label = "Inside Residence / Room 104",
            zoneName = "Residence Interior - Safe Zone",
            distanceMeters = 20f,
            bearingDegrees = 30f,
            latOffset = 0.00012,
            lngOffset = 0.00010
        ),
        DemoWaypoint(
            stepNumber = 2,
            label = "Inner Courtyard & Therapy Garden",
            zoneName = "Recreation Garden - Safe Perimeter",
            distanceMeters = 60f,
            bearingDegrees = 45f,
            latOffset = 0.00038,
            lngOffset = 0.00038
        ),
        DemoWaypoint(
            stepNumber = 3,
            label = "Walkway towards Property Boundary",
            zoneName = "Facility Grounds - Approaching Perimeter",
            distanceMeters = 105f,
            bearingDegrees = 50f,
            latOffset = 0.00068,
            lngOffset = 0.00072
        ),
        DemoWaypoint(
            stepNumber = 4,
            label = "Main Gate (Boundary Threshold)",
            zoneName = "Property Perimeter (10m inside boundary)",
            distanceMeters = 140f,
            bearingDegrees = 55f,
            latOffset = 0.00092,
            lngOffset = 0.00095
        ),
        DemoWaypoint(
            stepNumber = 5,
            label = "Public Sidewalk (BREACH TRIGGERED)",
            zoneName = "Outside Safe Perimeter - Public Street",
            distanceMeters = 175f,
            bearingDegrees = 60f,
            latOffset = 0.00115,
            lngOffset = 0.00125
        ),
        DemoWaypoint(
            stepNumber = 6,
            label = "Commercial Strip (Wandering Active)",
            zoneName = "High-Traffic Public Corridor (70m outside)",
            distanceMeters = 220f,
            bearingDegrees = 65f,
            latOffset = 0.00145,
            lngOffset = 0.00155
        ),
        DemoWaypoint(
            stepNumber = 7,
            label = "Disoriented Wanderer near Traffic Junction",
            zoneName = "Critical Safety Zone (115m outside)",
            distanceMeters = 265f,
            bearingDegrees = 70f,
            latOffset = 0.00175,
            lngOffset = 0.00185
        )
    )

    private val _patientLocation = MutableStateFlow(
        PatientLocation(
            latitude = 12.9716,
            longitude = 77.5946,
            locationName = "Acquiring GPS Location...",
            zoneDescription = "Initializing Safe Perimeter (${safeGeofenceRadiusMeters.toInt()}m)",
            isGeofenceBreached = false,
            distanceToBoundaryMeters = 150,
            distanceFromCenterMeters = 0f,
            safeRadiusMeters = 150f,
            bearingDegrees = 0f,
            trackingMode = "Autonomous GPS",
            satellitesInView = 14,
            isSimulationRunning = false,
            simulationStep = 0,
            totalSimulationSteps = 7
        )
    )
    val patientLocation: StateFlow<PatientLocation> = _patientLocation.asStateFlow()

    private val _alertsList = MutableStateFlow<List<CaretakerAlert>>(emptyList())
    val alertsList: StateFlow<List<CaretakerAlert>> = _alertsList.asStateFlow()

    val unreadAlertsCount: StateFlow<Int> = combine(_alertsList) { alerts ->
        alerts.first().count { !it.isDismissed }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val activeCriticalAlerts: StateFlow<List<CaretakerAlert>> = combine(_alertsList) { alerts ->
        alerts.first().filter { !it.isDismissed && (it.severity == AlertSeverity.CRITICAL || it.alertType == AlertType.SIGNIFICANT_DROP || it.alertType == AlertType.CONSISTENCY_WARNING) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Current Automated Adaptive Difficulty state per game type
    private val _difficultyPrefs = MutableStateFlow<Map<GameType, DifficultyLevel>>(
        mapOf(
            GameType.MEMORY_MATCH to DifficultyLevel.EASY,
            GameType.NUMBER_SEQUENCE to DifficultyLevel.EASY,
            GameType.DAILY_ROUTINE to DifficultyLevel.EASY,
            GameType.CATEGORY_SORTING to DifficultyLevel.EASY,
            GameType.FACE_NAME_ASSOCIATION to DifficultyLevel.EASY
        )
    )
    val difficultyPrefs: StateFlow<Map<GameType, DifficultyLevel>> = _difficultyPrefs.asStateFlow()

    // Snackbar event bus for Caretaker Dashboard
    private val _snackbarEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    // Fully editable Caretaker & Patient Details synced with Voice Companion
    private val _caretakerDetails = MutableStateFlow(CaretakerDetails())
    val caretakerDetails: StateFlow<CaretakerDetails> = _caretakerDetails.asStateFlow()

    // Patient Voice Inquiries live stream for caretaker monitoring
    private val _patientInquiries = MutableStateFlow<List<PatientInquiry>>(emptyList())
    val patientInquiries: StateFlow<List<PatientInquiry>> = _patientInquiries.asStateFlow()

    // Caretaker-configured Face Match Questions & active target difficulty
    private val _faceMatchQuestions = MutableStateFlow<List<FaceMatchQuestion>>(emptyList())
    val faceMatchQuestions: StateFlow<List<FaceMatchQuestion>> = _faceMatchQuestions.asStateFlow()

    private val _faceMatchTargetDifficulty = MutableStateFlow<DifficultyLevel>(DifficultyLevel.EASY)
    val faceMatchTargetDifficulty: StateFlow<DifficultyLevel> = _faceMatchTargetDifficulty.asStateFlow()

    private val _satelliteStatus = MutableStateFlow(
        SatelliteSyncStatus(
            satelliteId = "NavIC-IRNSS-1I",
            groundStation = "NER-Guwahati GS",
            isConnected = true,
            signalStrengthPercentage = 96,
            latencyMs = 28,
            lastSyncTimeFormatted = getCurrentFormattedTime(),
            downlinkSpeed = "1.2 MB/s"
        )
    )
    val satelliteStatus: StateFlow<SatelliteSyncStatus> = _satelliteStatus.asStateFlow()

    // Calculated Cognitive Effectiveness Score (0 to 100) dynamically updated from game results + reminder adherence
    val cognitiveEffectivenessScore: StateFlow<Int> = combine(_gameResults, _reminders) { results, rems ->
        calculateEffectivenessScore(results, rems)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val completedRemindersCount: StateFlow<Int> = combine(_reminders) { remsList ->
        remsList.first().count { it.isCompleted }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        // Auto-load last selected profile or default profile from DataStore on startup
        viewModelScope.launch {
            combine(profileDataStore.profilesFlow, profileDataStore.lastSelectedProfileIdFlow) { profList, lastId ->
                Pair(profList, lastId)
            }.collect { (profList, lastId) ->
                if (profList.isNotEmpty() && _currentProfile.value == null) {
                    val matching = profList.firstOrNull { it.id == lastId } ?: profList.first()
                    loadProfileData(matching)
                }
            }
        }
    }

    /**
     * Gets the active patient profile ID whose data (games, score, alerts, reminders, geofence)
     * is being played or monitored. If a Caretaker is logged in, this returns the linked Patient's ID.
     */
    private fun getActiveProfileId(): String {
        val curr = _currentProfile.value
        return if (curr?.defaultRole == UserRole.CARETAKER) {
            _activePatientProfile.value?.id ?: curr.linkedPatientId ?: LocalStorageRepository.DEFAULT_PATIENT_PROFILE_ID
        } else {
            curr?.id ?: LocalStorageRepository.DEFAULT_PATIENT_PROFILE_ID
        }
    }

    /**
     * Loads completely isolated data for the specified profile.
     * - For Patients: loads and scopes all cognitive scores, games, alerts, reminders to this patient.
     * - For Caretakers: resolves the assigned/linked Patient, and scopes the dashboard, progress sheets,
     *   geofence, and alerts to that patient's real data.
     */
    fun loadProfileData(profile: UserProfile, targetPatientId: String? = null) {
        _currentProfile.value = profile
        _userRole.value = profile.defaultRole
        repository.saveUserRole(profile.defaultRole)

        val patientProfile: UserProfile = if (profile.defaultRole == UserRole.CARETAKER) {
            val allProfiles = profiles.value
            val patientIdToUse = targetPatientId ?: profile.linkedPatientId
            allProfiles.firstOrNull { it.id == patientIdToUse && it.defaultRole == UserRole.PATIENT }
                ?: allProfiles.firstOrNull { it.defaultRole == UserRole.PATIENT }
                ?: UserProfile(
                    id = LocalStorageRepository.DEFAULT_PATIENT_PROFILE_ID,
                    name = "Grandpa John",
                    age = 78,
                    defaultRole = UserRole.PATIENT
                )
        } else {
            profile
        }

        _activePatientProfile.value = patientProfile
        val activePatientId = patientProfile.id

        // Load all data scoped to the active patient
        _gameResults.value = repository.getGameResults(activePatientId)
        val loadedReminders = repository.getReminders(activePatientId)
        _reminders.value = loadedReminders
        reminderAlarmScheduler.syncAllReminders(loadedReminders)
        _alertsList.value = repository.getCaretakerAlerts(activePatientId)
        _difficultyPrefs.value = repository.getDifficultyPrefs(activePatientId)

        val baseCaretakerDetails = repository.getCaretakerDetails(activePatientId)
        val resolvedCaretakerDetails = if (profile.defaultRole == UserRole.CARETAKER) {
            baseCaretakerDetails.copy(
                caretakerName = profile.name,
                patientName = patientProfile.name,
                patientAge = patientProfile.age
            )
        } else {
            baseCaretakerDetails.copy(
                patientName = patientProfile.name,
                patientAge = patientProfile.age
            )
        }
        _caretakerDetails.value = resolvedCaretakerDetails
        repository.saveCaretakerDetails(resolvedCaretakerDetails, activePatientId)

        _patientInquiries.value = repository.getPatientInquiries(activePatientId)
        _faceMatchQuestions.value = repository.getFaceMatchQuestions(activePatientId)
        _faceMatchTargetDifficulty.value = repository.getFaceMatchTargetDifficulty(activePatientId)
        _isGeofenceEnabled.value = repository.isGeofenceEnabled(activePatientId)
        _satelliteStatus.value = repository.getSatelliteStatus(activePatientId)

        val savedHome = repository.getHomeBase(activePatientId)
        if (savedHome != null) {
            homeBaseLatitude = savedHome.first
            homeBaseLongitude = savedHome.second
            homeBaseAddress = savedHome.third
            isHomeBaseCalibrated = true
            _patientLocation.value = _patientLocation.value.copy(
                latitude = homeBaseLatitude,
                longitude = homeBaseLongitude,
                locationName = homeBaseAddress,
                zoneDescription = "Safe Home Perimeter (${safeGeofenceRadiusMeters.toInt()}m)",
                distanceFromCenterMeters = 0f,
                distanceToBoundaryMeters = safeGeofenceRadiusMeters.toInt(),
                isGeofenceBreached = false
            )
        } else {
            isHomeBaseCalibrated = false
            startAutonomousGpsTracking()
        }
    }

    /**
     * Allows a Caretaker to switch the active patient they are monitoring.
     * Instantly refreshes all progress sheets, alerts, reminders, and geofence telemetry
     * to the newly selected patient.
     */
    fun switchActivePatient(patient: UserProfile) {
        val curr = _currentProfile.value
        if (curr != null && curr.defaultRole == UserRole.CARETAKER) {
            loadProfileData(curr, targetPatientId = patient.id)
            viewModelScope.launch {
                profileDataStore.updateLinkedPatient(curr.id, patient.id)
            }
            _snackbarEvent.tryEmit("Switched view to ${patient.name} (${patient.age} yrs)")
        }
    }

    /**
     * Selects a user profile, updates DataStore last selected, and navigates with that profile.
     */
    fun selectProfile(profile: UserProfile, targetPatientId: String? = null) {
        loadProfileData(profile, targetPatientId = targetPatientId)
        viewModelScope.launch {
            profileDataStore.setLastSelectedProfileId(profile.id)
        }
    }

    /**
     * Creates a new user profile with DataStore persistence.
     * If role is CARETAKER, linkedPatientId associates this caretaker to a specific patient.
     */
    fun createProfile(
        name: String,
        age: Int,
        role: UserRole,
        avatarEmoji: String,
        linkedPatientId: String? = null
    ) {
        val newProfile = UserProfile(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            age = age,
            defaultRole = role,
            avatarEmoji = avatarEmoji,
            linkedPatientId = linkedPatientId
        )
        viewModelScope.launch {
            profileDataStore.saveProfile(newProfile)
            profileDataStore.setLastSelectedProfileId(newProfile.id)
            loadProfileData(newProfile, targetPatientId = linkedPatientId)
        }
    }

    /**
     * Creates a linked Care Pair (Patient + Respective Caretaker) in a single step.
     */
    fun createCarePair(
        patientName: String,
        patientAge: Int,
        caretakerName: String,
        caretakerAge: Int = 38,
        caretakerRelation: String = "Primary Caretaker"
    ) {
        val patientId = UUID.randomUUID().toString()
        val caretakerId = UUID.randomUUID().toString()

        val patientProfile = UserProfile(
            id = patientId,
            name = patientName.trim(),
            age = patientAge,
            defaultRole = UserRole.PATIENT,
            avatarEmoji = "👴",
            linkedPatientId = null
        )

        val caretakerProfile = UserProfile(
            id = caretakerId,
            name = caretakerName.trim(),
            age = caretakerAge,
            defaultRole = UserRole.CARETAKER,
            avatarEmoji = "👩‍⚕️",
            linkedPatientId = patientId
        )

        viewModelScope.launch {
            profileDataStore.saveProfile(patientProfile)
            profileDataStore.saveProfile(caretakerProfile)
            profileDataStore.setLastSelectedProfileId(caretakerProfile.id)

            val initialDetails = CaretakerDetails(
                caretakerName = caretakerName.trim(),
                relationship = caretakerRelation.trim(),
                patientName = patientName.trim(),
                patientAge = patientAge
            )
            repository.saveCaretakerDetails(initialDetails, patientId)

            loadProfileData(caretakerProfile, targetPatientId = patientId)
        }
    }

    /**
     * Deletes a profile from DataStore and purges all of its isolated storage and photos.
     */
    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            profileDataStore.deleteProfile(profileId)
            repository.clearProfileData(profileId)
            if (_currentProfile.value?.id == profileId) {
                val remaining = profiles.value.filter { it.id != profileId }
                if (remaining.isNotEmpty()) {
                    selectProfile(remaining.first())
                } else {
                    _currentProfile.value = null
                }
            }
        }
    }

    /**
     * Signs out of current active profile and returns to profile selection.
     */
    fun signOutProfile() {
        _currentProfile.value = null
        _userRole.value = UserRole.NONE
        repository.saveUserRole(UserRole.NONE)
        viewModelScope.launch {
            profileDataStore.clearLastSelectedProfileId()
        }
    }

    fun selectRole(role: UserRole) {
        _userRole.value = role
        repository.saveUserRole(role)
    }

    /**
     * Gets the automated adaptive difficulty for a game.
     */
    fun getAdaptiveDifficulty(gameType: GameType): DifficultyLevel {
        return _difficultyPrefs.value[gameType] ?: DifficultyLevel.EASY
    }

    /**
     * Resets the difficulty of the newly introduced clinical games to EASY.
     */
    fun resetGameDifficultiesToEasy() {
        val profileId = getActiveProfileId()
        val updatedMap = _difficultyPrefs.value.toMutableMap()
        updatedMap[GameType.NUMBER_SEQUENCE] = DifficultyLevel.EASY
        updatedMap[GameType.DAILY_ROUTINE] = DifficultyLevel.EASY
        updatedMap[GameType.CATEGORY_SORTING] = DifficultyLevel.EASY
        _difficultyPrefs.value = updatedMap
        repository.saveDifficultyPrefs(updatedMap, profileId)
    }

    /**
     * Records game result, evaluates automated difficulty transition for next session,
     * checks performance alert conditions, and updates persistent storage.
     */
    fun recordGameResult(
        accuracyPercentage: Int,
        totalFlips: Int,
        reactionTimeMs: Long,
        totalDurationSeconds: Int,
        gameType: GameType = GameType.MEMORY_MATCH,
        currentDifficulty: DifficultyLevel = DifficultyLevel.EASY
    ): AdaptiveAdaptationResult {
        val previousResults = _gameResults.value
        val newResult = GameResult(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            accuracyPercentage = accuracyPercentage,
            totalFlips = totalFlips,
            reactionTimeMs = reactionTimeMs,
            totalDurationSeconds = totalDurationSeconds,
            gameType = gameType,
            difficulty = currentDifficulty
        )

        val profileId = getActiveProfileId()
        val updatedList = listOf(newResult) + previousResults
        _gameResults.value = updatedList
        repository.saveGameResults(updatedList, profileId)

        // 1. Compute Fully Automated Adaptive Difficulty Adjustment for NEXT session
        val (nextDifficulty, transition) = when {
            accuracyPercentage >= 80 -> {
                when (currentDifficulty) {
                    DifficultyLevel.EASY -> Pair(DifficultyLevel.MEDIUM, AdaptiveTransition.PROMOTED)
                    DifficultyLevel.MEDIUM -> Pair(DifficultyLevel.HARD, AdaptiveTransition.PROMOTED)
                    DifficultyLevel.HARD -> Pair(DifficultyLevel.HARD, AdaptiveTransition.MAINTAINED)
                }
            }
            accuracyPercentage < 60 -> {
                when (currentDifficulty) {
                    DifficultyLevel.HARD -> Pair(DifficultyLevel.MEDIUM, AdaptiveTransition.DEMOTED)
                    DifficultyLevel.MEDIUM -> Pair(DifficultyLevel.EASY, AdaptiveTransition.DEMOTED)
                    DifficultyLevel.EASY -> Pair(DifficultyLevel.EASY, AdaptiveTransition.MAINTAINED)
                }
            }
            else -> Pair(currentDifficulty, AdaptiveTransition.MAINTAINED)
        }

        // Save adjusted difficulty for next session of this game
        val updatedDifficultyMap = _difficultyPrefs.value.toMutableMap()
        updatedDifficultyMap[gameType] = nextDifficulty
        _difficultyPrefs.value = updatedDifficultyMap
        repository.saveDifficultyPrefs(updatedDifficultyMap, profileId)

        // 2. Evaluate Performance Alerts
        evaluatePerformanceAlerts(newResult, previousResults, currentDifficulty)

        // 3. Trigger satellite telemetry update packet
        _satelliteStatus.value = _satelliteStatus.value.copy(
            lastSyncTimeFormatted = getCurrentFormattedTime(),
            latencyMs = (20..32).random()
        )

        return AdaptiveAdaptationResult(
            previousDifficulty = currentDifficulty,
            newDifficulty = nextDifficulty,
            transition = transition
        )
    }

    /**
     * Evaluates drop alerts, improvement celebrations, consistency warnings, and difficulty too hard triggers.
     */
    private fun evaluatePerformanceAlerts(
        newResult: GameResult,
        previousResults: List<GameResult>,
        currentDifficulty: DifficultyLevel
    ) {
        val newAlerts = mutableListOf<CaretakerAlert>()
        val now = System.currentTimeMillis()

        val previousGameTypeResults = previousResults.filter { it.gameType == newResult.gameType }

        // Alert 1: Significant Drop Alert (Accuracy drops by >25% compared to average of last 3 sessions of same game)
        val last3Sessions = previousGameTypeResults.take(3)
        if (last3Sessions.isNotEmpty()) {
            val last3Avg = last3Sessions.map { it.accuracyPercentage }.average()
            val dropDiff = last3Avg - newResult.accuracyPercentage
            if (dropDiff > 25.0) {
                val alertMsg = "⚠️ ${newResult.gameType.displayName} performance declined significantly (Scored: ${newResult.accuracyPercentage}% vs 3-session baseline: ${last3Avg.toInt()}%). Consider easier activities today."
                newAlerts.add(
                    CaretakerAlert(
                        id = UUID.randomUUID().toString(),
                        title = "⚠️ Significant Drop Alert",
                        message = alertMsg,
                        severity = AlertSeverity.WARNING,
                        timestamp = now,
                        alertType = AlertType.SIGNIFICANT_DROP,
                        gameType = newResult.gameType
                    )
                )
                // Emit to snackbar
                _snackbarEvent.tryEmit(alertMsg)
            }
        }

        // Alert 2: Improvement Celebration (Accuracy improves by >20% compared to last session of same game)
        val lastSingleSession = previousGameTypeResults.firstOrNull()
        if (lastSingleSession != null) {
            val improvementDiff = newResult.accuracyPercentage - lastSingleSession.accuracyPercentage
            if (improvementDiff > 20) {
                newAlerts.add(
                    CaretakerAlert(
                        id = UUID.randomUUID().toString(),
                        title = "🎉 Improvement Celebration",
                        message = "🎉 Great improvement in ${newResult.gameType.displayName}! Patient scored ${newResult.accuracyPercentage}% (+${improvementDiff}%). Patient is responding well.",
                        severity = AlertSeverity.INFO,
                        timestamp = now,
                        alertType = AlertType.IMPROVEMENT_CELEBRATION,
                        gameType = newResult.gameType
                    )
                )
            }
        }

        // Alert 3: Consistency Warning (5+ games with accuracy below 40% in recent sessions)
        val allResults = listOf(newResult) + previousResults
        val lowAccuracyRecent = allResults.take(6).count { it.accuracyPercentage < 40 }
        if (lowAccuracyRecent >= 5) {
            newAlerts.add(
                CaretakerAlert(
                    id = UUID.randomUUID().toString(),
                    title = "🚨 Consistency Warning",
                    message = "⚠️ Consistency concern detected. Patient has 5+ sessions below 40% accuracy. Patient may be having a difficult week.",
                    severity = AlertSeverity.CRITICAL,
                    timestamp = now,
                    alertType = AlertType.CONSISTENCY_WARNING
                )
            )
        }

        // Alert 4: Difficulty Too Hard Alert (Patient fails 3 sessions in a row < 50% at current difficulty)
        val sameDifficultySessions = allResults.filter { it.gameType == newResult.gameType && it.difficulty == currentDifficulty }.take(3)
        if (sameDifficultySessions.size >= 3 && sameDifficultySessions.all { it.accuracyPercentage < 50 }) {
            newAlerts.add(
                CaretakerAlert(
                    id = UUID.randomUUID().toString(),
                    title = "💡 Difficulty Too Hard Alert",
                    message = "💡 ${currentDifficulty.label} difficulty might be too challenging for ${newResult.gameType.displayName}. Consider reviewing patient's status.",
                    severity = AlertSeverity.WARNING,
                    timestamp = now,
                    alertType = AlertType.DIFFICULTY_TOO_HARD,
                    gameType = newResult.gameType
                )
            )
        }

        if (newAlerts.isNotEmpty()) {
            val combined = newAlerts + _alertsList.value
            _alertsList.value = combined
            repository.saveCaretakerAlerts(combined, getActiveProfileId())
        }
    }

    /**
     * Toggles a reminder's completion state and syncs to caretaker metrics.
     */
    fun toggleReminder(reminderId: String) {
        val updated = _reminders.value.map { reminder ->
            if (reminder.id == reminderId) {
                val willBeCompleted = !reminder.isCompleted
                val toggled = reminder.copy(
                    isCompleted = willBeCompleted,
                    verificationStatus = if (willBeCompleted) ReminderVerificationStatus.PENDING_VERIFICATION else ReminderVerificationStatus.SCHEDULED,
                    completedAtTimestamp = if (willBeCompleted) System.currentTimeMillis() else 0L
                )
                if (willBeCompleted) {
                    reminderAlarmScheduler.cancelReminderAlarm(toggled.id)
                    reminderAlarmScheduler.scheduleVerificationCheckIn(toggled.id, toggled.title, delayMinutes = 30)
                } else {
                    reminderAlarmScheduler.cancelVerificationCheckIn(toggled.id)
                    reminderAlarmScheduler.scheduleReminderAlarm(toggled)
                }
                toggled
            } else {
                reminder
            }
        }
        _reminders.value = updated
        repository.saveReminders(updated, getActiveProfileId())
    }

    /**
     * Adds a new scheduled reminder / daily operation created by caretaker.
     * Immediately registers an exact system alarm with Android AlarmManager.
     */
    fun addReminder(
        title: String,
        description: String,
        timeFormatted: String,
        category: String,
        iconEmoji: String = "⏰",
        audioPromptText: String = ""
    ) {
        val newReminder = Reminder(
            id = "rem_${System.currentTimeMillis()}",
            title = title.trim(),
            description = description.trim(),
            timeFormatted = timeFormatted.trim(),
            category = category.trim(),
            iconEmoji = iconEmoji,
            isCompleted = false,
            audioPromptText = if (audioPromptText.isNotBlank()) audioPromptText.trim() else "It is time for your $title. $description"
        )
        val updated = _reminders.value + newReminder
        _reminders.value = updated
        repository.saveReminders(updated, getActiveProfileId())
        reminderAlarmScheduler.scheduleReminderAlarm(newReminder)
        _snackbarEvent.tryEmit("⏰ System Alarm scheduled for ${newReminder.timeFormatted} (${newReminder.title})")
    }

    /**
     * Deletes a scheduled reminder and cancels its system alarm.
     */
    fun deleteReminder(reminderId: String) {
        reminderAlarmScheduler.cancelReminderAlarm(reminderId)
        val updated = _reminders.value.filterNot { it.id == reminderId }
        _reminders.value = updated
        repository.saveReminders(updated, getActiveProfileId())
    }

    /**
     * Resets reminders to default mock schedule operations and syncs alarms.
     */
    fun resetDefaultReminders() {
        val defaults = repository.getDefaultMockReminders()
        _reminders.value = defaults
        repository.saveReminders(defaults, getActiveProfileId())
        reminderAlarmScheduler.syncAllReminders(defaults)
    }

    /**
     * Triggers an immediate test alarm outside the app.
     * Starts an exact system alarm in N seconds (default 5s).
     * The user can lock their phone or leave the app, and the screen will turn on,
     * play the 10-second chime, and speak the description in an AI voice.
     */
    fun triggerTestReminderAlarm(secondsFromNow: Int = 5, reminder: Reminder? = null): Long {
        val targetReminder = reminder ?: _reminders.value.firstOrNull { !it.isCompleted } ?: Reminder(
            id = "rem_test_demo",
            title = "Afternoon Blood Pressure Tablet",
            description = "Please take 1 tablet of Amlodipine with a full glass of lukewarm water.",
            timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
            category = "Medicine",
            iconEmoji = "💊",
            isCompleted = false,
            audioPromptText = "Attention. It is time for your afternoon blood pressure tablet. Please take 1 tablet with a full glass of lukewarm water now."
        )
        val triggerMs = reminderAlarmScheduler.scheduleTestAlarm(secondsFromNow, targetReminder)
        _snackbarEvent.tryEmit("⚡ Test Alarm scheduled in $secondsFromNow seconds! Lock your phone screen now to test...")
        return triggerMs
    }

    /**
     * Caretaker manually confirms that medication/routine was taken, resolving unverified alerts.
     */
    fun caretakerManualVerifyReminder(reminderId: String) {
        val updated = _reminders.value.map { reminder ->
            if (reminder.id == reminderId) {
                reminder.copy(
                    isCompleted = true,
                    verificationStatus = ReminderVerificationStatus.VERIFIED_COMPLETED,
                    verifiedAtTimestamp = System.currentTimeMillis()
                )
            } else reminder
        }
        _reminders.value = updated
        repository.saveReminders(updated, getActiveProfileId())
        reminderAlarmScheduler.cancelVerificationCheckIn(reminderId)
        _snackbarEvent.tryEmit("✅ Manually verified reminder as completed")
    }

    /**
     * Demo test for the 30-minute verification check-in flow.
     * Schedules the check-in to trigger in 15 seconds instead of 30 minutes.
     */
    fun triggerDemoVerificationCheckIn(secondsFromNow: Int = 15, reminder: Reminder? = null): Long {
        val target = reminder ?: _reminders.value.firstOrNull() ?: Reminder(
            id = "rem_test_demo",
            title = "Afternoon Blood Pressure Medicine",
            description = "Please take 1 tablet with a full glass of water.",
            timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
            category = "Medicine",
            iconEmoji = "💊",
            isCompleted = true,
            audioPromptText = "Attention. It is time for your afternoon blood pressure tablet."
        )

        // Mark target as pending verification
        val updated = _reminders.value.map {
            if (it.id == target.id) {
                it.copy(
                    isCompleted = true,
                    verificationStatus = ReminderVerificationStatus.PENDING_VERIFICATION,
                    completedAtTimestamp = System.currentTimeMillis()
                )
            } else it
        }
        _reminders.value = updated
        repository.saveReminders(updated, getActiveProfileId())

        val triggerMs = reminderAlarmScheduler.scheduleDemoVerificationCheckIn(target.id, target.title, delaySeconds = secondsFromNow)
        _snackbarEvent.tryEmit("⚡ Demo 15-Sec Check-in scheduled! Lock phone or wait 15 seconds...")
        return triggerMs
    }

    /**
     * Geofence switch toggle.
     */
    fun setGeofenceEnabled(enabled: Boolean) {
        _isGeofenceEnabled.value = enabled
        repository.setGeofenceEnabled(enabled, getActiveProfileId())
        if (!enabled) {
            stopLiveWalkSimulation()
            _patientLocation.value = _patientLocation.value.copy(
                isGeofenceBreached = false
            )
        } else {
            addAlert(
                title = "Geofence Monitoring Active",
                message = "Safe perimeter (${safeGeofenceRadiusMeters.toInt()}m) enforced around home base.",
                severity = AlertSeverity.INFO,
                alertType = AlertType.GEOFENCE
            )
        }
    }

    /**
     * Resolves human-readable street/city name from latitude and longitude using Android Geocoder.
     * Extracts exact street, landmark/sublocality, and locality so alerts sync with real neighborhoods.
     */
    private suspend fun reverseGeocodeAddress(lat: Double, lng: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(lat, lng, 1)
                if (!list.isNullOrEmpty()) {
                    val addr = list[0]
                    val street = addr.thoroughfare
                    val subLocality = addr.subLocality ?: addr.featureName
                    val locality = addr.locality ?: addr.subAdminArea
                    val fullLine = addr.getAddressLine(0)
                    when {
                        !street.isNullOrBlank() && !subLocality.isNullOrBlank() -> "$street, $subLocality"
                        !subLocality.isNullOrBlank() && !locality.isNullOrBlank() -> "$subLocality, $locality"
                        !street.isNullOrBlank() && !locality.isNullOrBlank() -> "$street, $locality"
                        !fullLine.isNullOrBlank() -> fullLine.split(",").take(2).joinToString(",").trim()
                        !locality.isNullOrBlank() -> locality
                        !subLocality.isNullOrBlank() -> subLocality
                        else -> "Local Perimeter Area"
                    }
                } else {
                    "Local Perimeter Area"
                }
            } catch (_: Exception) {
                "Local Perimeter Area"
            }
        }
    }

    /**
     * Sets the Geofence Center (Home Base) to the patient's current coordinate.
     * Allows testing anywhere in India or in the local testing venue.
     */
    fun setHomeBaseToCurrentLocation() {
        val currentLoc = _patientLocation.value
        homeBaseLatitude = currentLoc.latitude
        homeBaseLongitude = currentLoc.longitude
        isHomeBaseCalibrated = true

        viewModelScope.launch {
            val resolvedAddress = reverseGeocodeAddress(homeBaseLatitude, homeBaseLongitude)
            homeBaseAddress = resolvedAddress
            repository.saveHomeBase(homeBaseLatitude, homeBaseLongitude, resolvedAddress, getActiveProfileId())

            _patientLocation.value = currentLoc.copy(
                locationName = resolvedAddress,
                zoneDescription = "Safe Home Perimeter (${safeGeofenceRadiusMeters.toInt()}m)",
                distanceFromCenterMeters = 0f,
                distanceToBoundaryMeters = safeGeofenceRadiusMeters.toInt(),
                isGeofenceBreached = false,
                bearingDegrees = 0f
            )
            _snackbarEvent.tryEmit("🎯 Home Base calibrated to $resolvedAddress (${String.format(Locale.US, "%.4f, %.4f", homeBaseLatitude, homeBaseLongitude)})")
        }
    }

    /**
     * Starts the Hackathon Live Walk Simulation:
     * Advances the patient along a realistic wandering path step-by-step.
     * The radar blip physically moves outward every tick, and the moment distance > 150m,
     * the system automatically fires the CRITICAL GEOFENCE BREACH alert.
     */
    fun startLiveWalkSimulation(stepDelayMs: Long = 1800L) {
        if (!_isGeofenceEnabled.value) {
            setGeofenceEnabled(true)
        }
        liveWalkJob?.cancel()
        liveWalkJob = viewModelScope.launch {
            _snackbarEvent.tryEmit("🚶‍♂️ Live Walk Demo Started: Patient leaving residence...")
            for (waypoint in demoWaypoints) {
                val lat = homeBaseLatitude + waypoint.latOffset
                val lng = homeBaseLongitude + waypoint.lngOffset

                val results = FloatArray(2)
                Location.distanceBetween(homeBaseLatitude, homeBaseLongitude, lat, lng, results)
                val computedDistance = results[0]
                val computedBearing = results[1]

                val isBreached = computedDistance > safeGeofenceRadiusMeters
                val distanceToBoundary = if (isBreached) {
                    (computedDistance - safeGeofenceRadiusMeters).toInt()
                } else {
                    (safeGeofenceRadiusMeters - computedDistance).toInt()
                }

                val wasBreached = _patientLocation.value.isGeofenceBreached

                _patientLocation.value = PatientLocation(
                    latitude = lat,
                    longitude = lng,
                    locationName = if (waypoint.stepNumber == 1) "Inside Home ($homeBaseAddress)" else "${waypoint.label} ($homeBaseAddress)",
                    zoneDescription = waypoint.zoneName,
                    isGeofenceBreached = isBreached,
                    distanceToBoundaryMeters = distanceToBoundary,
                    distanceFromCenterMeters = computedDistance,
                    safeRadiusMeters = safeGeofenceRadiusMeters,
                    bearingDegrees = computedBearing,
                    trackingMode = "Live Walk Demo",
                    satellitesInView = (12..15).random(),
                    isSimulationRunning = true,
                    simulationStep = waypoint.stepNumber,
                    totalSimulationSteps = demoWaypoints.size
                )

                // Trigger satellite status ping
                _satelliteStatus.value = _satelliteStatus.value.copy(
                    lastSyncTimeFormatted = getCurrentFormattedTime(),
                    latencyMs = (22..34).random()
                )

                // First time crossing the 150m boundary!
                if (isBreached && !wasBreached) {
                    val breachArea = reverseGeocodeAddress(lat, lng)
                    val resolvedBreachArea = if (breachArea.isNotBlank() && breachArea != "Local Perimeter Area") breachArea else "${waypoint.label} ($homeBaseAddress)"
                    addAlert(
                        title = "🚨 GEOFENCE BREACH: Near $resolvedBreachArea",
                        message = "Patient has moved ${distanceToBoundary}m outside the safe perimeter near $resolvedBreachArea (Safe Zone: $homeBaseAddress). Immediate caretaker attention required.",
                        severity = AlertSeverity.CRITICAL,
                        alertType = AlertType.GEOFENCE
                    )
                    _snackbarEvent.tryEmit("🚨 ALERT: Patient has crossed the 150m safe perimeter!")
                }

                delay(stepDelayMs)
            }
            _patientLocation.value = _patientLocation.value.copy(isSimulationRunning = false)
            _snackbarEvent.tryEmit("🏁 Live Walk Demo reached furthest corridor waypoint.")
        }
    }

    /**
     * Stops the Live Walk Simulation.
     */
    fun stopLiveWalkSimulation() {
        liveWalkJob?.cancel()
        liveWalkJob = null
        _patientLocation.value = _patientLocation.value.copy(isSimulationRunning = false)
        _snackbarEvent.tryEmit("⏹️ Live Walk Demo stopped.")
    }

    /**
     * Simulates an immediate critical breach jump.
     * Geocodes the simulated coordinates based on real home base to name the actual local street/area.
     */
    fun simulateGeofenceBreach() {
        stopLiveWalkSimulation()
        if (!_isGeofenceEnabled.value) {
            _isGeofenceEnabled.value = true
            repository.setGeofenceEnabled(true, getActiveProfileId())
        }

        viewModelScope.launch {
            val breachDistance = 240f
            val distanceOutside = (breachDistance - safeGeofenceRadiusMeters).toInt()
            val breachLat = homeBaseLatitude + 0.00160
            val breachLng = homeBaseLongitude + 0.00170
            val breachArea = reverseGeocodeAddress(breachLat, breachLng)
            val resolvedArea = if (breachArea.isNotBlank() && breachArea != "Local Perimeter Area") breachArea else "Public Area outside $homeBaseAddress"

            _patientLocation.value = _patientLocation.value.copy(
                latitude = breachLat,
                longitude = breachLng,
                locationName = "Outside Safe Zone near $resolvedArea (+${distanceOutside}m)",
                zoneDescription = "🚨 Breach near $resolvedArea",
                isGeofenceBreached = true,
                distanceToBoundaryMeters = distanceOutside,
                distanceFromCenterMeters = breachDistance,
                bearingDegrees = 65f,
                trackingMode = "Simulated Breach",
                isSimulationRunning = false
            )

            addAlert(
                title = "🚨 GEOFENCE BREACH: Near $resolvedArea",
                message = "Patient has moved ${distanceOutside}m outside the safe perimeter from $homeBaseAddress. Currently detected near $resolvedArea.",
                severity = AlertSeverity.CRITICAL,
                alertType = AlertType.GEOFENCE
            )
            _snackbarEvent.tryEmit("🚨 ALERT: Patient breached perimeter near $resolvedArea!")
        }
    }

    /**
     * Resets location back to the safe zone center.
     */
    fun resetPatientLocationToSafe() {
        stopLiveWalkSimulation()
        _patientLocation.value = PatientLocation(
            latitude = homeBaseLatitude,
            longitude = homeBaseLongitude,
            locationName = homeBaseAddress.ifBlank { "Home Residence" },
            zoneDescription = "Safe Home Perimeter (${safeGeofenceRadiusMeters.toInt()}m)",
            isGeofenceBreached = false,
            distanceToBoundaryMeters = safeGeofenceRadiusMeters.toInt(),
            distanceFromCenterMeters = 0f,
            safeRadiusMeters = safeGeofenceRadiusMeters,
            bearingDegrees = 0f,
            trackingMode = "Autonomous GPS",
            satellitesInView = 14,
            isSimulationRunning = false,
            simulationStep = 0,
            totalSimulationSteps = 7
        )
        _snackbarEvent.tryEmit("🛡️ Patient reset to safe home center.")
    }

    /**
     * Starts Autonomous Real Hardware GPS Tracking.
     * Continuously senses real device movement in the background without requiring any button click.
     */
    fun startAutonomousGpsTracking() {
        try {
            val context = getApplication<Application>()
            val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (!fineGranted && !coarseGranted) {
                return
            }

            if (locationManager == null) {
                locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            }

            val mgr = locationManager ?: return

            if (locationListener == null) {
                locationListener = LocationListener { location ->
                    // Only process real GPS if simulation demo is not currently running
                    if (liveWalkJob == null || liveWalkJob?.isActive != true) {
                        processRealGpsLocation(location.latitude, location.longitude, location.accuracy)
                    }
                }
            }

            val listener = locationListener ?: return

            // Request updates from GPS and Network providers
            if (mgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mgr.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L, // every 2 seconds
                    1f,    // every 1 meter
                    listener
                )
            }
            if (mgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mgr.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    2f,
                    listener
                )
            }

            // Immediately check last known location if available
            val lastGps = mgr.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: mgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (lastGps != null && (liveWalkJob == null || liveWalkJob?.isActive != true)) {
                processRealGpsLocation(lastGps.latitude, lastGps.longitude, lastGps.accuracy)
            }
        } catch (_: SecurityException) {
            // Permission not granted yet
        } catch (_: Exception) {
            // Provider error
        }
    }

    /**
     * Stops autonomous GPS updates.
     */
    fun stopAutonomousGpsTracking() {
        try {
            locationListener?.let {
                locationManager?.removeUpdates(it)
            }
            locationListener = null
        } catch (_: Exception) {
        }
    }

    /**
     * Processes live coordinates received from physical Android GPS chip.
     */
    private fun processRealGpsLocation(lat: Double, lng: Double, accuracy: Float) {
        viewModelScope.launch {
            // If home base is not yet calibrated for this device, anchor it automatically to the first real GPS fix!
            if (!isHomeBaseCalibrated) {
                homeBaseLatitude = lat
                homeBaseLongitude = lng
                isHomeBaseCalibrated = true
                val address = reverseGeocodeAddress(lat, lng)
                homeBaseAddress = address
                repository.saveHomeBase(lat, lng, address, getActiveProfileId())
                _snackbarEvent.tryEmit("📍 Home Base auto-anchored to: $address")
            }

            val results = FloatArray(2)
            Location.distanceBetween(homeBaseLatitude, homeBaseLongitude, lat, lng, results)
            val distanceFromCenter = results[0]
            val bearing = results[1]

            val isBreached = distanceFromCenter > safeGeofenceRadiusMeters
            val distanceToBoundary = if (isBreached) {
                (distanceFromCenter - safeGeofenceRadiusMeters).toInt()
            } else {
                (safeGeofenceRadiusMeters - distanceFromCenter).toInt()
            }

            val wasBreached = _patientLocation.value.isGeofenceBreached
            val realAreaName = if (isBreached || distanceFromCenter > 45f) {
                reverseGeocodeAddress(lat, lng)
            } else {
                homeBaseAddress
            }
            val resolvedArea = if (realAreaName.isNotBlank() && realAreaName != "Local Perimeter Area") realAreaName else homeBaseAddress

            _patientLocation.value = _patientLocation.value.copy(
                latitude = lat,
                longitude = lng,
                locationName = if (isBreached) "Outside Safe Boundary near $resolvedArea (+${distanceToBoundary}m outside)" else resolvedArea,
                zoneDescription = if (isBreached) "🚨 Breach near $resolvedArea" else "🟢 In Safe Zone ($resolvedArea)",
                isGeofenceBreached = isBreached,
                distanceToBoundaryMeters = distanceToBoundary,
                distanceFromCenterMeters = distanceFromCenter,
                bearingDegrees = bearing,
                trackingMode = "Autonomous GPS (±${accuracy.toInt()}m)",
                satellitesInView = if (accuracy < 12) 16 else 12,
                isSimulationRunning = false
            )

            _satelliteStatus.value = _satelliteStatus.value.copy(
                signalStrengthPercentage = (100 - (accuracy * 2).toInt()).coerceIn(72, 99),
                lastSyncTimeFormatted = getCurrentFormattedTime()
            )

            // Automatic breach trigger
            if (isBreached && !wasBreached && _isGeofenceEnabled.value) {
                addAlert(
                    title = "🚨 GEOFENCE BREACH: Near $resolvedArea",
                    message = "Patient has physically walked ${distanceToBoundary}m outside the safe boundary from $homeBaseAddress. Currently detected near $resolvedArea (Coordinates: ${String.format(Locale.US, "%.5f, %.5f", lat, lng)}, GPS Accuracy: ±${accuracy.toInt()}m). Immediate check required.",
                    severity = AlertSeverity.CRITICAL,
                    alertType = AlertType.GEOFENCE
                )
                _snackbarEvent.tryEmit("🚨 GPS ALERT: Patient breached perimeter near $resolvedArea!")
            }
        }
    }

    fun addAlert(title: String, message: String, severity: AlertSeverity, alertType: AlertType, gameType: GameType? = null) {
        val alert = CaretakerAlert(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            severity = severity,
            timestamp = System.currentTimeMillis(),
            alertType = alertType,
            gameType = gameType,
            isDismissed = false
        )
        val updated = (listOf(alert) + _alertsList.value).take(30)
        _alertsList.value = updated
        repository.saveCaretakerAlerts(updated, getActiveProfileId())
    }

    fun dismissAlert(alertId: String) {
        val updated = _alertsList.value.filter { it.id != alertId }
        _alertsList.value = updated
        repository.saveCaretakerAlerts(updated, getActiveProfileId())
    }

    fun dismissAllAlerts() {
        _alertsList.value = emptyList()
        repository.saveCaretakerAlerts(emptyList(), getActiveProfileId())
    }

    fun clearAllAlerts() {
        dismissAllAlerts()
    }

    /**
     * Updates and saves caretaker & patient details, syncing directly to AI Voice Companion.
     */
    fun updateCaretakerDetails(details: CaretakerDetails) {
        _caretakerDetails.value = details
        repository.saveCaretakerDetails(details, getActiveProfileId())
        _patientLocation.value = _patientLocation.value.copy(
            locationName = details.facilityOrHomeName
        )
        _snackbarEvent.tryEmit("Caretaker details updated & synced to Voice Companion!")
    }

    /**
     * Records a voice inquiry from the patient, detects repetitive queries,
     * and streams to the Caretaker Dashboard.
     */
    fun recordPatientInquiry(question: String, category: String = "General") {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeFormatted = sdf.format(Date(now))

        val recentSame = _patientInquiries.value.take(6).filter {
            it.question.equals(question.trim(), ignoreCase = true) && (now - it.timestamp) < (15 * 60 * 1000L)
        }
        val isRepetitive = recentSame.isNotEmpty()

        val inquiry = PatientInquiry(
            question = question.trim(),
            timestamp = now,
            timestampFormatted = timeFormatted,
            category = category,
            isRepetitive = isRepetitive
        )

        val updated = (listOf(inquiry) + _patientInquiries.value).take(40)
        _patientInquiries.value = updated
        repository.savePatientInquiries(updated, getActiveProfileId())

        // If repetitive question or emotional distress, trigger alert for caretaker
        if (isRepetitive || category.equals("Emotional", ignoreCase = true)) {
            addAlert(
                title = if (isRepetitive) "🔄 Repetitive Inquiry Alert" else "💬 Patient Emotional Query",
                message = "Patient asked: \"$question\" ($timeFormatted). Repetitive query detected. Caretaker check recommended.",
                severity = if (isRepetitive) AlertSeverity.WARNING else AlertSeverity.INFO,
                alertType = AlertType.GENERAL
            )
        }
    }

    /**
     * Adds a new Face Match question created by the caretaker.
     */
    fun addFaceMatchQuestion(question: FaceMatchQuestion) {
        val updated = listOf(question) + _faceMatchQuestions.value
        _faceMatchQuestions.value = updated
        repository.saveFaceMatchQuestions(updated, getActiveProfileId())
        _snackbarEvent.tryEmit("Face Match question for ${question.personName} added!")
    }

    /**
     * Updates an existing Face Match question.
     */
    fun updateFaceMatchQuestion(question: FaceMatchQuestion) {
        val updated = _faceMatchQuestions.value.map {
            if (it.id == question.id) question else it
        }
        _faceMatchQuestions.value = updated
        repository.saveFaceMatchQuestions(updated, getActiveProfileId())
        _snackbarEvent.tryEmit("Question for ${question.personName} updated!")
    }

    /**
     * Deletes a Face Match question.
     */
    fun deleteFaceMatchQuestion(questionId: String) {
        val updated = _faceMatchQuestions.value.filter { it.id != questionId }
        _faceMatchQuestions.value = updated
        repository.saveFaceMatchQuestions(updated, getActiveProfileId())
        _snackbarEvent.tryEmit("Question removed from game.")
    }

    /**
     * Sets the caretaker-configured difficulty level for the patient's Family Face Match game.
     */
    fun setFaceMatchTargetDifficulty(difficulty: DifficultyLevel) {
        val profileId = getActiveProfileId()
        _faceMatchTargetDifficulty.value = difficulty
        repository.saveFaceMatchTargetDifficulty(difficulty, profileId)

        // Also sync to difficultyPrefs map so adaptive tracking respects it
        val updatedMap = _difficultyPrefs.value.toMutableMap()
        updatedMap[GameType.FACE_NAME_ASSOCIATION] = difficulty
        _difficultyPrefs.value = updatedMap
        repository.saveDifficultyPrefs(updatedMap, profileId)

        _snackbarEvent.tryEmit("Patient Face Match difficulty set to ${difficulty.label}!")
    }

    /**
     * Simulates satellite orbital telemetry sync with ISRO / NavIC ground station.
     */
    fun triggerSatelliteSync() {
        viewModelScope.launch {
            _satelliteStatus.value = _satelliteStatus.value.copy(
                isConnected = false,
                signalStrengthPercentage = 75
            )
            delay(600)
            val updatedStatus = _satelliteStatus.value.copy(
                isConnected = true,
                signalStrengthPercentage = 98,
                latencyMs = (20..30).random(),
                lastSyncTimeFormatted = getCurrentFormattedTime()
            )
            _satelliteStatus.value = updatedStatus
            repository.saveSatelliteStatus(updatedStatus, getActiveProfileId())
        }
    }

    private fun calculateEffectivenessScore(results: List<GameResult>, rems: List<Reminder>): Int {
        if (results.isEmpty()) {
            return 0
        }
        val avgAccuracy = results.take(6).map { it.accuracyPercentage }.average()
        val speedBonus = if (results.first().reactionTimeMs < 1500) 10 else 0
        val gameScore = (avgAccuracy * 0.7 + speedBonus).coerceIn(40.0, 100.0)

        val completedCount = rems.count { it.isCompleted }
        val reminderFactor = if (rems.isNotEmpty()) (completedCount.toDouble() / rems.size) * 20.0 else 10.0

        val total = (gameScore * 0.8 + reminderFactor).toInt().coerceIn(10, 99)
        return total
    }

    private fun getCurrentFormattedTime(): String {
        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveWalkSimulation()
        stopAutonomousGpsTracking()
    }
}
