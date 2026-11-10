package com.memorycare.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.memorycare.app.ui.screens.AlertCenterScreen
import com.memorycare.app.ui.screens.CaretakerDashboardScreen
import com.memorycare.app.ui.screens.CircadianClockGameScreen
import com.memorycare.app.ui.screens.EchoesOfLifeGameScreen
import com.memorycare.app.ui.screens.FaceNameAssociationGameScreen
import com.memorycare.app.ui.screens.ManageFaceMatchQuestionsScreen
import com.memorycare.app.ui.screens.MemoryMatchGameScreen
import com.memorycare.app.ui.screens.PatientHomeScreen
import com.memorycare.app.ui.screens.ProfileSelectionScreen
import com.memorycare.app.ui.screens.ProgressAnalyticsScreen
import com.memorycare.app.ui.screens.RemindersScreen
import com.memorycare.app.ui.screens.VoiceCompanionScreen
import com.memorycare.app.viewmodel.MemoryCareViewModel

/**
 * Main Compose Navigation Graph connecting all screens with direct profile routing.
 */
@Composable
fun MemoryCareNavGraph(
    viewModel: MemoryCareViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.ProfileSelection.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Screen 1: Unified Profile Selection (Direct route to Patient Home or Caretaker Dashboard)
        composable(route = Screen.ProfileSelection.route) {
            ProfileSelectionScreen(
                viewModel = viewModel,
                onNavigateToPatientHome = {
                    navController.navigate(Screen.PatientHome.route)
                },
                onNavigateToCaretakerDashboard = {
                    navController.navigate(Screen.CaretakerDashboard.route)
                }
            )
        }

        // Screen 2: Patient Home (Play 3 Games, Reminders, SOS)
        composable(route = Screen.PatientHome.route) {
            PatientHomeScreen(
                viewModel = viewModel,
                onNavigateToMemoryMatch = {
                    navController.navigate(Screen.MemoryMatchGame.route)
                },
                onNavigateToNumberSequence = {
                    navController.navigate(Screen.NumberSequenceGame.route)
                },
                onNavigateToCategorySorting = {
                    navController.navigate(Screen.CategorySortingGame.route)
                },
                onNavigateToFaceNameAssociation = {
                    navController.navigate(Screen.FaceNameAssociationGame.route)
                },
                onNavigateToVoiceCompanion = {
                    navController.navigate(Screen.VoiceCompanion.route)
                },
                onNavigateToReminders = {
                    navController.navigate(Screen.Reminders.route)
                },
                onNavigateToProfileSelection = {
                    navController.navigate(Screen.ProfileSelection.route) {
                        popUpTo(Screen.ProfileSelection.route) { inclusive = true }
                    }
                },
                onSignOut = {
                    navController.navigate(Screen.ProfileSelection.route) {
                        popUpTo(Screen.ProfileSelection.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 3: Memory Match Game (3x4, 4x4, 4x5 flip grid)
        composable(route = Screen.MemoryMatchGame.route) {
            MemoryMatchGameScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Screen 4: Journey Through a Day (Circadian Rhythm & Clock Orientation)
        composable(route = Screen.CircadianClockGame.route) {
            CircadianClockGameScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Screen 5: Echoes of Life (Nostalgia Soundscapes & Auditory Reminiscence)
        composable(route = Screen.EchoesOfLifeGame.route) {
            EchoesOfLifeGameScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Screen 6: Caretaker Dashboard
        composable(route = Screen.CaretakerDashboard.route) {
            CaretakerDashboardScreen(
                viewModel = viewModel,
                onNavigateToProgressAnalytics = {
                    navController.navigate(Screen.ProgressAnalytics.route)
                },
                onNavigateToAlertCenter = {
                    navController.navigate(Screen.AlertCenter.route)
                },
                onNavigateToReminders = {
                    navController.navigate(Screen.Reminders.route)
                },
                onNavigateToManageFaceMatch = {
                    navController.navigate(Screen.ManageFaceMatchQuestions.route)
                },
                onNavigateToProfileSelection = {
                    navController.navigate(Screen.ProfileSelection.route) {
                        popUpTo(Screen.ProfileSelection.route) { inclusive = true }
                    }
                },
                onSignOut = {
                    navController.navigate(Screen.ProfileSelection.route) {
                        popUpTo(Screen.ProfileSelection.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 7: Progress Analytics & Graphs
        composable(route = Screen.ProgressAnalytics.route) {
            ProgressAnalyticsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Screen 8: Alert Center
        composable(route = Screen.AlertCenter.route) {
            AlertCenterScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Screen 9: Reminders Screen
        composable(route = Screen.Reminders.route) {
            RemindersScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Screen 10: Two-Way Voice Conversational Companion
        composable(route = Screen.VoiceCompanion.route) {
            VoiceCompanionScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Screen 11: Face-Name Association Game
        composable(route = Screen.FaceNameAssociationGame.route) {
            FaceNameAssociationGameScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Screen 12: Manage Face Match Questions (Caretaker Setup)
        composable(route = Screen.ManageFaceMatchQuestions.route) {
            ManageFaceMatchQuestionsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
