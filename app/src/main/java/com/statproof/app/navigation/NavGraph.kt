package com.statproof.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.statproof.app.ui.about.AboutScreen
import com.statproof.app.ui.composer.ProofComposerScreen
import com.statproof.app.ui.home.HomeScreen
import com.statproof.app.ui.library.ExamplesLibraryScreen
import com.statproof.app.ui.settings.SettingsScreen
import com.statproof.app.ui.viewer.ProofViewerScreen

/**
 * Type-safe navigation routes for StatProof.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Library : Screen("library")
    data object Composer : Screen("composer")
    data object Settings : Screen("settings")
    data object About : Screen("about")

    data object ProofViewer : Screen("proof/{proofId}") {
        const val ARG_PROOF_ID = "proofId"
        fun createRoute(proofId: String) = "proof/$proofId"
    }
}

/**
 * Root navigation graph for the application.
 */
@Composable
fun StatProofNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                onProofClick = { proofId ->
                    navController.navigate(Screen.ProofViewer.createRoute(proofId))
                },
                onComposeClick = {
                    navController.navigate(Screen.Composer.route)
                },
                onLibraryClick = {
                    navController.navigate(Screen.Library.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
            )
        }

        composable(
            route = Screen.ProofViewer.route,
            arguments = listOf(
                navArgument(Screen.ProofViewer.ARG_PROOF_ID) {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val proofId = requireNotNull(backStackEntry.arguments?.getString(Screen.ProofViewer.ARG_PROOF_ID))
            ProofViewerScreen(
                proofId = proofId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(route = Screen.Composer.route) {
            ProofComposerScreen(
                onBack = { navController.popBackStack() },
                onProofGenerated = { proofId ->
                    navController.navigate(Screen.ProofViewer.createRoute(proofId)) {
                        popUpTo(Screen.Composer.route) { inclusive = true }
                    }
                },
            )
        }

        composable(route = Screen.Library.route) {
            ExamplesLibraryScreen(
                onProofClick = { proofId ->
                    navController.navigate(Screen.ProofViewer.createRoute(proofId))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAboutClick = { navController.navigate(Screen.About.route) },
            )
        }

        composable(route = Screen.About.route) {
            AboutScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
