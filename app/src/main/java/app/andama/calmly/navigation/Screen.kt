package app.andama.calmly.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object PermissionRequest : Screen("permission_request")
    object Achievements : Screen("achievements")
    object Delay : Screen("delay")
    object Breathing : Screen("breathing")
    object BodyReset : Screen("body_reset")
    object ThoughtDump : Screen("thought_dump")
    object CalmCompletion : Screen("calm_completion")
    object NightReset : Screen("night_reset")
    object UrgeHit : Screen("urge_hit")
    object Accountability : Screen("accountability")
    object UrgeComplete : Screen("urge_complete")
    object AlarmSetup : Screen("alarm_setup")
}
