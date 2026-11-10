package com.memorycare.app.navigation

/**
 * Navigation destinations for screens in MemoryCare.
 */
sealed class Screen(val route: String) {
    // Screen 1: Unified Profile Selection (Direct route to Patient Home or Caretaker Dashboard)
    data object ProfileSelection : Screen("profile_selection")

    // Screen 2: Patient Home
    data object PatientHome : Screen("patient_home")

    // Screen 3: Memory Match Game (3x4, 4x4, 4x5 grid)
    data object MemoryMatchGame : Screen("memory_match_game")

    // Screen 4: Journey Through a Day (Circadian Rhythm & Clock Orientation)
    data object CircadianClockGame : Screen("circadian_clock_game")
    data object NumberSequenceGame : Screen("circadian_clock_game")

    // Screen 5: Echoes of Life (Nostalgia Soundscapes & Auditory Reminiscence)
    data object EchoesOfLifeGame : Screen("echoes_of_life_game")
    data object DailyRoutineGame : Screen("echoes_of_life_game")
    data object CategorySortingGame : Screen("echoes_of_life_game")

    // Screen 6: Caretaker Dashboard
    data object CaretakerDashboard : Screen("caretaker_dashboard")

    // Screen 7: Progress Analytics & Graphs (Line, Bar & Step charts)
    data object ProgressAnalytics : Screen("progress_analytics")

    // Screen 8: Alert Center
    data object AlertCenter : Screen("alert_center")

    // Screen 9: Reminders Screen
    data object Reminders : Screen("reminders")

    // Screen 10: Two-Way Voice Conversational Companion
    data object VoiceCompanion : Screen("voice_companion")

    // Screen 11: Face-Name Association Game
    data object FaceNameAssociationGame : Screen("face_name_association_game")

    // Screen 12: Caretaker Setup for Face Match Questions
    data object ManageFaceMatchQuestions : Screen("manage_face_match_questions")
}
