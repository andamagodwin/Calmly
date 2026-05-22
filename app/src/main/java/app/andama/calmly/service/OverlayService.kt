package app.andama.calmly.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import app.andama.calmly.R

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var countdownTimer: CountDownTimer? = null

    companion object {
        const val CHANNEL_ID = "calmly_overlay_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "START_OVERLAY"
        const val ACTION_STOP = "STOP_OVERLAY"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_MODE = "extra_mode"

        private const val DEFAULT_DURATION = 15 * 60 * 1000L
        private const val EXIT_PHRASE = "I am choosing weakness"

        fun startService(context: Context, durationMs: Long = DEFAULT_DURATION, mode: String = "calm") {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION, durationMs)
                putExtra(EXTRA_MODE, mode)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val duration = intent.getLongExtra(EXTRA_DURATION, DEFAULT_DURATION)
                val mode = intent.getStringExtra(EXTRA_MODE) ?: "calm"
                startForeground(NOTIFICATION_ID, createNotification())
                showOverlay(duration, mode)
            }
            ACTION_STOP -> {
                countdownTimer?.cancel()
                removeOverlay()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Calmly Focus Mode",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Focus mode active - stay strong"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Calmly - Lock Active")
            .setContentText("You're locked in. The timer will free you.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun showOverlay(durationMs: Long, mode: String) {
        if (overlayView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.CENTER

        val isUrgeMode = mode == "urge"

        val timerText = TextView(this).apply {
            text = formatTime(durationMs)
            setTextColor(if (isUrgeMode) Color.parseColor("#FF4757") else Color.parseColor("#58A6FF"))
            textSize = 48f
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val statusText = TextView(this).apply {
            text = if (isUrgeMode) "LOCKED. RIDE IT OUT." else "FOCUS MODE ACTIVE"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val messageText = TextView(this).apply {
            text = if (isUrgeMode)
                "This overlay will disappear when the timer ends.\nNo shortcuts. No cheats. You're stronger than this."
            else
                "This overlay will disappear when the timer ends.\nStay with the process."
            setTextColor(Color.parseColor("#8B949E"))
            textSize = 14f
            gravity = Gravity.CENTER
        }

        val exitLabel = TextView(this).apply {
            text = if (isUrgeMode)
                "To quit early, type: \"$EXIT_PHRASE\""
            else
                "To exit early, type: \"$EXIT_PHRASE\""
            setTextColor(Color.parseColor("#484F58"))
            textSize = 12f
            gravity = Gravity.CENTER
        }

        val exitInput = EditText(this).apply {
            hint = "Type the phrase to exit..."
            setHintTextColor(Color.parseColor("#484F58"))
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#21262D"))
            setPadding(32, 24, 32, 24)
        }

        val exitStatus = TextView(this).apply {
            text = ""
            setTextColor(Color.parseColor("#FF4757"))
            textSize = 12f
            gravity = Gravity.CENTER
        }

        exitInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val typed = s?.toString()?.trim()?.lowercase() ?: ""
                if (typed == EXIT_PHRASE.lowercase()) {
                    stopService(this@OverlayService)
                } else if (typed.isNotEmpty()) {
                    exitStatus.text = "That's not right. Keep trying or keep waiting."
                }
            }
        })

        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F0000000"))
            gravity = Gravity.CENTER
            setPadding(64, 0, 64, 0)

            addView(statusText, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 48 })

            addView(timerText, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 32 })

            addView(messageText, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 80 })

            addView(exitLabel, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 })

            addView(exitInput, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 })

            addView(exitStatus, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }

        windowManager?.addView(overlayView, layoutParams)

        countdownTimer = object : CountDownTimer(durationMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timerText.text = formatTime(millisUntilFinished)
            }

            override fun onFinish() {
                timerText.text = "00:00"
                statusText.text = "TIME'S UP. YOU'RE FREE."
                statusText.setTextColor(Color.parseColor("#2ED573"))
                removeOverlay()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        removeOverlay()
    }
}
