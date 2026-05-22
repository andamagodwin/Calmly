package app.andama.calmly.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.andama.calmly.screens.*

@Composable
fun CalmlyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOverwhelmClick = { navController.navigate(Screen.Delay.route) },
                onNightResetClick = { navController.navigate(Screen.NightReset.route) }
            )
        }
        
        composable(Screen.Delay.route) {
            DelayScreen(
                onNext = { navController.navigate(Screen.Breathing.route) },
                onBack = { navController.popBackStack() }
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
                onBackToHome = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) } }
            )
        }
        
        composable(Screen.NightReset.route) {
            NightResetScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
