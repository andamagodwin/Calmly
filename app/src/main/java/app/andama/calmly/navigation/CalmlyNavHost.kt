package app.andama.calmly.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.andama.calmly.screens.*
import app.andama.calmly.service.OverlayService
import app.andama.calmly.screens.checkOverlayPermission

@Composable
fun CalmlyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOverwhelmClick = {
                    if (checkOverlayPermission(context)) {
                        OverlayService.startService(context)
                        navController.navigate(Screen.Delay.route)
                    } else {
                        navController.navigate(Screen.PermissionRequest.route)
                    }
                },
                onNightResetClick = { navController.navigate(Screen.NightReset.route) },
                onAchievementsClick = { navController.navigate(Screen.Achievements.route) }
            )
        }
        
        composable(Screen.Achievements.route) {
            AchievementsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.PermissionRequest.route) {
            PermissionRequestScreen(
                onPermissionGranted = {
                    OverlayService.startService(context)
                    navController.navigate(Screen.Delay.route) { popUpTo(Screen.Home.route) }
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Delay.route) {
            DelayScreen(
                onNext = { navController.navigate(Screen.Breathing.route) },
                onBack = {
                    OverlayService.stopService(context)
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Breathing.route) {
            BreathingScreen(
                onNext = { navController.navigate(Screen.BodyReset.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.BodyReset.route) {
            BodyResetScreen(
                onNext = { navController.navigate(Screen.ThoughtDump.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ThoughtDump.route) {
            ThoughtDumpScreen(
                onNext = { navController.navigate(Screen.CalmCompletion.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.CalmCompletion.route) {
            CalmCompletionScreen(
                onBackToHome = {
                    OverlayService.stopService(context)
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) }
                }
            )
        }
        
        composable(Screen.NightReset.route) {
            NightResetScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
