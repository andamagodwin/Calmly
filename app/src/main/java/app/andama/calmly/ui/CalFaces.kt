package app.andama.calmly.ui

import androidx.annotation.DrawableRes
import app.andama.calmly.R
import app.andama.calmly.data.CalMood

/**
 * The bridge from Cal's emotional state to pixels. Kept out of the `data`
 * package so the mood rules themselves stay free of Android resources.
 */
@get:DrawableRes
val CalMood.faceRes: Int
    get() = when (this) {
        CalMood.SAD -> R.drawable.face_sad
        CalMood.STRUGGLING -> R.drawable.face_struggling
        CalMood.NEUTRAL -> R.drawable.face_neutral
        CalMood.HAPPY -> R.drawable.face_happy
        CalMood.FIERCE -> R.drawable.face_fierce
    }

/**
 * The colour that travels with each face. Raw ARGB rather than a Compose
 * `Color` because RemoteViews (the widgets) can only take an int, and the two
 * surfaces must not drift apart.
 */
val CalMood.accentArgb: Int
    get() = when (this) {
        CalMood.SAD -> 0xFFDC3545.toInt()        // DangerRed
        CalMood.STRUGGLING -> 0xFFF0692E.toInt() // BrutalOrange
        CalMood.NEUTRAL -> 0xFFFFC107.toInt()    // WarningAmber
        CalMood.HAPPY -> 0xFF17A2B8.toInt()      // PrimaryBlue
        CalMood.FIERCE -> 0xFF9B78E8.toInt()     // SoftPurple
    }

/** The word under the face on the check-in scale. */
val CalMood.label: String
    get() = when (this) {
        CalMood.SAD -> "Terrible"
        CalMood.STRUGGLING -> "Struggling"
        CalMood.NEUTRAL -> "Okay"
        CalMood.HAPPY -> "Good"
        CalMood.FIERCE -> "Strong"
    }
