package app.andama.calmly.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import app.andama.calmly.block.BlockActivity
import app.andama.calmly.data.CalmlyTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * The enforcement arm. An accessibility service is the only thing on Android that
 * can watch which app just came to the foreground and slam a chosen one shut, and
 * it's exactly what the user asked for: authority to lock apps, and to keep doing
 * it after a reboot — the OS re-enables accessibility services automatically once
 * the user has granted them, so nothing here needs to survive death on its own.
 *
 * When a blocked app opens we go straight to the home screen (which always works,
 * even if a background-activity-start would be refused) and then throw up
 * [BlockActivity] with Cal's game face. Relaunch the app and it happens again.
 * There is deliberately no "5 more minutes".
 */
class AppBlockerService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // The live set of blocked packages, kept current by observing DataStore so an
    // edit in the picker takes effect without toggling the service off and on.
    private val blocked = MutableStateFlow<Set<String>>(emptySet())

    // Debounce: window-state events fire in bursts, and re-launching the block
    // screen on every one of them would strobe. One block per app per short window.
    private var lastBlockedPackage: String? = null
    private var lastBlockedAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val tracker = CalmlyTracker(applicationContext)
        scope.launch {
            tracker.blockedPackagesFlow.collect { blocked.value = it }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // Never fence in our own app, the launcher, or the system UI — only the
        // packages the user explicitly chose.
        if (pkg == packageName) return
        if (pkg !in blocked.value) {
            lastBlockedPackage = null
            return
        }

        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPackage && now - lastBlockedAt < DEBOUNCE_MS) return
        lastBlockedPackage = pkg
        lastBlockedAt = now

        // Kick them out first — this is the part that always works — then show Cal.
        performGlobalAction(GLOBAL_ACTION_HOME)

        val intent = Intent(this, BlockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(BlockActivity.EXTRA_PACKAGE, pkg)
        }
        runCatching { startActivity(intent) }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        scope.cancel()
        return super.onUnbind(intent)
    }

    companion object {
        private const val DEBOUNCE_MS = 1500L
    }
}
