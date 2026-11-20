package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localization.LocalAppStrings
import com.example.localization.stringsFor
import com.example.model.AppLanguage
import com.example.model.LauncherOverlay
import com.example.stability.StabilityMonitor
import com.example.ui.animation.AnimationManager
import com.example.ui.components.GlassSurface
import com.example.ui.components.MonoWallpaperBackground
import com.example.ui.screens.AppDrawerScreen
import com.example.ui.screens.ControlPanelScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MonoColors
import com.example.ui.theme.MonoGlassTheme
import com.example.viewmodel.LauncherViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val strings = stringsFor(state.settings.language)
            val layoutDirection = if (state.settings.language == AppLanguage.ARABIC) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            // Lifecycle-safe BroadcastReceiver for package install/uninstall & battery changes
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val packageFilter = IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_REMOVED)
                    addAction(Intent.ACTION_PACKAGE_CHANGED)
                    addDataScheme("package")
                }
                val packageReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        viewModel.refreshApps()
                    }
                }

                val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val batteryReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        viewModel.refreshSystemStatus()
                    }
                }

                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        registerReceiver(packageReceiver, packageFilter, RECEIVER_NOT_EXPORTED)
                        registerReceiver(batteryReceiver, batteryFilter, RECEIVER_NOT_EXPORTED)
                    } else {
                        registerReceiver(packageReceiver, packageFilter)
                        registerReceiver(batteryReceiver, batteryFilter)
                    }
                }.onFailure { StabilityMonitor.recordHandledError("RegisterReceiver", it) }

                val lifecycleObserver = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.refreshSystemStatus()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                    runCatching { unregisterReceiver(packageReceiver) }
                    runCatching { unregisterReceiver(batteryReceiver) }
                }
            }

            // Auto-dismiss transient glass toast
            LaunchedEffect(state.toastMessage) {
                if (state.toastMessage != null) {
                    delay(2200L)
                    viewModel.clearToast()
                }
            }

            CompositionLocalProvider(
                LocalAppStrings provides strings,
                LocalLayoutDirection provides layoutDirection
            ) {
                MonoGlassTheme {
                    MonoGlassRoot(
                        viewModel = viewModel,
                        activity = this@MainActivity
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Pressing system Home button while already in Launcher returns cleanly to Home
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.closeOverlay()
            viewModel.setEditMode(false)
        }
    }
}

