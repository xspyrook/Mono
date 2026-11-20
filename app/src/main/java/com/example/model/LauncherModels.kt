package com.example.model

enum class AppLanguage(val code: String) {
    ARABIC("ar"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ARABIC
    }
}

enum class AppCategory {
    ALL,
    COMMUNICATION,
    SOCIAL,
    MEDIA,
    TOOLS,
    PRODUCTIVITY,
    GAMES,
    SYSTEM,
    OTHER
}

enum class ClockStyle {
    EDITORIAL,
    GEOMETRIC,
    MONOSPACE
}

enum class BackgroundStyle {
    PURE_BLACK,
    OBSIDIAN_DEPTH,
    OBSIDIAN_WAVE,
    MONOLITH_GRID
}

enum class DockStyle {
    FLOATING_PILL,
    FULL_GLASS,
    HIDDEN
}

enum class TransitionSpeed(val durationMultiplier: Float) {
    INSTANT(0.45f),
    SMOOTH(1.0f),
    CINEMATIC(1.45f)
}

enum class GridDensity(val columns: Int, val label: String) {
    COMPACT_3X4(3, "3 × 4"),
    BALANCED_4X5(4, "4 × 5"),
    DENSE_5X6(5, "5 × 6")
}

enum class LauncherOverlay {
    NONE,
    APP_DRAWER,
    SEARCH,
    CONTROL_PANEL,
    SETTINGS
}

data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val category: AppCategory,
    val isSystemApp: Boolean,
    val firstInstallTime: Long = 0L
) {
    val componentKey: String get() = "$packageName/$activityName"
}

data class AppRef(
    val packageName: String,
    val activityName: String,
    val label: String
) {
    val componentKey: String get() = "$packageName/$activityName"
}

sealed interface HomeItem {
    val id: String

    data class AppShortcut(
        override val id: String,
        val app: AppRef
    ) : HomeItem

    data class Folder(
        override val id: String,
        val name: String,
        val apps: List<AppRef>
    ) : HomeItem
}

data class LauncherSettings(
    val language: AppLanguage = AppLanguage.ARABIC,
    val iconSizeDp: Float = 56f,
    val gridDensity: GridDensity = GridDensity.BALANCED_4X5,
    val glassOpacity: Float = 0.12f,
    val enableBlur: Boolean = true,
    val transitionSpeed: TransitionSpeed = TransitionSpeed.SMOOTH,
    val clockStyle: ClockStyle = ClockStyle.EDITORIAL,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.PURE_BLACK,
    val dockStyle: DockStyle = DockStyle.FLOATING_PILL,
    val sortAlphabetically: Boolean = true
)

data class DeviceStatus(
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val batteryTempC: Float = 28.0f,
    val isWifiConnected: Boolean = false,
    val isCellularConnected: Boolean = false,
    val isOnline: Boolean = false,
    val mediaVolume: Int = 5,
    val maxMediaVolume: Int = 15,
    val ringVolume: Int = 5,
    val maxRingVolume: Int = 7,
    val windowBrightness: Float = 0.7f
)

data class StabilitySnapshot(
    val uptimeSeconds: Long = 0L,
    val heapUsedMb: Float = 0f,
    val heapMaxMb: Float = 0f,
    val cachedIconCount: Int = 0,
    val iconCacheHits: Long = 0L,
    val iconCacheMisses: Long = 0L,
    val loadedAppsCount: Int = 0,
    val handledErrorsCount: Int = 0,
    val lastErrorSummary: String? = null
)
