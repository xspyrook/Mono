package com.example.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.PreferenceManager
import com.example.engine.IconProcessor
import com.example.launcher.LauncherManager
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.model.AppLanguage
import com.example.model.AppRef
import com.example.model.BackgroundStyle
import com.example.model.ClockStyle
import com.example.model.DeviceStatus
import com.example.model.DockStyle
import com.example.model.GridDensity
import com.example.model.HomeItem
import com.example.model.LauncherOverlay
import com.example.model.LauncherSettings
import com.example.model.StabilitySnapshot
import com.example.model.TransitionSpeed
import com.example.stability.StabilityMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class LauncherUiState(
    val isLoading: Boolean = true,
    val settings: LauncherSettings = LauncherSettings(),
    val allApps: List<AppInfo> = emptyList(),
    val homeItems: List<HomeItem> = emptyList(),
    val dockItems: List<AppRef> = emptyList(),
    val hiddenPackages: Set<String> = emptySet(),
    val recentKeys: List<String> = emptyList(),
    val activeOverlay: LauncherOverlay = LauncherOverlay.NONE,
    val isEditMode: Boolean = false,
    val openFolderId: String? = null,
    val drawerQuery: String = "",
    val selectedCategory: AppCategory = AppCategory.ALL,
    val showHiddenInDrawer: Boolean = false,
    val searchQuery: String = "",
    val deviceStatus: DeviceStatus = DeviceStatus(),
    val isDefaultLauncher: Boolean = false,
    val stability: StabilitySnapshot = StabilitySnapshot(),
    val toastMessage: String? = null
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    val iconProcessor = IconProcessor(application.applicationContext)
    private val appRepository = AppRepository(application.applicationContext, iconProcessor)
    private val preferenceManager = PreferenceManager(application.applicationContext)
    val launcherManager = LauncherManager(application.applicationContext)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private var windowBrightness = 0.75f

    init {
        StabilityMonitor.installUncaughtExceptionGuard()
        observeDataStore()
        observeStability()
        refreshApps()
        refreshSystemStatus()
    }

    private fun observeStability() {
        viewModelScope.launch {
            StabilityMonitor.snapshot.collect { snap ->
                _uiState.update { it.copy(stability = snap) }
            }
        }
    }

    private fun observeDataStore() {
        viewModelScope.launch {
            combine(
                preferenceManager.settingsFlow,
                preferenceManager.homeItemsFlow,
                preferenceManager.dockItemsFlow,
                preferenceManager.hiddenPackagesFlow,
                preferenceManager.recentKeysFlow
            ) { settings, homeItems, dockItems, hidden, recent ->
                PersistedBundle(settings, homeItems, dockItems, hidden, recent)
            }.collect { bundle ->
                val currentApps = _uiState.value.allApps
                val resolvedHome = bundle.homeItems
                val resolvedDock = bundle.dockItems

                _uiState.update { state ->
                    state.copy(
                        settings = bundle.settings,
                        homeItems = resolvedHome ?: state.homeItems,
                        dockItems = resolvedDock ?: state.dockItems,
                        hiddenPackages = bundle.hidden,
                        recentKeys = bundle.recent
                    )
                }

                if ((resolvedHome == null || resolvedDock == null) && currentApps.isNotEmpty()) {
                    seedInitialLayout(currentApps)
                }
            }
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            val apps = appRepository.loadInstalledApps()
            val installedMap = apps.associateBy { it.componentKey }
            val currentState = _uiState.value

            _uiState.update { state ->
                val cleanedHome = state.homeItems.mapNotNull { item ->
                    when (item) {
                        is HomeItem.AppShortcut -> {
                            val match = installedMap[item.app.componentKey]
                            if (match != null) {
                                item.copy(app = match.toRef())
                            } else if (apps.isEmpty()) {
                                item
                            } else {
                                null
                            }
                        }
                        is HomeItem.Folder -> {
                            val validApps = item.apps.mapNotNull { ref ->
                                installedMap[ref.componentKey]?.toRef()
                            }
                            item.copy(apps = validApps)
                        }
                    }
                }
                val cleanedDock = state.dockItems.mapNotNull { ref ->
                    installedMap[ref.componentKey]?.toRef() ?: if (apps.isEmpty()) ref else null
                }

                state.copy(
                    isLoading = false,
                    allApps = apps,
                    homeItems = cleanedHome,
                    dockItems = cleanedDock
                )
            }

            if (currentState.homeItems.isEmpty() && currentState.dockItems.isEmpty() && apps.isNotEmpty()) {
                seedInitialLayout(apps)
            }
        }
    }

    private fun seedInitialLayout(apps: List<AppInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            val dockCandidates = pickPreferredApps(
                apps,
                listOf("dialer", "phone", "messaging", "sms", "mms", "whatsapp", "chrome", "browser", "camera")
            ).take(4)

            val dockKeys = dockCandidates.map { it.componentKey }.toSet()
            val remaining = apps.filterNot { it.componentKey in dockKeys }

            val homeApps = remaining.take(8).map { app ->
                HomeItem.AppShortcut(
                    id = "app_${app.componentKey}",
                    app = app.toRef()
                )
            }

            val toolsApps = remaining
                .filter { it.category == AppCategory.TOOLS || it.category == AppCategory.SYSTEM }
                .take(4)
                .map { it.toRef() }

            val initialHome = if (toolsApps.size >= 2) {
                homeApps + HomeItem.Folder(
                    id = "folder_tools_seed",
                    name = if (_uiState.value.settings.language == AppLanguage.ARABIC) "الأدوات" else "Tools",
                    apps = toolsApps
                )
            } else {
                homeApps
            }

            val initialDock = dockCandidates.map { it.toRef() }
            preferenceManager.saveLayout(initialHome, initialDock)
        }
    }

    private fun pickPreferredApps(apps: List<AppInfo>, keywords: List<String>): List<AppInfo> {
        val picked = mutableListOf<AppInfo>()
        for (kw in keywords) {
            val found = apps.firstOrNull { app ->
                app !in picked && (
                    app.packageName.lowercase().contains(kw) ||
                        app.label.lowercase().contains(kw)
                    )
            }
            if (found != null) picked.add(found)
        }
        if (picked.size < 4) {
            for (app in apps) {
                if (app !in picked) picked.add(app)
                if (picked.size == 4) break
            }
        }
        return picked
    }

    fun refreshSystemStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val status = launcherManager.readDeviceStatus(windowBrightness)
            val isDefault = launcherManager.isDefaultLauncher()
            StabilityMonitor.refreshSnapshot()
            _uiState.update {
                it.copy(
                    deviceStatus = status,
                    isDefaultLauncher = isDefault
                )
            }
        }
    }

    fun openOverlay(overlay: LauncherOverlay) {
        if (overlay == LauncherOverlay.CONTROL_PANEL || overlay == LauncherOverlay.SETTINGS) {
            refreshSystemStatus()
        }
        _uiState.update {
            it.copy(
                activeOverlay = overlay,
                isEditMode = false,
                openFolderId = null
            )
        }
    }

    fun closeOverlay() {
        _uiState.update {
            it.copy(
                activeOverlay = LauncherOverlay.NONE,
                drawerQuery = "",
                searchQuery = "",
                openFolderId = null
            )
        }
    }

    fun setEditMode(enabled: Boolean) {
        _uiState.update { it.copy(isEditMode = enabled) }
    }

    fun openFolder(folderId: String?) {
        _uiState.update { it.copy(openFolderId = folderId) }
    }

    fun launchApp(app: AppRef, errorText: String) {
        val launched = launcherManager.launchApp(app.packageName, app.activityName)
        if (launched) {
            viewModelScope.launch {
                preferenceManager.recordRecentLaunch(app.componentKey)
            }
            closeOverlay()
        } else {
            showToast(errorText)
        }
    }

    fun addAppToHome(app: AppInfo, addedMsg: String) {
        val current = _uiState.value.homeItems
        val exists = current.any {
            it is HomeItem.AppShortcut && it.app.componentKey == app.componentKey
        }
        if (!exists) {
            val updated = current + HomeItem.AppShortcut(
                id = "app_${app.componentKey}_${System.currentTimeMillis()}",
                app = app.toRef()
            )
            _uiState.update { it.copy(homeItems = updated) }
            viewModelScope.launch { preferenceManager.saveHomeItems(updated) }
        }
        showToast(addedMsg)
    }

    fun pinAppToDock(app: AppInfo, addedMsg: String) {
        val current = _uiState.value.dockItems
        if (current.any { it.componentKey == app.componentKey }) {
            showToast(addedMsg)
            return
        }
        val updated = (current + app.toRef()).take(5)
        _uiState.update { it.copy(dockItems = updated) }
        viewModelScope.launch { preferenceManager.saveDockItems(updated) }
        showToast(addedMsg)
    }

    fun removeAppFromDock(app: AppRef) {
        val updated = _uiState.value.dockItems.filterNot { it.componentKey == app.componentKey }
        _uiState.update { it.copy(dockItems = updated) }
        viewModelScope.launch { preferenceManager.saveDockItems(updated) }
    }

    fun removeHomeItem(itemId: String) {
        val updated = _uiState.value.homeItems.filterNot { it.id == itemId }
        _uiState.update {
            it.copy(
                homeItems = updated,
                openFolderId = if (it.openFolderId == itemId) null else it.openFolderId
            )
        }
        viewModelScope.launch { preferenceManager.saveHomeItems(updated) }
    }

    fun moveHomeItem(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.homeItems.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _uiState.update { it.copy(homeItems = current) }
        viewModelScope.launch { preferenceManager.saveHomeItems(current) }
    }

    fun createFolder(name: String, initialApps: List<AppInfo> = emptyList()) {
        val cleanName = name.trim().ifEmpty {
            if (_uiState.value.settings.language == AppLanguage.ARABIC) "مجلد" else "Folder"
        }
        val folder = HomeItem.Folder(
            id = "folder_${UUID.randomUUID()}",
            name = cleanName,
            apps = initialApps.map { it.toRef() }
        )
        val updated = _uiState.value.homeItems + folder
        _uiState.update { it.copy(homeItems = updated, openFolderId = folder.id) }
        viewModelScope.launch { preferenceManager.saveHomeItems(updated) }
    }

    fun renameFolder(folderId: String, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        val updated = _uiState.value.homeItems.map { item ->
            if (item is HomeItem.Folder && item.id == folderId) item.copy(name = clean) else item
        }
        _uiState.update { it.copy(homeItems = updated) }
        viewModelScope.launch { preferenceManager.saveHomeItems(updated) }
    }

    fun toggleAppInFolder(folderId: String, app: AppInfo) {
        val updated = _uiState.value.homeItems.map { item ->
            if (item is HomeItem.Folder && item.id == folderId) {
                val exists = item.apps.any { it.componentKey == app.componentKey }
                val nextApps = if (exists) {
                    item.apps.filterNot { it.componentKey == app.componentKey }
                } else {
                    item.apps + app.toRef()
                }
                item.copy(apps = nextApps)
            } else {
                item
            }
        }
        _uiState.update { it.copy(homeItems = updated) }
        viewModelScope.launch { preferenceManager.saveHomeItems(updated) }
    }

    fun setAppHidden(packageName: String, hidden: Boolean, msg: String? = null) {
        viewModelScope.launch {
            preferenceManager.setPackageHidden(packageName, hidden)
        }
        if (msg != null) showToast(msg)
    }

    fun updateDrawerQuery(query: String) {
        _uiState.update { it.copy(drawerQuery = query) }
    }

    fun selectCategory(category: AppCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun toggleShowHiddenInDrawer() {
        _uiState.update { it.copy(showHiddenInDrawer = !it.showHiddenInDrawer) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setMediaVolume(value: Int) {
        launcherManager.setMediaVolume(value)
        _uiState.update {
            it.copy(deviceStatus = it.deviceStatus.copy(mediaVolume = value))
        }
    }

    fun setRingVolume(value: Int) {
        launcherManager.setRingVolume(value)
        _uiState.update {
            it.copy(deviceStatus = it.deviceStatus.copy(ringVolume = value))
        }
    }

    fun setWindowBrightness(activity: Activity?, value: Float) {
        val clamped = value.coerceIn(0.15f, 1.0f)
        windowBrightness = clamped
        activity?.window?.let { win ->
            runCatching {
                val lp = win.attributes
                lp.screenBrightness = clamped
                win.attributes = lp
            }
        }
        _uiState.update {
            it.copy(deviceStatus = it.deviceStatus.copy(windowBrightness = clamped))
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferenceManager.updateSettings { it.copy(language = language) }
        }
    }

    fun updateIconSize(sizeDp: Float) {
        viewModelScope.launch {
            preferenceManager.updateSettings { it.copy(iconSizeDp = sizeDp) }
        }
    }

    fun updateGridDensity(density: GridDensity) {
        viewModelScope.launch {
            preferenceManager.updateSettings { it.copy(gridDensity = density) }
        }
    }

    fun updateGlassOpacity(opacity: Float) {
        viewModelScope.launch {
            preferenceManager.updateSettings { it.copy(glassOpacity = opacity) }
        }
    }

    fun updateEnableBlur(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.updateSettings { it.copy(enableBlur = enabled) }
        }
    }

    fun updateTransitionSpeed(speed: TransitionSpeed) {
        viewModelScope.launch {
            preferenceManager.updateSettings { it.copy(transitionSpeed = speed) }
        }
    }

    fun updateClockStyle(style: ClockStyle) {
        viewModelScope.launch {
            preferenceManager.updateSettings { it.copy(clockStyle = style) }
        }
    }

    fun updateBackgroundStyle(style: BackgroundStyle) {
        viewModelScope.launch {
            preferenceManager.updateSettings { it.copy(backgroundStyle = style) }
        }
    }

    fun updateDockStyle(style: DockStyle) {
        viewModelScope.launch {
            preferenceManager.updateSettings { it.copy(dockStyle = style) }
        }
    }

    fun updateSortAlphabetically(alphabetical: Boolean) {
        viewModelScope.launch {
            preferenceManager.updateSettings { it.copy(sortAlphabetically = alphabetical) }
        }
    }

    fun resetHomeLayout() {
        viewModelScope.launch {
            preferenceManager.resetLayoutFlag()
            val apps = _uiState.value.allApps
            if (apps.isNotEmpty()) {
                seedInitialLayout(apps)
            }
        }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun AppInfo.toRef() = AppRef(
        packageName = packageName,
        activityName = activityName,
        label = label
    )

    private data class PersistedBundle(
        val settings: LauncherSettings,
        val homeItems: List<HomeItem>?,
        val dockItems: List<AppRef>?,
        val hidden: Set<String>,
        val recent: List<String>
    )
}
