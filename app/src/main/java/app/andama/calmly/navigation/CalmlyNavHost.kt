package app.andama.calmly.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.screens.*
import app.andama.calmly.service.OverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                        OverlayService.startService(context, durationMs = 10 * 60 * 1000L, mode = "calm")
                        navController.navigate(Screen.Delay.route)
                    } else {
                        navController.navigate(Screen.PermissionRequest.route)
                    }
                },
                onUrgeClick = {
                    if (checkOverlayPermission(context)) {
                        OverlayService.startService(context, durationMs = 15 * 60 * 1000L, mode = "urge")
                        // Send accountability text if enabled
                        CoroutineScope(Dispatchers.IO).launch {
                            val tracker = CalmlyTracker(context)
                            val partner = tracker.getAccountabilityPartner()
                            if (partner != null && partner.third) {
                                sendAccountabilityText(context, partner.first, partner.second)
                            }
                            tracker.logTrigger("Urge button pressed")
                        }
                        navController.navigate(Screen.UrgeHit.route)
                    } else {
                        navController.navigate(Screen.PermissionRequest.route)
                    }
                },
                onNightResetClick = { navController.navigate(Screen.NightReset.route) },
                onAlarmClick = { navController.navigate(Screen.AlarmSetup.route) },
                onDailyCheckinClick = { navController.navigate(Screen.DailyCheckin.route) },
                onTriggerTrackerClick = { navController.navigate(Screen.TriggerTracker.route) },
                onRelapseClick = { navController.navigate(Screen.Relapse.route) },
                onDangerHoursClick = { navController.navigate(Screen.DangerHours.route) },
                onPartnerClick = { navController.navigate(Screen.Partner.route) },
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
                    OverlayService.startService(context, durationMs = 10 * 60 * 1000L, mode = "calm")
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
                onBack = {
                    OverlayService.stopService(context)
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.BodyReset.route) {
            BodyResetScreen(
                onNext = { navController.navigate(Screen.ThoughtDump.route) },
                onBack = {
                    OverlayService.stopService(context)
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ThoughtDump.route) {
            ThoughtDumpScreen(
                onNext = { navController.navigate(Screen.CalmCompletion.route) },
                onBack = {
                    OverlayService.stopService(context)
                    navController.popBackStack()
                }
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

        // Urge flow
        composable(Screen.UrgeHit.route) {
            UrgeHitScreen(
                onNext = { navController.navigate(Screen.Accountability.route) },
                onBack = {
                    OverlayService.stopService(context)
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Accountability.route) {
            AccountabilityScreen(
                onNext = { navController.navigate(Screen.UrgeComplete.route) },
                onBack = {
                    OverlayService.stopService(context)
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.UrgeComplete.route) {
            UrgeCompleteScreen(
                onBackToHome = {
                    OverlayService.stopService(context)
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) }
                }
            )
        }

        composable(Screen.AlarmSetup.route) {
            AlarmSetupScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DailyCheckin.route) {
            DailyCheckinScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TriggerTracker.route) {
            TriggerTrackerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Relapse.route) {
            RelapseScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DangerHours.route) {
            DangerHoursScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Partner.route) {
            PartnerScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
