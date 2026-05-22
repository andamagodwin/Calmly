package app.andama.calmly.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.andama.calmly.R
import kotlin.random.Random

class QuoteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        showQuoteNotification(context)
    }
}

private val brutalQuotes = listOf(
    "You're one decision away from a completely different life.",
    "Discipline is choosing between what you want NOW and what you want MOST.",
    "Every time you resist, your willpower gets stronger. Like a muscle.",
    "The pain of discipline weighs ounces. The pain of regret weighs tons.",
    "You are not your urges. You are the one who decides what to do with them.",
    "Nobody is coming to save you. You have to save yourself.",
    "A year from now you'll wish you started today.",
    "You didn't come this far to only come this far.",
    "The person you're becoming requires the sacrifice of who you've been.",
    "Hard choices, easy life. Easy choices, hard life.",
    "Your addiction wants you alone. Don't give it that.",
    "90 days. That's what it takes to rewire. Are you in or out?",
    "The urge is strongest right before it breaks. Hold the line.",
    "You're not giving something up. You're gaining freedom.",
    "What would the best version of you do right now?",
    "Stop negotiating with your weakest self.",
    "Porn didn't solve any of your problems. It just added new ones.",
    "Every single day clean is a victory. Celebrate it.",
    "You are training your brain right now. What are you training it to do?",
    "The chains of habit are too light to be felt until they're too heavy to be broken.",
    "Regret from inaction lasts longer than discomfort from discipline.",
    "This moment will pass. Your self-respect won't come back as easily."
)

fun showQuoteNotification(context: Context) {
    val channelId = "calmly_quotes_channel"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Daily Motivation",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Motivational reminders throughout the day"
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    val quote = brutalQuotes[Random.nextInt(brutalQuotes.size)]

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("Calmly")
        .setContentText(quote)
        .setStyle(NotificationCompat.BigTextStyle().bigText(quote))
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(Random.nextInt(10000), notification)
}
