package app.andama.calmly.block

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import app.andama.calmly.service.AppBlockerService

/** Everything the App Lock UI needs to know about the enforcement service. */
object AppLock {

    /** One installed, user-launchable app. */
    data class InstalledApp(val packageName: String, val label: String)

    /**
     * Is our accessibility service actually switched on? Granting it is a manual
     * trip to system settings, so the UI has to be able to tell whether it stuck.
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context, AppBlockerService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        // The setting is a colon-joined list of component names.
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        for (component in splitter) {
            if (component.equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    fun accessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Every app with a launcher entry, minus Calmly itself, sorted by name. We
     * only offer things the user can actually open — blocking a background service
     * package would be meaningless.
     */
    fun launchableApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .map { it.activityInfo.packageName }
            .filter { it != context.packageName }
            .distinct()
            .mapNotNull { pkg ->
                runCatching {
                    val label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                    InstalledApp(pkg, label)
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
