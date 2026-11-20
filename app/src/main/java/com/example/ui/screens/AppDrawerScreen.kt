package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.engine.IconProcessor
import com.example.localization.LocalAppStrings
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.model.AppRef
import com.example.ui.components.GlassSurface
import com.example.ui.components.MonoAppIconTile
import com.example.ui.theme.MonoColors
import com.example.viewmodel.LauncherUiState

@Composable
fun AppDrawerScreen(
    state: LauncherUiState,
    iconProcessor: IconProcessor,
    onClose: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectCategory: (AppCategory) -> Unit,
    onToggleShowHidden: () -> Unit,
    onLaunchApp: (AppRef) -> Unit,
    onAddToHome: (AppInfo) -> Unit,
    onPinToDock: (AppInfo) -> Unit,
    onToggleHidden: (AppInfo, Boolean) -> Unit,
    onOpenAppInfo: (String) -> Unit,
    onUninstallApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var contextApp by remember { mutableStateOf<AppInfo?>(null) }

    BackHandler(enabled = true) {
        if (contextApp != null) {
            contextApp = null
        } else {
            onClose()
        }
    }

    val displayedApps = remember(
        state.allApps,
        state.drawerQuery,
        state.selectedCategory,
        state.hiddenPackages,
        state.showHiddenInDrawer,
        state.settings.sortAlphabetically,
        state.recentKeys
    ) {
        val base = state.allApps.filter { app ->
            val hiddenMatch = state.showHiddenInDrawer || app.packageName !in state.hiddenPackages
            val categoryMatch = state.selectedCategory == AppCategory.ALL || app.category == state.selectedCategory
            val queryMatch = state.drawerQuery.isBlank() ||
                app.label.contains(state.drawerQuery, ignoreCase = true) ||
                app.packageName.contains(state.drawerQuery, ignoreCase = true)
            hiddenMatch && categoryMatch && queryMatch
        }

        if (state.settings.sortAlphabetically) {
            base
        } else {
            val recentOrder = state.recentKeys.withIndex().associate { it.value to it.index }
            base.sortedWith(compareBy<AppInfo> { recentOrder[it.componentKey] ?: Int.MAX_VALUE }.thenBy { it.label })
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassSurface(
                        shape = CircleShape,
                        glassOpacity = state.settings.glassOpacity,
                        modifier = Modifier.size(48.dp)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("drawer_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = strings.doneEditing,
                                tint = MonoColors.PureWhite
                            )
                        }
                    }
                    Text(
                        text = strings.appsTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MonoColors.PureWhite
                    )
                }

                if (state.hiddenPackages.isNotEmpty()) {
                    GlassSurface(
                        shape = CircleShape,
                        glassOpacity = state.settings.glassOpacity,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onToggleShowHidden)
                            .testTag("drawer_toggle_hidden_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (state.showHiddenInDrawer) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = MonoColors.SoftWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (state.showHiddenInDrawer) strings.hideHiddenApps else strings.showHiddenApps,
                                style = MaterialTheme.typography.labelMedium,
                                color = MonoColors.SoftWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Input
            GlassSurface(
                shape = RoundedCornerShape(22.dp),
                glassOpacity = state.settings.glassOpacity + 0.03f,
                enableBlur = state.settings.enableBlur,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.drawerQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text(strings.searchHint, color = MonoColors.Gray) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = MonoColors.SoftWhite)
                    },
                    trailingIcon = {
                        if (state.drawerQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = null, tint = MonoColors.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MonoColors.PureWhite,
                        unfocusedTextColor = MonoColors.SoftWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawer_search_input")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(AppCategory.entries) { category ->
                    val selected = state.selectedCategory == category
                    val label = strings.categoryNames[category] ?: category.name
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) MonoColors.PureWhite else MonoColors.GlassWhite)
                            .border(
                                width = 0.75.dp,
                                color = if (selected) MonoColors.PureWhite else MonoColors.GlassBorder,
                                shape = CircleShape
                            )
                            .clickable { onSelectCategory(category) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("drawer_cat_${category.name}")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MonoColors.PureBlack else MonoColors.SoftWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // App Grid
            if (displayedApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noAppsFound,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MonoColors.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(state.settings.gridDensity.columns),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("drawer_apps_grid")
                ) {
                    items(
                        items = displayedApps,
                        key = { it.componentKey }
                    ) { app ->
                        val ref = AppRef(app.packageName, app.activityName, app.label)
                        MonoAppIconTile(
                            app = ref,
                            iconProcessor = iconProcessor,
                            iconSize = state.settings.iconSizeDp.dp,
                            glassOpacity = state.settings.glassOpacity,
                            onClick = { onLaunchApp(ref) },
                            onLongClick = { contextApp = app },
                            modifier = Modifier.testTag("drawer_app_${app.packageName}")
                        )
                    }
                }
            }
        }
    }

    // Long-press App Context Glass Dialog
    val selectedApp = contextApp
    if (selectedApp != null) {
        val isHidden = selectedApp.packageName in state.hiddenPackages
        Dialog(onDismissRequest = { contextApp = null }) {
            GlassSurface(
                shape = RoundedCornerShape(26.dp),
                glassOpacity = (state.settings.glassOpacity + 0.08f).coerceAtMost(0.32f),
                enableBlur = false,
                elevated = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = selectedApp.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = MonoColors.PureWhite
                    )
                    Text(
                        text = selectedApp.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MonoColors.Gray
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    DrawerActionRow(
                        icon = Icons.Rounded.Home,
                        label = strings.addToHome,
                        onClick = {
                            onAddToHome(selectedApp)
                            contextApp = null
                        }
                    )
                    DrawerActionRow(
                        icon = Icons.Rounded.PushPin,
                        label = strings.addToDock,
                        onClick = {
                            onPinToDock(selectedApp)
                            contextApp = null
                        }
                    )
                    DrawerActionRow(
                        icon = if (isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        label = if (isHidden) strings.unhideApp else strings.hideApp,
                        onClick = {
                            onToggleHidden(selectedApp, !isHidden)
                            contextApp = null
                        }
                    )
                    DrawerActionRow(
                        icon = Icons.Rounded.Info,
                        label = strings.appInfo,
                        onClick = {
                            onOpenAppInfo(selectedApp.packageName)
                            contextApp = null
                        }
                    )
                    if (!selectedApp.isSystemApp) {
                        DrawerActionRow(
                            icon = Icons.Rounded.DeleteOutline,
                            label = strings.uninstall,
                            onClick = {
                                onUninstallApp(selectedApp.packageName)
                                contextApp = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MonoColors.PureWhite, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MonoColors.SoftWhite)
    }
}