@Composable
private fun MonoGlassRoot(
    viewModel: LauncherViewModel,
    activity: ComponentActivity
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    val settings = state.settings
    val overlayActive = state.activeOverlay != LauncherOverlay.NONE

    Box(modifier = Modifier.fillMaxSize()) {
        // Wallpaper Layer (dims/blurs softly behind open overlays)
        MonoWallpaperBackground(
            style = settings.backgroundStyle,
            dimmed = overlayActive,
            enableBlur = settings.enableBlur
        )

        // Base Home Screen
        HomeScreen(
            state = state,
            iconProcessor = viewModel.iconProcessor,
            onLaunchApp = { app -> viewModel.launchApp(app, strings.cannotLaunchApp) },
            onOpenOverlay = viewModel::openOverlay,
            onSetEditMode = viewModel::setEditMode,
            onMoveHomeItem = viewModel::moveHomeItem,
            onRemoveHomeItem = viewModel::removeHomeItem,
            onRemoveDockItem = viewModel::removeAppFromDock,
            onAddAppToHome = { app -> viewModel.addAppToHome(app, strings.addedToHomeMsg) },
            onPinAppToDock = { app -> viewModel.pinAppToDock(app, strings.addedToDockMsg) },
            onCreateFolder = viewModel::createFolder,
            onOpenFolder = viewModel::openFolder,
            onRenameFolder = viewModel::renameFolder,
            onToggleAppInFolder = viewModel::toggleAppInFolder
        )

        // App Drawer Overlay
        AnimatedVisibility(
            visible = state.activeOverlay == LauncherOverlay.APP_DRAWER,
            enter = AnimationManager.drawerEnter(settings.transitionSpeed),
            exit = AnimationManager.drawerExit(settings.transitionSpeed)
        ) {
            AppDrawerScreen(
                state = state,
                iconProcessor = viewModel.iconProcessor,
                onClose = viewModel::closeOverlay,
                onQueryChange = viewModel::updateDrawerQuery,
                onSelectCategory = viewModel::selectCategory,
                onToggleShowHidden = viewModel::toggleShowHiddenInDrawer,
                onLaunchApp = { app -> viewModel.launchApp(app, strings.cannotLaunchApp) },
                onAddToHome = { app -> viewModel.addAppToHome(app, strings.addedToHomeMsg) },
                onPinToDock = { app -> viewModel.pinAppToDock(app, strings.addedToDockMsg) },
                onToggleHidden = { app, hide ->
                    viewModel.setAppHidden(app.packageName, hide, if (hide) strings.hiddenMsg else null)
                },
                onOpenAppInfo = viewModel.launcherManager::openAppInfo,
                onUninstallApp = viewModel.launcherManager::uninstallApp
            )
        }

        // Spotlight Search Overlay
        AnimatedVisibility(
            visible = state.activeOverlay == LauncherOverlay.SEARCH,
            enter = AnimationManager.modalEnter(settings.transitionSpeed),
            exit = AnimationManager.modalExit(settings.transitionSpeed)
        ) {
            SearchScreen(
                state = state,
                iconProcessor = viewModel.iconProcessor,
                onClose = viewModel::closeOverlay,
                onQueryChange = viewModel::updateSearchQuery,
                onLaunchApp = { app -> viewModel.launchApp(app, strings.cannotLaunchApp) },
                onOpenWebSearch = viewModel.launcherManager::openWebSearch,
                onOpenWifi = viewModel.launcherManager::openWifiSettings,
                onOpenBluetooth = viewModel.launcherManager::openBluetoothSettings,
                onOpenDisplay = viewModel.launcherManager::openDisplaySettings,
                onOpenSettings = { viewModel.openOverlay(LauncherOverlay.SETTINGS) }
            )
        }

        // Quick Control Panel Overlay
        AnimatedVisibility(
            visible = state.activeOverlay == LauncherOverlay.CONTROL_PANEL,
            enter = AnimationManager.controlPanelEnter(settings.transitionSpeed),
            exit = AnimationManager.controlPanelExit(settings.transitionSpeed)
        ) {
            ControlPanelScreen(
                state = state,
                onClose = viewModel::closeOverlay,
                onOpenWifi = viewModel.launcherManager::openWifiSettings,
                onOpenBluetooth = viewModel.launcherManager::openBluetoothSettings,
                onOpenDisplay = viewModel.launcherManager::openDisplaySettings,
                onOpenSound = viewModel.launcherManager::openSoundSettings,
                onOpenSystemSettings = viewModel.launcherManager::openSystemSettings,
                onOpenDefaultLauncher = { viewModel.launcherManager.openDefaultLauncherSettings(activity) },
                onMediaVolumeChange = viewModel::setMediaVolume,
                onRingVolumeChange = viewModel::setRingVolume,
                onBrightnessChange = { value -> viewModel.setWindowBrightness(activity, value) }
            )
        }

        // Personalization & Stability Settings Overlay
        AnimatedVisibility(
            visible = state.activeOverlay == LauncherOverlay.SETTINGS,
            enter = AnimationManager.modalEnter(settings.transitionSpeed),
            exit = AnimationManager.modalExit(settings.transitionSpeed)
        ) {
            SettingsScreen(
                state = state,
                onClose = viewModel::closeOverlay,
                onOpenDefaultLauncher = { viewModel.launcherManager.openDefaultLauncherSettings(activity) },
                onLanguageChange = viewModel::updateLanguage,
                onIconSizeChange = viewModel::updateIconSize,
                onGridDensityChange = viewModel::updateGridDensity,
                onGlassOpacityChange = viewModel::updateGlassOpacity,
                onBlurToggle = viewModel::updateEnableBlur,
                onTransitionSpeedChange = viewModel::updateTransitionSpeed,
                onClockStyleChange = viewModel::updateClockStyle,
                onBackgroundStyleChange = viewModel::updateBackgroundStyle,
                onDockStyleChange = viewModel::updateDockStyle,
                onSortAlphabeticallyChange = viewModel::updateSortAlphabetically,
                onUnhideApp = { pkg -> viewModel.setAppHidden(pkg, false) },
                onResetLayout = viewModel::resetHomeLayout
            )
        }

        // Glass Toast Pill
        AnimatedVisibility(
            visible = state.toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 94.dp)
        ) {
            state.toastMessage?.let { msg ->
                GlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    glassOpacity = 0.24f,
                    elevated = true
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelMedium,
                        color = MonoColors.PureWhite,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
