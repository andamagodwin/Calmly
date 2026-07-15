package app.andama.calmly.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.andama.calmly.R
import app.andama.calmly.block.AppLock
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.ui.components.*
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppLockScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tracker = remember { CalmlyTracker(context) }
    val scope = rememberCoroutineScope()

    var serviceEnabled by remember { mutableStateOf(AppLock.isAccessibilityEnabled(context)) }
    var apps by remember { mutableStateOf<List<AppLock.InstalledApp>>(emptyList()) }
    var blocked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }

    // Enabling the service is a round trip to system settings, so re-check the
    // moment they come back rather than stranding them on the enable prompt.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serviceEnabled = AppLock.isAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        blocked = tracker.getBlockedPackages()
        apps = withContext(Dispatchers.IO) { AppLock.launchableApps(context) }
        loading = false
    }

    CalmlyScreen(title = "App Lock", onBack = onBack, scrollable = false) {
        EnterBounce {
            Column {
                Text(
                    text = "Lock the apps that get you.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Cal slams these shut the instant they open — day or night, " +
                            "and it keeps working after a reboot. No timer, no “just once”.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (!serviceEnabled) {
            EnableServiceCard(
                onEnable = { context.startActivity(AppLock.accessibilitySettingsIntent()) }
            )
            Spacer(Modifier.height(16.dp))
        }

        val blockedCount = blocked.size
        if (blockedCount > 0) {
            Text(
                text = if (blockedCount == 1) "1 app locked" else "$blockedCount apps locked",
                style = MaterialTheme.typography.labelLarge,
                color = if (serviceEnabled) SuccessGreen else WarningAmber
            )
            if (!serviceEnabled) {
                Text(
                    text = "Turn the service on above or these locks do nothing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarningAmber
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        when {
            loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = PrimaryBlue) }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    val isBlocked = app.packageName in blocked
                    AppRow(
                        app = app,
                        blocked = isBlocked,
                        onToggle = {
                            val next = if (isBlocked) blocked - app.packageName
                            else blocked + app.packageName
                            blocked = next
                            scope.launch { tracker.setBlockedPackages(next) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EnableServiceCard(onEnable: () -> Unit) {
    CalmlyCard(containerColor = ElevatedBackground) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.face_fierce),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Give Cal the authority",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "App Lock runs on an accessibility service — the only thing Android " +
                        "lets watch for a blocked app and close it. Calmly reads only which app " +
                        "opened, never your screen. Turn on “Calmly” in the next screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 21.sp
            )
            Spacer(Modifier.height(16.dp))
            CalmlyButton(text = "Enable App Lock", onClick = onEnable)
        }
    }
}

@Composable
private fun AppRow(
    app: AppLock.InstalledApp,
    blocked: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching {
            val px = (44 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
            context.packageManager.getApplicationIcon(app.packageName)
                .toBitmap(width = px, height = px).asImageBitmap()
        }.getOrNull()
    }

    CalmlyCard(containerColor = if (blocked) ElevatedBackground else CardBackground, onClick = onToggle) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SoftBackground)
                )
            }

            Spacer(Modifier.width(14.dp))

            Text(
                text = app.label,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = blocked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextPrimary,
                    checkedTrackColor = DangerRed,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = SoftBackground
                )
            )
        }
    }
}
