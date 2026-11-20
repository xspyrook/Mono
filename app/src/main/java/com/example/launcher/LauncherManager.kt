package com.example.launcher

import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import com.example.model.DeviceStatus
import com.example.stability.StabilityMonitor

class LauncherManager(private val context: Context) {

    fun launchApp(packageName: String, activityName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(packageName, activityName)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )
            }
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            // Fallback to package launch intent if explicit component failed
            try {
                val fallback = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (fallback != null) {
                    context.startActivity(fallback)
                    true
                } else {
                    StabilityMonitor.recordHandledError("LaunchApp:$packageName", t)
                    false
                }
            } catch (err: Throwable) {
                StabilityMonitor.recordHandledError("LaunchFallback:$packageName", err)
                false
            }
        }
    }

    fun isDefaultLauncher(): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            resolveInfo?.activityInfo?.packageName == context.packageName
        }.getOrDefault(false)
    }

    fun openDefaultLauncherSettings(activity: Activity? = null) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && activity != null) {
                val roleManager = activity.getSystemService(RoleManager::class.java)
                if (roleManager != null &&
                    roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                    !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                ) {
                    @Suppress("DEPRECATION")
                    activity.startActivityForResult(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME),
                        REQUEST_HOME_ROLE
                    )
                    return
                }
            }
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (t: Throwable) {
            StabilityMonitor.recordHandledError("DefaultHomeSettings", t)
            openSystemSettings()
        }
    }

    fun openAppInfo(packageName: String) {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure { StabilityMonitor.recordHandledError("OpenAppInfo", it) }
    }

    fun uninstallApp(packageName: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure { StabilityMonitor.recordHandledError("UninstallApp", it) }
    }

    fun openWifiSettings() {
        runCatching {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure { err -> StabilityMonitor.recordHandledError("WifiSettings", err) }
        }
    }

    fun openBluetoothSettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { StabilityMonitor.recordHandledError("BluetoothSettings", it) }
    }

    fun openDisplaySettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { StabilityMonitor.recordHandledError("DisplaySettings", it) }
    }

    fun openSoundSettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { StabilityMonitor.recordHandledError("SoundSettings", it) }
    }

    fun openSystemSettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { StabilityMonitor.recordHandledError("SystemSettings", it) }
    }

    fun openWebSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        runCatching {
            val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(trimmed)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure { StabilityMonitor.recordHandledError("WebSearch", it) }
    }

    fun readDeviceStatus(currentBrightness: Float): DeviceStatus {
        val batteryStatus = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 100
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280

        var isWifi = false
        var isCellular = false
        var isOnline = false
        runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(network)
            if (caps != null) {
                isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                isOnline = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }

        val audioManager = runCatching {
            context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        }.getOrNull()

        val mediaVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 5
        val maxMediaVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
        val ringVol = audioManager?.getStreamVolume(AudioManager.STREAM_RING) ?: 4
        val maxRingVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_RING)?.coerceAtLeast(1) ?: 7

        return DeviceStatus(
            batteryLevel = batteryPct,
            isCharging = isCharging,
            batteryTempC = tempTenths / 10f,
            isWifiConnected = isWifi,
            isCellularConnected = isCellular,
            isOnline = isOnline,
            mediaVolume = mediaVol,
            maxMediaVolume = maxMediaVol,
            ringVolume = ringVol,
            maxRingVolume = maxRingVol,
            windowBrightness = currentBrightness
        )
    }

    fun setMediaVolume(value: Int) {
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, value.coerceIn(0, max), 0)
        }.onFailure { StabilityMonitor.recordHandledError("SetMediaVolume", it) }
    }

    fun setRingVolume(value: Int) {
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val max = am.getStreamMaxVolume(AudioManager.STREAM_RING)
            am.setStreamVolume(AudioManager.STREAM_RING, value.coerceIn(0, max), 0)
        }.onFailure { StabilityMonitor.recordHandledError("SetRingVolume", it) }
    }

    companion object {
        const val REQUEST_HOME_ROLE = 4102
    }
}
