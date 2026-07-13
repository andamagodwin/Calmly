package app.andama.calmly.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette:
//   #6f42c1 purple (hero) · #17a2b8 teal · #ffc107 amber · #dc3545 red · #d5c8ff lavender
// Surfaces are ink with a purple cast — derived from the hero colour so the
// backgrounds and accents read as one system rather than accents pasted on grey.
val DeepBackground = Color(0xFF0E0A1A)
val SoftBackground = Color(0xFF171129)
val CardBackground = Color(0xFF1F1735)
val ElevatedBackground = Color(0xFF2A2046)
val BorderSubtle = Color(0xFF382C58)

// Core accents
val PrimaryBlue = Color(0xFF17A2B8)     // teal — the "you're okay" colour
val SoftPurple = Color(0xFF9B78E8)      // hero purple lifted for legibility on dark
val HeroPurple = Color(0xFF6F42C1)
val Lavender = Color(0xFFD5C8FF)
val CalmGrey = Color(0xFF5E5580)

// Text — lavender-tinted whites
val TextPrimary = Color(0xFFEFEAFB)
val TextSecondary = Color(0xFFA79CC8)
val TextTertiary = Color(0xFF6F6493)

// The signature gradient: hero purple -> teal.
val AccentGradientStart = Color(0xFF6F42C1)
val AccentGradientEnd = Color(0xFF17A2B8)

// Urge/danger gradient: red -> amber. Fire, not a pastel warning.
val DangerRed = Color(0xFFDC3545)
val BrutalOrange = Color(0xFFF0692E)

val WarningAmber = Color(0xFFFFC107)
val SuccessGreen = Color(0xFF17A2B8)    // no green in the brand palette; teal carries "good"

// Milestone tiers for the streak ring — amber seed to lavender mastery,
// brightening as the streak grows.
val StreakSeed = Color(0xFFFFC107)
val StreakSprout = Color(0xFF17A2B8)
val StreakRooted = Color(0xFF9B78E8)
val StreakForged = Color(0xFFD5C8FF)
