package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.IconProcessor
import com.example.localization.LocalAppStrings
import com.example.model.AppInfo
import com.example.model.AppLanguage
import com.example.model.AppRef
import com.example.model.ClockStyle
import com.example.model.DockStyle
import com.example.model.HomeItem
import com.example.model.LauncherOverlay
import com.example.ui.components.GlassSurface
import com.example.ui.components.MonoAppIconTile
import com.example.ui.components.MonoFolderTile
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoColors
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.viewmodel.LauncherUiState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: LauncherUiState,
    iconProcessor: IconProcessor,
    onLaunchApp: (AppRef) -> Unit,
    onOpenOverlay: (LauncherOverlay) -> Unit,
    onSetEditMode: (Boolean) -> Unit,
    onMoveHomeItem: (Int, Int) -> Unit,
    onRemoveHomeItem: (String) -> Unit,
    onRemoveDockItem: (AppRef) -> Unit,
    onAddAppToHome: (AppInfo) -> Unit,
    onPinAppToDock: (AppInfo) -> Unit,
    onCreateFolder: (String) -> Unit,
    onOpenFolder: (String?) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onToggleAppInFolder: (String, AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val settings = state.settings
    var showAddAppSheet by remember { mutableStateOf(false) }
    var addTargetFolderId by remember { mutableStateOf<String?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    // Drag & Drop state on Home Grid
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragAccumulated by remember { mutableStateOf(Offset.Zero) }
    val itemBoundsMap = remember { mutableMapOf<Int, Rect>() }

    // Handle Back button while in Edit Mode or inside an open folder
    BackHandler(enabled = state.openFolderId != null || state.isEditMode || showAddAppSheet) {
        when {
            showAddAppSheet -> showAddAppSheet = false
            state.openFolderId != null -> onOpenFolder(null)
            state.isEditMode -> onSetEditMode(false)
        }
    }

    // Current time ticker (ticks gently every 15s; stops automatically when disposed)
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    var verticalSwipeDelta by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .pointerInput(state.isEditMode) {
                if (!state.isEditMode) {
                    detectVerticalDragGestures(
                        onDragStart = { verticalSwipeDelta = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            verticalSwipeDelta += dragAmount
                        },
                        onDragEnd = {
                            if (verticalSwipeDelta < -90f) {
                                onOpenOverlay(LauncherOverlay.APP_DRAWER)
                            } else if (verticalSwipeDelta > 90f) {
                                onOpenOverlay(LauncherOverlay.CONTROL_PANEL)
                            }
                            verticalSwipeDelta = 0f
                        },
                        onDragCancel = { verticalSwipeDelta = 0f }
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Minimalist Monochrome Top Bar
            HomeTopStatusRow(
                state = state,
                onOpenControl = { onOpenOverlay(LauncherOverlay.CONTROL_PANEL) },
                onOpenSettings = { onOpenOverlay(LauncherOverlay.SETTINGS) },
                onToggleEdit = { onSetEditMode(!state.isEditMode) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Hero Digital Clock & Date & Battery
            HeroClockHeader(
                nowMs = nowMs,
                clockStyle = settings.clockStyle,
                language = settings.language,
                batteryLevel = state.deviceStatus.batteryLevel,
                isCharging = state.deviceStatus.isCharging,
                glassOpacity = settings.glassOpacity,
                enableBlur = settings.enableBlur
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Edit Mode Quick Action Bar
            AnimatedVisibility(
                visible = state.isEditMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                GlassSurface(
                    shape = RoundedCornerShape(18.dp),
                    glassOpacity = settings.glassOpacity + 0.04f,
                    enableBlur = settings.enableBlur,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionPillButton(
                                icon = Icons.Rounded.Add,
                                label = strings.addApp,
                                onClick = {
                                    addTargetFolderId = null
                                    showAddAppSheet = true
                                },
                                testTag = "home_add_app_btn"
                            )
                            ActionPillButton(
                                icon = Icons.Rounded.CreateNewFolder,
                                label = strings.newFolder,
                                onClick = { showCreateFolderDialog = true },
                                testTag = "home_new_folder_btn"
                            )
                        }
                        ActionPillButton(
                            icon = Icons.Rounded.Check,
                            label = strings.doneEditing,
                            filled = true,
                            onClick = { onSetEditMode(false) },
                            testTag = "home_done_edit_btn"
                        )
                    }
                }
            }

            if (state.isEditMode) {
                Text(
                    text = strings.dragHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MonoColors.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // 4. Home Grid (Apps + Folders with Drag & Drop)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (state.homeItems.isEmpty() && !state.isLoading) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = strings.emptyHomeHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MonoColors.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        ActionPillButton(
                            icon = Icons.Rounded.Add,
                            label = strings.addApp,
                            filled = true,
                            onClick = {
                                addTargetFolderId = null
                                showAddAppSheet = true
                            },
                            testTag = "empty_add_app_btn"
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(settings.gridDensity.columns),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("home_apps_grid")
                    ) {
                        itemsIndexed(
                            items = state.homeItems,
                            key = { _, item -> item.id }
                        ) { index, item ->
                            val isDragging = draggedIndex == index
                            val dragScale by animateFloatAsState(
                                targetValue = if (isDragging) 1.08f else 1f,
                                label = "drag_scale"
                            )

                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { coords ->
                                        itemBoundsMap[index] = coords.boundsInRoot()
                                    }
                                    .graphicsLayer {
                                        scaleX = dragScale
                                        scaleY = dragScale
                                        if (isDragging) {
                                            translationX = dragAccumulated.x
                                            translationY = dragAccumulated.y
                                        }
                                    }
                                    .then(
                                        if (state.isEditMode) {
                                            Modifier.pointerInput(item.id, state.homeItems.size) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        draggedIndex = index
                                                        dragAccumulated = Offset.Zero
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragAccumulated += dragAmount
                                                        val currentBounds = itemBoundsMap[draggedIndex]
                                                        if (currentBounds != null) {
                                                            val pointerCenter = currentBounds.center + dragAccumulated
                                                            val targetEntry = itemBoundsMap.entries.firstOrNull { (idx, rect) ->
                                                                idx != draggedIndex && rect.contains(pointerCenter)
                                                            }
                                                            if (targetEntry != null) {
                                                                val targetIdx = targetEntry.key
                                                                onMoveHomeItem(draggedIndex, targetIdx)
                                                                draggedIndex = targetIdx
                                                                dragAccumulated = Offset.Zero
                                                            }
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        draggedIndex = -1
                                                        dragAccumulated = Offset.Zero
                                                    },
                                                    onDragCancel = {
                                                        draggedIndex = -1
                                                        dragAccumulated = Offset.Zero
                                                    }
                                                )
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                when (item) {
                                    is HomeItem.AppShortcut -> {
                                        MonoAppIconTile(
                                            app = item.app,
                                            iconProcessor = iconProcessor,
                                            iconSize = settings.iconSizeDp.dp,
                                            glassOpacity = settings.glassOpacity,
                                            editMode = state.isEditMode,
                                            onRemoveClick = { onRemoveHomeItem(item.id) },
                                            onClick = {
                                                if (!state.isEditMode) onLaunchApp(item.app)
                                            },
                                            onLongClick = { onSetEditMode(true) },
                                            modifier = Modifier.testTag("home_item_${item.app.packageName}")
                                        )
                                    }
                                    is HomeItem.Folder -> {
                                        MonoFolderTile(
                                            folderName = item.name,
                                            apps = item.apps,
                                            iconProcessor = iconProcessor,
                                            iconSize = settings.iconSizeDp.dp,
                                            glassOpacity = settings.glassOpacity,
                                            editMode = state.isEditMode,
                                            onRemoveClick = { onRemoveHomeItem(item.id) },
                                            onClick = { onOpenFolder(item.id) },
                                            onLongClick = { onSetEditMode(true) },
                                            modifier = Modifier.testTag("home_folder_${item.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Glass Quick Search + Drawer Triggers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSurface(
                    shape = CircleShape,
                    glassOpacity = settings.glassOpacity,
                    enableBlur = settings.enableBlur,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { onOpenOverlay(LauncherOverlay.SEARCH) }
                        .testTag("home_search_pill")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = strings.searchHint,
                            tint = MonoColors.SoftWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = strings.searchHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MonoColors.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                GlassSurface(
                    shape = CircleShape,
                    glassOpacity = settings.glassOpacity + 0.03f,
                    enableBlur = settings.enableBlur,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onOpenOverlay(LauncherOverlay.APP_DRAWER) }
                        .testTag("home_drawer_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = strings.appsTitle,
                        tint = MonoColors.PureWhite,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(22.dp)
                    )
                }
            }

            // 6. Glass Dock
            if (settings.dockStyle != DockStyle.HIDDEN) {
                HomeGlassDock(
                    dockItems = state.dockItems,
                    dockStyle = settings.dockStyle,
                    iconProcessor = iconProcessor,
                    iconSize = (settings.iconSizeDp * 0.88f).coerceIn(40f, 60f).dp,
                    glassOpacity = settings.glassOpacity,
                    enableBlur = settings.enableBlur,
                    editMode = state.isEditMode,
                    onLaunchApp = onLaunchApp,
                    onRemoveDockItem = onRemoveDockItem,
                    onLongClick = { onSetEditMode(!state.isEditMode) }
                )
            } else {
                Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
            }
        }
    }

    // Folder Popup Dialog
    val openFolder = state.homeItems.filterIsInstance<HomeItem.Folder>()
        .firstOrNull { it.id == state.openFolderId }
    if (openFolder != null) {
        FolderGlassDialog(
            folder = openFolder,
            iconProcessor = iconProcessor,
            glassOpacity = settings.glassOpacity,
            onDismiss = { onOpenFolder(null) },
            onLaunchApp = onLaunchApp,
            onRename = { newName -> onRenameFolder(openFolder.id, newName) },
            onAddApps = {
                addTargetFolderId = openFolder.id
                showAddAppSheet = true
            },
            onDeleteFolder = { onRemoveHomeItem(openFolder.id) }
        )
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { name ->
                onCreateFolder(name)
                showCreateFolderDialog = false
            }
        )
    }

    // Add App Picker Dialog
    if (showAddAppSheet) {
        val targetFolder = state.homeItems.filterIsInstance<HomeItem.Folder>()
            .firstOrNull { it.id == addTargetFolderId }
        AddAppGlassDialog(
            apps = state.allApps,
            iconProcessor = iconProcessor,
            glassOpacity = settings.glassOpacity,
            targetFolder = targetFolder,
            onDismiss = { showAddAppSheet = false },
            onSelectForHome = { app ->
                onAddAppToHome(app)
                showAddAppSheet = false
            },
            onPinToDock = { app ->
                onPinAppToDock(app)
                showAddAppSheet = false
            },
            onToggleInFolder = { folderId, app ->
                onToggleAppInFolder(folderId, app)
            }
        )
    }
}

@Composable
private fun HomeTopStatusRow(
    state: LauncherUiState,
    onOpenControl: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleEdit: () -> Unit
) {
    val strings = LocalAppStrings.current
    val status = state.deviceStatus

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand + Connectivity Pill
        GlassSurface(
            shape = CircleShape,
            glassOpacity = state.settings.glassOpacity,
            enableBlur = false,
            modifier = Modifier
                .height(38.dp)
                .clickable(onClick = onOpenControl)
                .testTag("home_status_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "M",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MonoColors.PureWhite,
                    fontSize = 14.sp
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MonoColors.Gray)
                )
                Icon(
                    imageVector = if (status.isOnline) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                    contentDescription = strings.wifi,
                    tint = if (status.isOnline) MonoColors.PureWhite else MonoColors.Gray,
                    modifier = Modifier.size(15.dp)
                )
                Icon(
                    imageVector = if (status.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                    contentDescription = strings.battery,
                    tint = MonoColors.SoftWhite,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${status.batteryLevel}%",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    color = MonoColors.SoftWhite
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlassSurface(
                shape = CircleShape,
                glassOpacity = state.settings.glassOpacity,
                enableBlur = false,
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(
                    onClick = onToggleEdit,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_edit_button")
                ) {
                    Icon(
                        imageVector = if (state.isEditMode) Icons.Rounded.Check else Icons.Rounded.Edit,
                        contentDescription = strings.editHome,
                        tint = MonoColors.SoftWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            GlassSurface(
                shape = CircleShape,
                glassOpacity = state.settings.glassOpacity,
                enableBlur = false,
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(
                    onClick = onOpenControl,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_control_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = strings.controlTitle,
                        tint = MonoColors.SoftWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            GlassSurface(
                shape = CircleShape,
                glassOpacity = state.settings.glassOpacity,
                enableBlur = false,
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = strings.settingsTitle,
                        tint = MonoColors.SoftWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroClockHeader(
    nowMs: Long,
    clockStyle: ClockStyle,
    language: AppLanguage,
    batteryLevel: Int,
    isCharging: Boolean,
    glassOpacity: Float,
    enableBlur: Boolean
) {
    val strings = LocalAppStrings.current
    val locale = if (language == AppLanguage.ARABIC) Locale("ar") else Locale.ENGLISH
    val timeText = remember(nowMs) {
        SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date(nowMs))
    }
    val dateText = remember(nowMs, language) {
        SimpleDateFormat("EEEE، d MMMM", locale).let { sdf ->
            if (language == AppLanguage.ARABIC) sdf.format(Date(nowMs))
            else SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH).format(Date(nowMs))
        }
    }

    val clockFont = when (clockStyle) {
        ClockStyle.EDITORIAL -> SpaceGroteskFontFamily
        ClockStyle.GEOMETRIC -> CairoFontFamily
        ClockStyle.MONOSPACE -> JetBrainsMonoFontFamily
    }
    val clockWeight = when (clockStyle) {
        ClockStyle.EDITORIAL -> FontWeight.Light
        ClockStyle.GEOMETRIC -> FontWeight.Bold
        ClockStyle.MONOSPACE -> FontWeight.Medium
    }

    GlassSurface(
        shape = RoundedCornerShape(28.dp),
        glassOpacity = glassOpacity * 0.85f,
        enableBlur = enableBlur,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeText,
                fontFamily = clockFont,
                fontWeight = clockWeight,
                fontSize = if (clockStyle == ClockStyle.MONOSPACE) 54.sp else 62.sp,
                letterSpacing = if (clockStyle == ClockStyle.EDITORIAL) (-2).sp else 1.sp,
                color = MonoColors.PureWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleMedium,
                color = MonoColors.SoftWhite.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isCharging) "${strings.battery}: $batteryLevel% • ${strings.charging}"
                else "${strings.battery}: $batteryLevel%",
                style = MaterialTheme.typography.labelMedium,
                color = MonoColors.Gray
            )
        }
    }
}

@Composable
private fun HomeGlassDock(
    dockItems: List<AppRef>,
    dockStyle: DockStyle,
    iconProcessor: IconProcessor,
    iconSize: androidx.compose.ui.unit.Dp,
    glassOpacity: Float,
    enableBlur: Boolean,
    editMode: Boolean,
    onLaunchApp: (AppRef) -> Unit,
    onRemoveDockItem: (AppRef) -> Unit,
    onLongClick: () -> Unit
) {
    val shape = if (dockStyle == DockStyle.FLOATING_PILL) {
        RoundedCornerShape(30.dp)
    } else {
        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    }

    GlassSurface(
        shape = shape,
        glassOpacity = (glassOpacity + 0.05f).coerceAtMost(0.32f),
        enableBlur = enableBlur,
        elevated = true,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 10.dp)
            .testTag("home_glass_dock")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dockItems.forEach { app ->
                MonoAppIconTile(
                    app = app,
                    iconProcessor = iconProcessor,
                    iconSize = iconSize,
                    glassOpacity = glassOpacity,
                    showLabel = false,
                    editMode = editMode,
                    onRemoveClick = { onRemoveDockItem(app) },
                    onClick = { if (!editMode) onLaunchApp(app) },
                    onLongClick = onLongClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    filled: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (filled) MonoColors.PureWhite else MonoColors.GlassWhiteStrong)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (filled) MonoColors.PureBlack else MonoColors.PureWhite,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (filled) MonoColors.PureBlack else MonoColors.PureWhite
        )
    }
}

@Composable
private fun FolderGlassDialog(
    folder: HomeItem.Folder,
    iconProcessor: IconProcessor,
    glassOpacity: Float,
    onDismiss: () -> Unit,
    onLaunchApp: (AppRef) -> Unit,
    onRename: (String) -> Unit,
    onAddApps: () -> Unit,
    onDeleteFolder: () -> Unit
) {
    val strings = LocalAppStrings.current
    var editingName by remember(folder.id) { mutableStateOf(false) }
    var draftName by remember(folder.id, folder.name) { mutableStateOf(folder.name) }

    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            shape = RoundedCornerShape(28.dp),
            glassOpacity = (glassOpacity + 0.08f).coerceAtMost(0.32f),
            enableBlur = false,
            elevated = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (editingName) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = draftName,
                            onValueChange = { draftName = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MonoColors.PureWhite,
                                unfocusedBorderColor = MonoColors.GlassBorder,
                                focusedTextColor = MonoColors.PureWhite,
                                unfocusedTextColor = MonoColors.SoftWhite
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            onRename(draftName)
                            editingName = false
                        }) {
                            Icon(Icons.Rounded.Check, contentDescription = strings.confirm, tint = MonoColors.PureWhite)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MonoColors.PureWhite,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { editingName = true }
                        )
                        Row {
                            IconButton(onClick = { editingName = true }) {
                                Icon(Icons.Rounded.Edit, contentDescription = strings.renameFolder, tint = MonoColors.SoftWhite)
                            }
                            IconButton(onClick = onAddApps) {
                                Icon(Icons.Rounded.Add, contentDescription = strings.addAppsToFolder, tint = MonoColors.SoftWhite)
                            }
                            IconButton(onClick = onDeleteFolder) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.deleteFolder, tint = MonoColors.Gray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (folder.apps.isEmpty()) {
                    Text(
                        text = strings.addAppsToFolder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MonoColors.Gray,
                        modifier = Modifier
                            .clickable(onClick = onAddApps)
                            .padding(vertical = 24.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        items(folder.apps.size) { idx ->
                            val app = folder.apps[idx]
                            MonoAppIconTile(
                                app = app,
                                iconProcessor = iconProcessor,
                                iconSize = 52.dp,
                                glassOpacity = glassOpacity,
                                onClick = { onLaunchApp(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            shape = RoundedCornerShape(24.dp),
            glassOpacity = 0.20f,
            enableBlur = false,
            elevated = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = strings.newFolder,
                    style = MaterialTheme.typography.titleLarge,
                    color = MonoColors.PureWhite
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(strings.folderNameHint, color = MonoColors.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MonoColors.PureWhite,
                        unfocusedBorderColor = MonoColors.GlassBorder,
                        focusedTextColor = MonoColors.PureWhite,
                        unfocusedTextColor = MonoColors.SoftWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel, color = MonoColors.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onCreate(name.ifBlank { strings.folderDefaultName }) }) {
                        Text(strings.confirm, color = MonoColors.PureWhite)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAppGlassDialog(
    apps: List<AppInfo>,
    iconProcessor: IconProcessor,
    glassOpacity: Float,
    targetFolder: HomeItem.Folder?,
    onDismiss: () -> Unit,
    onSelectForHome: (AppInfo) -> Unit,
    onPinToDock: (AppInfo) -> Unit,
    onToggleInFolder: (String, AppInfo) -> Unit
) {
    val strings = LocalAppStrings.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            shape = RoundedCornerShape(28.dp),
            glassOpacity = (glassOpacity + 0.08f).coerceAtMost(0.32f),
            enableBlur = false,
            elevated = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (targetFolder != null) "${strings.addAppsToFolder}: ${targetFolder.name}" else strings.addApp,
                    style = MaterialTheme.typography.titleMedium,
                    color = MonoColors.PureWhite
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(strings.searchHint, color = MonoColors.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MonoColors.PureWhite,
                        unfocusedBorderColor = MonoColors.GlassBorder,
                        focusedTextColor = MonoColors.PureWhite,
                        unfocusedTextColor = MonoColors.SoftWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered, key = { it.componentKey }) { app ->
                        val inFolder = targetFolder?.apps?.any { it.componentKey == app.componentKey } == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (inFolder) MonoColors.GlassWhiteStrong else Color.Transparent)
                                .clickable {
                                    if (targetFolder != null) {
                                        onToggleInFolder(targetFolder.id, app)
                                    } else {
                                        onSelectForHome(app)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                MonoAppIconTile(
                                    app = AppRef(app.packageName, app.activityName, app.label),
                                    iconProcessor = iconProcessor,
                                    iconSize = 40.dp,
                                    glassOpacity = glassOpacity,
                                    showLabel = false,
                                    onClick = {
                                        if (targetFolder != null) onToggleInFolder(targetFolder.id, app)
                                        else onSelectForHome(app)
                                    }
                                )
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MonoColors.SoftWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (targetFolder != null) {
                                Icon(
                                    imageVector = if (inFolder) Icons.Rounded.Check else Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = if (inFolder) MonoColors.PureWhite else MonoColors.Gray
                                )
                            } else {
                                TextButton(onClick = { onPinToDock(app) }) {
                                    Text("Dock", style = MaterialTheme.typography.labelSmall, color = MonoColors.Gray)
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.doneEditing, color = MonoColors.PureWhite)
                    }
                }
            }
        }
    }
}
