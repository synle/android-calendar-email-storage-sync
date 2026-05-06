package com.unifiedhub.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unifiedhub.app.ui.screen.digest.DigestScreen
import com.unifiedhub.app.ui.screen.permissions.PermissionsScreen
import com.unifiedhub.app.ui.screen.timeline.TimelineScreen

object Routes {
    const val PERMISSIONS = "permissions"
    const val TIMELINE = "timeline"
    const val DIGEST = "digest"
}

@Composable
fun UnifiedHubNavGraph(startDestination: String = Routes.PERMISSIONS) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(
                onAllGranted = {
                    navController.navigate(Routes.TIMELINE) {
                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.TIMELINE) {
            TimelineScreen(
                onNavigateToDigest = {
                    navController.navigate(Routes.DIGEST)
                }
            )
        }

        composable(Routes.DIGEST) {
            DigestScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
