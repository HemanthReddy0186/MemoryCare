package com.memorycare.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.memorycare.app.model.AlertSeverity
import com.memorycare.app.model.AlertType
import com.memorycare.app.model.CaretakerAlert
import com.memorycare.app.model.CaretakerDetails
import com.memorycare.app.model.DifficultyLevel
import com.memorycare.app.model.FaceMatchQuestion
import com.memorycare.app.model.GameResult
import com.memorycare.app.model.GameType
import com.memorycare.app.model.PatientInquiry
import com.memorycare.app.model.Reminder
import com.memorycare.app.model.SatelliteSyncStatus
import com.memorycare.app.model.UserRole
import com.memorycare.app.util.ImageUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalStorageRepository(private val context: Context) {

    private val globalPrefs: SharedPreferences =
        context.getSharedPreferences("memory_care_local_store", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        const val DEFAULT_PATIENT_PROFILE_ID = "profile_default_patient"

        private const val KEY_USER_ROLE = "key_user_role"
        private const val KEY_GAME_RESULTS = "key_game_results"
        private const val KEY_REMINDERS = "key_reminders"
        private const val KEY_GEOFENCE_ENABLED = "key_geofence_enabled"
        private const val KEY_CARETAKER_ALERTS = "key_caretaker_alerts"
        private const val KEY_DIFFICULTY_PREFS = "key_difficulty_prefs"
        private const val KEY_CARETAKER_DETAILS = "key_caretaker_details"
        private const val KEY_PATIENT_INQUIRIES = "key_patient_inquiries"
        private const val KEY_FACE_MATCH_QUESTIONS = "key_face_match_questions"
        private const val KEY_FACE_MATCH_TARGET_DIFFICULTY = "key_face_match_target_difficulty"
        private const val KEY_SATELLITE_STATUS = "key_satellite_status"
        private const val KEY_HOME_BASE_LAT = "key_home_base_lat"
        private const val KEY_HOME_BASE_LNG = "key_home_base_lng"
        private const val KEY_HOME_BASE_ADDRESS = "key_home_base_address"
        private const val KEY_CIRCADIAN_ECHOES_RESET_V1 = "key_circadian_echoes_reset_v1"
    }

    private fun getProfilePrefs(profileId: String): SharedPreferences {
        return context.getSharedPreferences("memory_care_profile_${profileId}", Context.MODE_PRIVATE)
    }

    fun saveUserRole(role: UserRole) {
        globalPrefs.edit().putString(KEY_USER_ROLE, role.name).apply()
    }

    fun getUserRole(): UserRole {
        val roleName = globalPrefs.getString(KEY_USER_ROLE, UserRole.NONE.name)
            ?: UserRole.NONE.name
        return try {
            UserRole.valueOf(roleName)
        } catch (e: Exception) {
            UserRole.NONE
        }
    }

    // GAME RESULTS (Isolated per profile)
    fun saveGameResults(results: List<GameResult>, profileId: String = DEFAULT_PATIENT_PROFILE_ID) {
        val json = gson.toJson(results)
        getProfilePrefs(profileId).edit().putString(KEY_GAME_RESULTS, json).apply()
    }

    fun getGameResults(profileId: String = DEFAULT_PATIENT_PROFILE_ID): List<GameResult> {
        val prefs = getProfilePrefs(profileId)
        val json = prefs.getString(KEY_GAME_RESULTS, null)
        if (json.isNullOrEmpty()) {
            return if (profileId == DEFAULT_PATIENT_PROFILE_ID) {
                getInitialMockGameResults()
            } else {
                emptyList()
            }
        }
        return try {
            val type = object : TypeToken<List<GameResult>>() {}.type
            gson.fromJson(json, type) ?: if (profileId == DEFAULT_PATIENT_PROFILE_ID) getInitialMockGameResults() else emptyList()
        } catch (e: Exception) {
            if (profileId == DEFAULT_PATIENT_PROFILE_ID) getInitialMockGameResults() else emptyList()
        }
    }

    // REMINDERS (Isolated per profile)
    fun saveReminders(reminders: List<Reminder>, profileId: String = DEFAULT_PATIENT_PROFILE_ID) {
        val json = gson.toJson(reminders)
        getProfilePrefs(profileId).edit().putString(KEY_REMINDERS, json).apply()
    }

    fun getReminders(profileId: String = DEFAULT_PATIENT_PROFILE_ID): List<Reminder> {
        val prefs = getProfilePrefs(profileId)
        val json = prefs.getString(KEY_REMINDERS, null)
        if (json.isNullOrEmpty()) {
            return if (profileId == DEFAULT_PATIENT_PROFILE_ID) {
                getDefaultMockReminders()
            } else {
                emptyList()
            }
        }
        return try {
            val type = object : TypeToken<List<Reminder>>() {}.type
            val list: List<Reminder>? = gson.fromJson(json, type)
            (list ?: if (profileId == DEFAULT_PATIENT_PROFILE_ID) getDefaultMockReminders() else emptyList()).map {
                it.copy(
                    verificationStatus = it.safeVerificationStatus,
                    completedAtTimestamp = it.safeCompletedAt,
                    verifiedAtTimestamp = it.safeVerifiedAt
                )
            }
        } catch (e: Exception) {
            if (profileId == DEFAULT_PATIENT_PROFILE_ID) getDefaultMockReminders() else emptyList()
        }
    }

    // GEOFENCE (Isolated per profile)
    fun setGeofenceEnabled(enabled: Boolean, profileId: String = DEFAULT_PATIENT_PROFILE_ID) {
        getProfilePrefs(profileId).edit().putBoolean(KEY_GEOFENCE_ENABLED, enabled).apply()
    }

    fun isGeofenceEnabled(profileId: String = DEFAULT_PATIENT_PROFILE_ID): Boolean {
        return getProfilePrefs(profileId).getBoolean(KEY_GEOFENCE_ENABLED, true)
    }

    fun saveHomeBase(lat: Double, lng: Double, address: String, profileId: String = DEFAULT_PATIENT_PROFILE_ID) {
        getProfilePrefs(profileId).edit()
            .putString(KEY_HOME_BASE_LAT, lat.toString())
            .putString(KEY_HOME_BASE_LNG, lng.toString())
            .putString(KEY_HOME_BASE_ADDRESS, address)
            .apply()
    }

    fun getHomeBase(profileId: String = DEFAULT_PATIENT_PROFILE_ID): Triple<Double, Double, String>? {
        val prefs = getProfilePrefs(profileId)
        val latStr = prefs.getString(KEY_HOME_BASE_LAT, null) ?: return null
        val lngStr = prefs.getString(KEY_HOME_BASE_LNG, null) ?: return null
        val address = prefs.getString(KEY_HOME_BASE_ADDRESS, "Home Safe Perimeter") ?: "Home Safe Perimeter"
        return try {
            Triple(latStr.toDouble(), lngStr.toDouble(), address)
        } catch (_: Exception) {
            null
        }
    }

    // CARETAKER ALERTS (Isolated per profile)
    fun saveCaretakerAlerts(alerts: List<CaretakerAlert>, profileId: String = DEFAULT_PATIENT_PROFILE_ID) {
        val json = gson.toJson(alerts)
        getProfilePrefs(profileId).edit().putString(KEY_CARETAKER_ALERTS, json).apply()
    }

    fun getCaretakerAlerts(profileId: String = DEFAULT_PATIENT_PROFILE_ID): List<CaretakerAlert> {
        val prefs = getProfilePrefs(profileId)
        val json = prefs.getString(KEY_CARETAKER_ALERTS, null)
        if (json.isNullOrEmpty()) {
            return if (profileId == DEFAULT_PATIENT_PROFILE_ID) {
                getInitialMockAlerts()
            } else {
                emptyList()
            }
        }
        return try {
            val type = object : TypeToken<List<CaretakerAlert>>() {}.type
            gson.fromJson(json, type) ?: if (profileId == DEFAULT_PATIENT_PROFILE_ID) getInitialMockAlerts() else emptyList()
        } catch (e: Exception) {
            if (profileId == DEFAULT_PATIENT_PROFILE_ID) getInitialMockAlerts() else emptyList()
        }
    }

    // ADAPTIVE DIFFICULTY PREFS (Isolated per profile)
    fun saveDifficultyPrefs(map: Map<GameType, DifficultyLevel>, profileId: String = DEFAULT_PATIENT_PROFILE_ID) {
        val json = gson.toJson(map)
        getProfilePrefs(profileId).edit().putString(KEY_DIFFICULTY_PREFS, json).apply()
    }

    fun getDifficultyPrefs(profileId: String = DEFAULT_PATIENT_PROFILE_ID): Map<GameType, DifficultyLevel> {
        val defaultMap = mapOf(
            GameType.MEMORY_MATCH to DifficultyLevel.EASY,
            GameType.NUMBER_SEQUENCE to DifficultyLevel.EASY,
            GameType.DAILY_ROUTINE to DifficultyLevel.EASY,
            GameType.CATEGORY_SORTING to DifficultyLevel.EASY,
            GameType.FACE_NAME_ASSOCIATION to DifficultyLevel.EASY
        )
        val prefs = getProfilePrefs(profileId)
        val json = prefs.getString(KEY_DIFFICULTY_PREFS, null)
        val isResetDone = prefs.getBoolean(KEY_CIRCADIAN_ECHOES_RESET_V1, false)

        if (json.isNullOrEmpty()) {
            prefs.edit().putBoolean(KEY_CIRCADIAN_ECHOES_RESET_V1, true).apply()
            return defaultMap
        }
        return try {
            val type = object : TypeToken<Map<GameType, DifficultyLevel>>() {}.type
            val loaded: Map<GameType, DifficultyLevel>? = gson.fromJson(json, type)
            if (loaded != null) {
                val mutable = loaded.toMutableMap()
                if (!isResetDone) {
                    // Reset new clinical replacement games to Easy for a fresh, welcoming baseline
                    mutable[GameType.NUMBER_SEQUENCE] = DifficultyLevel.EASY
                    mutable[GameType.DAILY_ROUTINE] = DifficultyLevel.EASY
                    mutable[GameType.CATEGORY_SORTING] = DifficultyLevel.EASY
                    prefs.edit()
                        .putString(KEY_DIFFICULTY_PREFS, gson.toJson(mutable))
                        .putBoolean(KEY_CIRCADIAN_ECHOES_RESET_V1, true)
                        .apply()
                } else if (!mutable.containsKey(GameType.DAILY_ROUTINE)) {
                    mutable[GameType.DAILY_ROUTINE] = mutable[GameType.CATEGORY_SORTING] ?: DifficultyLevel.EASY
                }
                mutable
            } else defaultMap
        } catch (e: Exception) {
            defaultMap
        }
    }

    fun resetGameDifficultiesToEasy(profileId: String = DEFAULT_PATIENT_PROFILE_ID) {
        val prefs = getProfilePrefs(profileId)
        val current = getDifficultyPrefs(profileId).toMutableMap()
        current[GameType.NUMBER_SEQUENCE] = DifficultyLevel.EASY
        current[GameType.DAILY_ROUTINE] = DifficultyLevel.EASY
        current[GameType.CATEGORY_SORTING] = DifficultyLevel.EASY
        prefs.edit()
            .putString(KEY_DIFFICULTY_PREFS, gson.toJson(current))
            .putBoolean(KEY_CIRCADIAN_ECHOES_RESET_V1, true)
            .apply()
    }

    // CARETAKER DETAILS (Isolated per profile)
    fun saveCaretakerDetails(details: CaretakerDetails, profileId: String = DEFAULT_PATIENT_PROFILE_ID) {
        val json = gson.toJson(details)
        getProfilePrefs(profileId).edit().putString(KEY_CARETAKER_DETAILS, json).apply()
    }

    fun getCaretakerDetails(profileId: String = DEFAULT_PATIENT_PROFILE_ID): CaretakerDetails {
        val json = getProfilePrefs(profileId).getString(KEY_CARETAKER_DETAILS, null)
        if (json.isNullOrEmpty()) {
            return CaretakerDetails()
        }
        return try {
            gson.fromJson(json, CaretakerDetails::class.java) ?: CaretakerDetails()
        } catch (e: Exception) {
            CaretakerDetails()
        }
    }

    // PATIENT INQUIRIES (Isolated per profile)
    fun savePatientInquiries(inquiries: List<PatientInquiry>, profileId: String = DEFAULT_PATIENT_PROFILE_ID) {
        val json = gson.toJson(inquiries)
        getProfilePrefs(profileId).edit().putString(KEY_PATIENT_INQUIRIES, json).apply()
    }

    fun getPatientInquiries(profileId: String = DEFAULT_PATIENT_PROFILE_ID): List<PatientInquiry> {
        val prefs = getProfilePrefs(profileId)
        val json = prefs.getString(KEY_PATIENT_INQUIRIES, null)
        if (json.isNullOrEmpty()) {
            return emptyList()
        }
        return try {
            val type = object : TypeToken<List<PatientInquiry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // FACE MATCH QUESTIONS (Isolated per profile)
    fun saveFaceMatchQuestions(questions: List<FaceMatchQuestion>, profileId: String = DEFAULT_PATIENT_PROFILE_ID) {
        val json = gson.toJson(questions)
        getProfilePrefs(profileId).edit().putString(KEY_FACE_MATCH_QUESTIONS, json).apply()
    }

    fun getFaceMatchQuestions(profileId: String = DEFAULT_PATIENT_PROFILE_ID): List<FaceMatchQuestion> {
        val prefs = getProfilePrefs(profileId)
        val json = prefs.getString(KEY_FACE_MATCH_QUESTIONS, null)
        if (json.isNullOrEmpty()) {
            return if (profileId == DEFAULT_PATIENT_PROFILE_ID) {
                getInitialDefaultFaceMatchQuestions()
            } else {
                emptyList()
            }
        }
        return try {
            val type = object : TypeToken<List<FaceMatchQuestion>>() {}.type
            gson.fromJson(json, type) ?: if (profileId == DEFAULT_PATIENT_PROFILE_ID) getInitialDefaultFaceMatchQuestions() else emptyList()
        } catch (e: Exception) {
            if (profileId == DEFAULT_PATIENT_PROFILE_ID) getInitialDefaultFaceMatchQuestions() else emptyList()
        }
    }

    // FACE MATCH TARGET DIFFICULTY (Isolated per profile)
    fun saveFaceMatchTargetDifficulty(difficulty: DifficultyLevel, profileId: String = DEFAULT_PATIENT_PROFILE_ID) {
        getProfilePrefs(profileId).edit().putString(KEY_FACE_MATCH_TARGET_DIFFICULTY, difficulty.name).apply()
    }

    fun getFaceMatchTargetDifficulty(profileId: String = DEFAULT_PATIENT_PROFILE_ID): DifficultyLevel {
        val name = getProfilePrefs(profileId).getString(KEY_FACE_MATCH_TARGET_DIFFICULTY, DifficultyLevel.EASY.name)
            ?: DifficultyLevel.EASY.name
        return try {
            DifficultyLevel.valueOf(name)
        } catch (e: Exception) {
            DifficultyLevel.EASY
        }
    }

    // SATELLITE STATUS (Isolated per profile)
    fun saveSatelliteStatus(status: SatelliteSyncStatus, profileId: String = DEFAULT_PATIENT_PROFILE_ID) {
        val json = gson.toJson(status)
        getProfilePrefs(profileId).edit().putString(KEY_SATELLITE_STATUS, json).apply()
    }

    fun getSatelliteStatus(profileId: String = DEFAULT_PATIENT_PROFILE_ID): SatelliteSyncStatus {
        val json = getProfilePrefs(profileId).getString(KEY_SATELLITE_STATUS, null)
        if (json.isNullOrEmpty()) {
            val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            return SatelliteSyncStatus(
                satelliteId = "NavIC-IRNSS-1I",
                groundStation = "NER-Guwahati GS",
                isConnected = true,
                signalStrengthPercentage = 96,
                latencyMs = 28,
                lastSyncTimeFormatted = sdf.format(Date()),
                downlinkSpeed = "1.2 MB/s"
            )
        }
        return try {
            gson.fromJson(json, SatelliteSyncStatus::class.java) ?: getSatelliteStatus(profileId)
        } catch (e: Exception) {
            val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            SatelliteSyncStatus(
                satelliteId = "NavIC-IRNSS-1I",
                groundStation = "NER-Guwahati GS",
                isConnected = true,
                signalStrengthPercentage = 96,
                latencyMs = 28,
                lastSyncTimeFormatted = sdf.format(Date()),
                downlinkSpeed = "1.2 MB/s"
            )
        }
    }

    // PURGE PROFILE DATA (When profile is deleted)
    fun clearProfileData(profileId: String) {
        getProfilePrefs(profileId).edit().clear().apply()
        ImageUtils.deleteProfilePhotos(context, profileId)
    }

    fun getInitialDefaultFaceMatchQuestions(): List<FaceMatchQuestion> {
        return listOf(
            // EASY (3 Questions)
            FaceMatchQuestion(
                id = "fmq_easy_1",
                personName = "Sarah",
                relationship = "Granddaughter",
                difficulty = DifficultyLevel.EASY,
                wrongOptions = listOf("Emily", "Chloe", "Jessica"),
                avatarEmoji = "👧",
                memoryClue = "Loves painting watercolors with you and brought warm cinnamon cookies yesterday."
            ),
            FaceMatchQuestion(
                id = "fmq_easy_2",
                personName = "David",
                relationship = "Eldest Son",
                difficulty = DifficultyLevel.EASY,
                wrongOptions = listOf("Michael", "Robert", "James"),
                avatarEmoji = "👨‍💼",
                memoryClue = "Calls you every Sunday morning and lives nearby in the city."
            ),
            FaceMatchQuestion(
                id = "fmq_easy_3",
                personName = "Maya",
                relationship = "Dedicated Caretaker",
                difficulty = DifficultyLevel.EASY,
                wrongOptions = listOf("Dr. Lisa", "Sister Clara", "Attendant Priya"),
                avatarEmoji = "👩‍⚕️",
                memoryClue = "Helps prepare your warm breakfast and takes garden walks with you."
            ),

            // MEDIUM (5 Questions)
            FaceMatchQuestion(
                id = "fmq_med_1",
                personName = "Sarah",
                relationship = "Granddaughter",
                difficulty = DifficultyLevel.MEDIUM,
                wrongOptions = listOf("Emily", "Chloe", "Jessica"),
                avatarEmoji = "👧",
                memoryClue = "Loves painting watercolors with you."
            ),
            FaceMatchQuestion(
                id = "fmq_med_2",
                personName = "David",
                relationship = "Eldest Son",
                difficulty = DifficultyLevel.MEDIUM,
                wrongOptions = listOf("Michael", "Robert", "James"),
                avatarEmoji = "👨‍💼",
                memoryClue = "Calls you every Sunday morning."
            ),
            FaceMatchQuestion(
                id = "fmq_med_3",
                personName = "Maya",
                relationship = "Dedicated Caretaker",
                difficulty = DifficultyLevel.MEDIUM,
                wrongOptions = listOf("Dr. Lisa", "Sister Clara", "Attendant Priya"),
                avatarEmoji = "👩‍⚕️",
                memoryClue = "Takes garden walks with you."
            ),
            FaceMatchQuestion(
                id = "fmq_med_4",
                personName = "Rohan",
                relationship = "Younger Grandson",
                difficulty = DifficultyLevel.MEDIUM,
                wrongOptions = listOf("Lucas", "Noah", "Leo"),
                avatarEmoji = "👦",
                memoryClue = "Plays football in high school and loves your train stories."
            ),
            FaceMatchQuestion(
                id = "fmq_med_5",
                personName = "Kamala",
                relationship = "Loving Sister",
                difficulty = DifficultyLevel.MEDIUM,
                wrongOptions = listOf("Aunt Rita", "Cousin Mary", "Neighbor Grace"),
                avatarEmoji = "👵",
                memoryClue = "Grew up together with you in childhood."
            ),

            // HARD (7 Questions)
            FaceMatchQuestion(
                id = "fmq_hard_1",
                personName = "Sarah",
                relationship = "Granddaughter",
                difficulty = DifficultyLevel.HARD,
                wrongOptions = listOf("Emily", "Chloe", "Jessica"),
                avatarEmoji = "👧",
                memoryClue = "Loves painting watercolors with you."
            ),
            FaceMatchQuestion(
                id = "fmq_hard_2",
                personName = "David",
                relationship = "Eldest Son",
                difficulty = DifficultyLevel.HARD,
                wrongOptions = listOf("Michael", "Robert", "James"),
                avatarEmoji = "👨‍💼",
                memoryClue = "Calls you every Sunday morning."
            ),
            FaceMatchQuestion(
                id = "fmq_hard_3",
                personName = "Maya",
                relationship = "Dedicated Caretaker",
                difficulty = DifficultyLevel.HARD,
                wrongOptions = listOf("Dr. Lisa", "Sister Clara", "Attendant Priya"),
                avatarEmoji = "👩‍⚕️",
                memoryClue = "Helps prepare your breakfast."
            ),
            FaceMatchQuestion(
                id = "fmq_hard_4",
                personName = "Rohan",
                relationship = "Younger Grandson",
                difficulty = DifficultyLevel.HARD,
                wrongOptions = listOf("Lucas", "Noah", "Leo"),
                avatarEmoji = "👦",
                memoryClue = "Plays football in high school."
            ),
            FaceMatchQuestion(
                id = "fmq_hard_5",
                personName = "Kamala",
                relationship = "Loving Sister",
                difficulty = DifficultyLevel.HARD,
                wrongOptions = listOf("Aunt Rita", "Cousin Mary", "Neighbor Grace"),
                avatarEmoji = "👵",
                memoryClue = "Shares childhood memories."
            ),
            FaceMatchQuestion(
                id = "fmq_hard_6",
                personName = "Dr. Sharma",
                relationship = "Family Doctor",
                difficulty = DifficultyLevel.HARD,
                wrongOptions = listOf("Dr. Gupta", "Dr. Banner", "Dr. Mehta"),
                avatarEmoji = "🩺",
                memoryClue = "Visits to check blood pressure and health."
            ),
            FaceMatchQuestion(
                id = "fmq_hard_7",
                personName = "Ananya",
                relationship = "Daughter-in-Law",
                difficulty = DifficultyLevel.HARD,
                wrongOptions = listOf("Pooja", "Sunita", "Meera"),
                avatarEmoji = "👩",
                memoryClue = "Brews your favorite fragrant cardamom tea."
            )
        )
    }

    private fun getInitialMockAlerts(): List<CaretakerAlert> {
        val now = System.currentTimeMillis()
        val hour = 3600 * 1000L
        return listOf(
            CaretakerAlert(
                id = "alert_mock_1",
                title = "🎉 Improvement Celebration",
                message = "Great improvement in Category Sorting! Patient scored 92% (+24% increase). Patient is responding well.",
                severity = AlertSeverity.INFO,
                timestamp = now - (2 * hour),
                alertType = AlertType.IMPROVEMENT_CELEBRATION,
                gameType = GameType.CATEGORY_SORTING
            ),
            CaretakerAlert(
                id = "alert_mock_2",
                title = "🛰️ Satellite Downlink Sync",
                message = "Telemetry active with Guwahati ISRO/NavIC Ground Station (Assam Region).",
                severity = AlertSeverity.INFO,
                timestamp = now - (5 * hour),
                alertType = AlertType.GENERAL
            )
        )
    }

    /**
     * Provides comprehensive 14-day multi-game historical telemetry sessions
     * across Memory Match, Number Sequence, and Category Sorting.
     */
    private fun getInitialMockGameResults(): List<GameResult> {
        val now = System.currentTimeMillis()
        val day = 24 * 3600 * 1000L
        val list = mutableListOf<GameResult>()

        // Day 14 ago (Oldest)
        list.add(GameResult("r1", now - 13 * day, 65, 18, 1600, 55, GameType.MEMORY_MATCH, DifficultyLevel.EASY))
        list.add(GameResult("r2", now - 13 * day, 70, 6, 1450, 32, GameType.NUMBER_SEQUENCE, DifficultyLevel.EASY))

        // Day 12 ago
        list.add(GameResult("r3", now - 11 * day, 82, 14, 1400, 48, GameType.MEMORY_MATCH, DifficultyLevel.EASY))
        list.add(GameResult("r4", now - 11 * day, 75, 7, 1300, 35, GameType.CATEGORY_SORTING, DifficultyLevel.EASY))

        // Day 10 ago (Memory Match promoted to MEDIUM)
        list.add(GameResult("r5", now - 9 * day, 85, 12, 1250, 42, GameType.NUMBER_SEQUENCE, DifficultyLevel.EASY))
        list.add(GameResult("r6", now - 9 * day, 78, 20, 1500, 52, GameType.MEMORY_MATCH, DifficultyLevel.MEDIUM))

        // Day 8 ago (Number Sequence promoted to MEDIUM)
        list.add(GameResult("r7", now - 7 * day, 88, 5, 1100, 28, GameType.NUMBER_SEQUENCE, DifficultyLevel.MEDIUM))
        list.add(GameResult("r8", now - 7 * day, 84, 8, 1200, 39, GameType.CATEGORY_SORTING, DifficultyLevel.EASY))

        // Day 6 ago
        list.add(GameResult("r9", now - 5 * day, 82, 18, 1350, 44, GameType.MEMORY_MATCH, DifficultyLevel.MEDIUM))
        list.add(GameResult("r10", now - 5 * day, 90, 8, 1050, 33, GameType.CATEGORY_SORTING, DifficultyLevel.MEDIUM))

        // Day 4 ago (Number Sequence promoted to HARD)
        list.add(GameResult("r11", now - 3 * day, 92, 6, 980, 25, GameType.NUMBER_SEQUENCE, DifficultyLevel.HARD))
        list.add(GameResult("r12", now - 3 * day, 88, 16, 1200, 40, GameType.MEMORY_MATCH, DifficultyLevel.MEDIUM))

        // Day 2 ago
        list.add(GameResult("r13", now - 1 * day, 85, 8, 1150, 36, GameType.CATEGORY_SORTING, DifficultyLevel.MEDIUM))
        list.add(GameResult("r14", now - 1 * day, 94, 6, 920, 22, GameType.NUMBER_SEQUENCE, DifficultyLevel.HARD))
        list.add(GameResult("r14b", now - 1 * day, 88, 5, 1300, 45, GameType.FACE_NAME_ASSOCIATION, DifficultyLevel.EASY))

        // Today (Most recent)
        list.add(GameResult("r15", now - (4 * 3600 * 1000L), 90, 14, 1100, 38, GameType.MEMORY_MATCH, DifficultyLevel.MEDIUM))
        list.add(GameResult("r16", now - (1 * 3600 * 1000L), 92, 8, 1020, 30, GameType.CATEGORY_SORTING, DifficultyLevel.MEDIUM))
        list.add(GameResult("r17", now - (30 * 60 * 1000L), 100, 6, 1150, 40, GameType.FACE_NAME_ASSOCIATION, DifficultyLevel.EASY))

        // Reverse to ensure most recent is first in the list
        return list.reversed()
    }

    fun getDefaultMockReminders(): List<Reminder> {
        return listOf(
            Reminder(
                id = "rem_1",
                title = "Morning Blood Pressure Tablet",
                description = "Take 1 Amlodipine tablet with a glass of lukewarm water after breakfast.",
                timeFormatted = "08:30 AM",
                category = "Medicine",
                iconEmoji = "💊",
                isCompleted = false,
                audioPromptText = "Time to take your morning blood pressure tablet with water."
            ),
            Reminder(
                id = "rem_2",
                title = "Nutritious Midday Lunch",
                description = "Warm rice, lentil dal, steamed seasonal vegetables, and fruit.",
                timeFormatted = "01:00 PM",
                category = "Meal",
                iconEmoji = "🍲",
                isCompleted = false,
                audioPromptText = "It is lunch time. Enjoy your warm meal and drink plenty of water."
            ),
            Reminder(
                id = "rem_3",
                title = "Evening Courtyard Walk & Tea",
                description = "15 minutes mild walking in the garden accompanied by tea.",
                timeFormatted = "05:30 PM",
                category = "Wellness",
                iconEmoji = "🚶",
                isCompleted = false,
                audioPromptText = "Time for your pleasant evening garden walk and warm herbal tea."
            )
        )
    }
}
