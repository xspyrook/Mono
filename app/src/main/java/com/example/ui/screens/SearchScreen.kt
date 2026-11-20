package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.IconProcessor
import com.example.localization.LocalAppStrings
import com.example.model.AppRef
import com.example.ui.components.GlassSurface
import com.example.ui.components.MonoAppIconTile
import com.example.ui.theme.MonoColors
import com.example.viewmodel.LauncherUiState

@Composable
fun SearchScreen(
    state: LauncherUiState,
    iconProcessor: IconProcessor,
    onClose: () -> Unit,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (AppRef) -> Unit,
    onOpenWebSearch: (String) -> Unit,
    onOpenWifi: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenDisplay: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    BackHandler(enabled = true, onBack = onClose)

    val visibleApps = remember(state.allApps, state.hiddenPackages) {
        state.allApps.filterNot { it.packageName in state.hiddenPackages }
    }

    val matchedApps = remember(visibleApps, state.searchQuery) {
        if (state.searchQuery.isBlank()) emptyList()
        else visibleApps.filter {
            it.label.contains(state.searchQuery, ignoreCase = true) ||
                it.packageName.contains(state.searchQuery, ignoreCase = true)
        }
    }

    val recentApps = remember(visibleApps, state.recentKeys) {
        val byKey = visibleApps.associateBy { it.componentKey }
        state.recentKeys.mapNotNull { byKey[it] }.take(8)
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
            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            .testTag("search_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.cancel,
                            tint = MonoColors.PureWhite
                        )
                    }
                }

                GlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    glassOpacity = state.settings.glassOpacity + 0.04f,
                    enableBlur = state.settings.enableBlur,
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onQueryChange,
                        placeholder = { Text(strings.searchHint, color = MonoColors.Gray) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = MonoColors.PureWhite)
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
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
                            .testTag("search_screen_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.searchQuery.isBlank()) {
                // Quick Actions + Recent Apps
                Text(
                    text = strings.quickActions,
                    style = MaterialTheme.typography.titleMedium,
                    color = MonoColors.Gray,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickSearchActionChip(
                        icon = Icons.Rounded.Wifi,
                        label = strings.wifi,
                        glassOpacity = state.settings.glassOpacity,
                        onClick = onOpenWifi,
                        modifier = Modifier.weight(1f)
                    )
                    QuickSearchActionChip(
                        icon = Icons.Rounded.Bluetooth,
                        label = strings.bluetooth,
                        glassOpacity = state.settings.glassOpacity,
                        onClick = onOpenBluetooth,
                        modifier = Modifier.weight(1f)
                    )
                    QuickSearchActionChip(
                        icon = Icons.Rounded.Brightness6,
                        label = strings.display,
                        glassOpacity = state.settings.glassOpacity,
                        onClick = onOpenDisplay,
                        modifier = Modifier.weight(1f)
                    )
                    QuickSearchActionChip(
                        icon = Icons.Rounded.Settings,
                        label = strings.systemSettings,
                        glassOpacity = state.settings.glassOpacity,
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (recentApps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = strings.recentApps,
                        style = MaterialTheme.typography.titleMedium,
                        color = MonoColors.Gray,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(state.settings.gridDensity.columns),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentApps, key = { it.componentKey }) { app ->
                            val ref = AppRef(app.packageName, app.activityName, app.label)
                            MonoAppIconTile(
                                app = ref,
                                iconProcessor = iconProcessor,
                                iconSize = state.settings.iconSizeDp.dp,
                                glassOpacity = state.settings.glassOpacity,
                                onClick = { onLaunchApp(ref) }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(matchedApps, key = { it.componentKey }) { app ->
                        val ref = AppRef(app.packageName, app.activityName, app.label)
                        GlassSurface(
                            shape = RoundedCornerShape(18.dp),
                            glassOpacity = state.settings.glassOpacity,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onLaunchApp(ref) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MonoAppIconTile(
                                    app = ref,
                                    iconProcessor = iconProcessor,
                                    iconSize = 44.dp,
                                    glassOpacity = state.settings.glassOpacity,
                                    showLabel = false,
                                    onClick = { onLaunchApp(ref) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MonoColors.PureWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = strings.categoryNames[app.category] ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MonoColors.Gray
                                    )
                                }
                            }
                        }
                    }

                    // Web search fallback
                    item(key = "web_search_fallback") {
                        GlassSurface(
                            shape = RoundedCornerShape(18.dp),
                            glassOpacity = state.settings.glassOpacity + 0.04f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenWebSearch(state.searchQuery) }
                                .testTag("search_web_fallback_btn")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = MonoColors.PureWhite
                                )
                                Text(
                                    text = "${strings.searchWebFor} «${state.searchQuery}»",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MonoColors.SoftWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSearchActionChip(
    icon: ImageVector,
    label: String,
    glassOpacity: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        shape = RoundedCornerShape(18.dp),
        glassOpacity = glassOpacity,
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = MonoColors.PureWhite, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MonoColors.SoftWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
