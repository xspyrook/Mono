package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.example.engine.IconProcessor
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.stability.StabilityMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

class AppRepository(
    private val context: Context,
    val iconProcessor: IconProcessor
) {
    suspend fun loadInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val selfPkg = context.packageName
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    launcherIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, 0)
            }
        }.onFailure { err ->
            StabilityMonitor.recordHandledError("QueryApps", err)
        }.getOrDefault(emptyList())

        val seenKeys = HashSet<String>(resolveInfos.size)
        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }

        val apps = resolveInfos.mapNotNull { info ->
            runCatching {
                val activityInfo = info.activityInfo ?: return@runCatching null
                val pkg = activityInfo.packageName ?: return@runCatching null
                val act = activityInfo.name ?: return@runCatching null
                if (pkg == selfPkg) return@runCatching null

                val key = "$pkg/$act"
                if (!seenKeys.add(key)) return@runCatching null

                val label = info.loadLabel(pm)?.toString()?.trim()
                    ?.takeIf { it.isNotEmpty() } ?: pkg

                val appInfo = activityInfo.applicationInfo
                val isSystem = appInfo != null &&
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                val pkgInfo = runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(pkg, 0)
                    }
                }.getOrNull()

                val category = classifyApp(pkg, label, appInfo)

                AppInfo(
                    packageName = pkg,
                    activityName = act,
                    label = label,
                    category = category,
                    isSystemApp = isSystem,
                    firstInstallTime = pkgInfo?.firstInstallTime ?: 0L
                )
            }.onFailure { err ->
                StabilityMonitor.recordHandledError("ParseResolveInfo", err)
            }.getOrNull()
        }.sortedWith { a, b -> collator.compare(a.label, b.label) }

        StabilityMonitor.updateLoadedAppsCount(apps.size)
        apps
    }

    private fun classifyApp(
        packageName: String,
        label: String,
        appInfo: ApplicationInfo?
    ): AppCategory {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo != null) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.MEDIA
                ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
                ApplicationInfo.CATEGORY_MAPS,
                ApplicationInfo.CATEGORY_NEWS -> return AppCategory.TOOLS
            }
        }

        val lower = "$packageName ${label.lowercase(Locale.ROOT)}"
        return when {
            listOf("dialer", "phone", "contacts", "messaging", "mms", "sms", "whatsapp", "telegram", "signal", "viber", "gmail", "mail", "call")
                .any { lower.contains(it) } -> AppCategory.COMMUNICATION
            listOf("instagram", "facebook", "twitter", "x.", "tiktok", "snapchat", "reddit", "discord", "threads", "linkedin")
                .any { lower.contains(it) } -> AppCategory.SOCIAL
            listOf("youtube", "spotify", "music", "video", "camera", "gallery", "photos", "netflix", "podcast", "vlc", "player", "sound")
                .any { lower.contains(it) } -> AppCategory.MEDIA
            listOf("docs", "sheets", "drive", "calendar", "notes", "keep", "notion", "office", "pdf", "word", "excel", "slack", "zoom")
                .any { lower.contains(it) } -> AppCategory.PRODUCTIVITY
            listOf("calculator", "clock", "alarm", "weather", "compass", "files", "browser", "chrome", "firefox", "maps", "translate")
                .any { lower.contains(it) } -> AppCategory.TOOLS
            listOf("settings", "system", "security", "updater", "play", "vending")
                .any { lower.contains(it) } -> AppCategory.SYSTEM
            else -> AppCategory.OTHER
        }
    }
}
