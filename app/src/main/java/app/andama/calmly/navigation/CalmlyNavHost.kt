package app.andama.calmly.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.IntOffset
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.screens.*
import app.andama.calmly.service.OverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun CalmlyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route
) {
    val context = LocalContext.current
    // Bound to the composition rather than a free-floating CoroutineScope, which
    // would outlive the screen and leak.
    val scope = rememberCoroutineScope()
    val tracker = remember { CalmlyTracker(context) }

    // Springy push/pop with depth: the incoming screen slides in with a slight
    // overshoot while scaling up from 92%, and the outgoing one scales down and
    // fades rather than just sliding off — the new screen reads as arriving
    // "above" the old one instead of the two swapping places on a flat plane.
    val bouncySlide = spring<IntOffset>(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMediumLow
    )
    val bouncyScale = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMediumLow
    )
    val exitFade = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(animationSpec = bouncySlide) { it / 4 } +
                fadeIn(tween(320)) +
                scaleIn(animationSpec = bouncyScale, initialScale = 0.92f)
        },
        exitTransition = {
            fadeOut(exitFade) + scaleOut(animationSpec = exitFade, targetScale = 0.95f)
        },
        popEnterTransition = {
            slideInHorizontally(animationSpec = bouncySlide) { -it / 4 } +
                fadeIn(tween(320)) +
                scaleIn(animationSpec = bouncyScale, initialScale = 0.92f)
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { it / 6 } +
                fadeOut(exitFade)
        }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

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
                        scope.launch(Dispatchers.IO) {
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
                onAchievementsClick = { navController.navigate(Screen.Achievements.route) },
                onPatternsClick = { navController.navigate(Screen.Patterns.route) }
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

        // A payoff screen, not a lateral step — it reveals from the center
        // instead of sliding in from the side, so the "you did it" moment reads
        // as distinct from the rest of the flow's forward progress.
        composable(
            Screen.CalmCompletion.route,
            enterTransition = {
                scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    initialScale = 0.75f
                ) + fadeIn(tween(350))
            },
            exitTransition = { fadeOut(tween(200)) }
        ) {
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

        composable(
            Screen.UrgeComplete.route,
            enterTransition = {
                scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    initialScale = 0.75f
                ) + fadeIn(tween(350))
            },
            exitTransition = { fadeOut(tween(200)) }
        ) {
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

        composable(Screen.Patterns.route) {
            PatternsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
