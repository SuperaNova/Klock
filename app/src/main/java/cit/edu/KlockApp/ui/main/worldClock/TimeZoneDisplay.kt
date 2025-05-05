package cit.edu.KlockApp.ui.main.worldClock

/**
 * Data class to hold both the original TimeZone ID and its user-friendly display name.
 */
data class TimeZoneDisplay(
    val id: String,       // e.g., "Asia/Manila"
    val displayName: String // e.g., "Manila"
) 