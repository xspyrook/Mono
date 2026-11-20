package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.localization.LocalAppStrings
import com.example.model.AppLanguage
import com.example.model.BackgroundStyle
import com.example.model.ClockStyle
import com.example.model.DockStyle
import com.example.model.GridDensity
import com.example.model.TransitionSpeed
import com.example.ui.components.GlassSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoColors
import com.example.viewmodel.LauncherUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: LauncherUiState,
    onClose: () -> Unit,
    onOpenDefaultLauncher: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onIconSizeChange: (Float) -> Unit,
    onGridDensityChange: (GridDensity) -> Unit,
    onGlassOpacityChange: (Float) -> Unit,
    onBlurToggle: (Boolean) -> Unit,
    onTransitionSpeedChange: (TransitionSpeed) -> Unit,
    onClockStyleChange: (ClockStyle) -> Unit,
    onBackgroundStyleChange: (BackgroundStyle) -> Unit,
    onDockStyleChange: (DockStyle) -> Unit,
    onSortAlphabeticallyChange: (Boolean) -> Unit,
    onUnhideApp: (String) -> Unit,
    onResetLayout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val settings = state.settings
    var showHiddenDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (showHiddenDialog) showHiddenDialog = false else onClose()
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassSurface(
                    shape = CircleShape,
                    glassOpacity = settings.glassOpacity,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("settings_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.doneEditing,
                            tint = MonoColors.PureWhite
                        )
                    }
                }
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MonoColors.PureWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Default Home Launcher Card
            GlassSurface(
                shape = RoundedCornerShape(22.dp),
                glassOpacity = settings.glassOpacity,
                enableBlur = settings.enableBlur,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onOpenDefaultLauncher)
                    .testTag("settings_default_home_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Home, contentDescription = null, tint = MonoColors.PureWhite)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.defaultLauncherTitle, style = MaterialTheme.typography.titleMedium, color = MonoColors.PureWhite)
                        Text(
                            if (state.isDefaultLauncher) strings.defaultLauncherActive else strings.defaultLauncherInactive,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MonoColors.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Language Card
            SettingSectionCard(glassOpacity = settings.glassOpacity, enableBlur = settings.enableBlur) {
                Text(strings.appLanguageLabel, style = MaterialTheme.typography.titleMedium, color = MonoColors.PureWhite)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChoiceChipPill(
                        label = strings.arabic,
                        selected = settings.language == AppLanguage.ARABIC,
                        onClick = { onLanguageChange(AppLanguage.ARABIC) },
                        testTag = "lang_arabic_btn",
                        modifier = Modifier.weight(1f)
                    )
                    ChoiceChipPill(
                        label = strings.english,
                        selected = settings.language == AppLanguage.ENGLISH,
                        onClick = { onLanguageChange(AppLanguage.ENGLISH) },
                        testTag = "lang_english_btn",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visual & Glass Engine Card
            SettingSectionCard(glassOpacity = settings.glassOpacity, enableBlur = settings.enableBlur) {
                SliderSettingRow(
                    label = strings.iconSize,
                    valueLabel = "${settings.iconSizeDp.roundToInt()} dp",
                    value = settings.iconSizeDp,
                    range = 44f..72f,
                    onValueChange = onIconSizeChange
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(strings.gridDensity, style = MaterialTheme.typography.titleMedium, color = MonoColors.PureWhite)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GridDensity.entries.forEach { density ->
                        ChoiceChipPill(
                            label = density.label,
                            selected = settings.gridDensity == density,
                            onClick = { onGridDensityChange(density) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SliderSettingRow(
                    label = strings.glassOpacity,
                    valueLabel = "${(settings.glassOpacity * 100).roundToInt()}%",
                    value = settings.glassOpacity,
                    range = 0.05f..0.26f,
                    onValueChange = onGlassOpacityChange
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(strings.blurEffect, style = MaterialTheme.typography.titleMedium, color = MonoColors.PureWhite)
                        Text(strings.blurApiNote, style = MaterialTheme.typography.labelSmall, color = MonoColors.Gray)
                    }
                    Switch(
                        checked = settings.enableBlur,
                        onCheckedChange = onBlurToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MonoColors.PureBlack,
                            checkedTrackColor = MonoColors.PureWhite,
                            uncheckedThumbColor = MonoColors.SoftWhite,
                            uncheckedTrackColor = MonoColors.GlassWhite
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clock, Wallpaper, Dock, Speed, Sorting
            SettingSectionCard(glassOpacity = settings.glassOpacity, enableBlur = settings.enableBlur) {
                Text(strings.clockStyle, style = MaterialTheme.typography.titleMedium, color = MonoColors.PureWhite)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClockStyle.entries.forEach { style ->
                        ChoiceChipPill(
                            label = strings.clockStyleNames[style] ?: style.name,
                            selected = settings.clockStyle == style,
                            onClick = { onClockStyleChange(style) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(strings.backgroundStyle, style = MaterialTheme.typography.titleMedium, color = MonoColors.PureWhite)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BackgroundStyle.entries.forEach { style ->
                        ChoiceChipPill(
                            label = strings.backgroundStyleNames[style] ?: style.name,
                            selected = settings.backgroundStyle == style,
                            onClick = { onBackgroundStyleChange(style) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(strings.dockStyle, style = MaterialTheme.typography.titleMedium, color = MonoColors.PureWhite)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DockStyle.entries.forEach { style ->
                        ChoiceChipPill(
                            label = strings.dockStyleNames[style] ?: style.name,
                            selected = settings.dockStyle == style,
                            onClick = { onDockStyleChange(style) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(strings.transitionSpeed, style = MaterialTheme.typography.titleMedium, color = MonoColors.PureWhite)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransitionSpeed.entries.forEach { speed ->
                        ChoiceChipPill(
                            label = strings.transitionSpeedNames[speed] ?: speed.name,
                            selected = settings.transitionSpeed == speed,
                            onClick = { onTransitionSpeedChange(speed) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(strings.appOrder, style = MaterialTheme.typography.titleMedium, color = MonoColors.PureWhite)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChipPill(
                        label = strings.alphabeticalSort,
                        selected = settings.sortAlphabetically,
                        onClick = { onSortAlphabeticallyChange(true) },
                        modifier = Modifier.weight(1f)
                    )
                    ChoiceChipPill(
                        label = strings.customOrder,
                        selected = !settings.sortAlphabetically,
                        onClick = { onSortAlphabeticallyChange(false) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hidden Apps & Reset Layout
            SettingSectionCard(glassOpacity = settings.glassOpacity, enableBlur = settings.enableBlur) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showHiddenDialog = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Visibility, contentDescription = null, tint = MonoColors.PureWhite)
                        Text(strings.manageHiddenApps, style = MaterialTheme.typography.bodyLarge, color = MonoColors.SoftWhite)
                    }
                    Text(
                        text = "${state.hiddenPackages.size}",
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MonoColors.Gray
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onResetLayout)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = MonoColors.PureWhite)
                    Text(strings.resetHomeLayout, style = MaterialTheme.typography.bodyLarge, color = MonoColors.SoftWhite)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stability & Diagnostics Card
            SettingSectionCard(glassOpacity = settings.glassOpacity, enableBlur = settings.enableBlur) {
                val stab = state.stability
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MonoColors.PureWhite)
                    Column {
                        Text(strings.stabilityTitle, style = MaterialTheme.typography.titleMedium, color = MonoColors.PureWhite)
                        Text(strings.healthyStatus, style = MaterialTheme.typography.labelSmall, color = MonoColors.SoftWhite)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(strings.stabilitySubtitle, style = MaterialTheme.typography.labelSmall, color = MonoColors.Gray)
                Spacer(modifier = Modifier.height(10.dp))

                MetricRow(strings.uptimeLabel, "${stab.uptimeSeconds}s")
                MetricRow(strings.memoryLabel, "${stab.heapUsedMb.roundToInt()} / ${stab.heapMaxMb.roundToInt()} MB")
                MetricRow(strings.iconCacheLabel, "${stab.cachedIconCount} (${stab.iconCacheHits} hits)")
                MetricRow(strings.appsLoadedLabel, "${stab.loadedAppsCount}")
                MetricRow(strings.handledErrorsLabel, "${stab.handledErrorsCount}")
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showHiddenDialog) {
        val hiddenApps = state.allApps.filter { it.packageName in state.hiddenPackages }
        Dialog(onDismissRequest = { showHiddenDialog = false }) {
            GlassSurface(
                shape = RoundedCornerShape(24.dp),
                glassOpacity = 0.22f,
                enableBlur = false,
                elevated = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(strings.manageHiddenApps, style = MaterialTheme.typography.titleLarge, color = MonoColors.PureWhite)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (hiddenApps.isEmpty()) {
                        Text(strings.noAppsFound, style = MaterialTheme.typography.bodyMedium, color = MonoColors.Gray)
                    } else {
                        hiddenApps.forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(app.label, style = MaterialTheme.typography.bodyLarge, color = MonoColors.SoftWhite)
                                TextButton(onClick = { onUnhideApp(app.packageName) }) {
                                    Text(strings.unhideApp, color = MonoColors.PureWhite)
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showHiddenDialog = false }) {
                            Text(strings.doneEditing, color = MonoColors.PureWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSectionCard(
    glassOpacity: Float,
    enableBlur: Boolean,
    content: @Composable () -> Unit
) {
    GlassSurface(
        shape = RoundedCornerShape(24.dp),
        glassOpacity = glassOpacity,
        enableBlur = enableBlur,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

@Composable
private fun ChoiceChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) MonoColors.PureWhite else MonoColors.GlassWhite)
            .border(
                width = 0.75.dp,
                color = if (selected) MonoColors.PureWhite else MonoColors.GlassBorder,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .let { m -> if (testTag != null) m.testTag(testTag) else m },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MonoColors.PureBlack else MonoColors.SoftWhite
        )
    }
}

@Composable
private fun SliderSettingRow(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = MonoColors.PureWhite)
            Text(
                text = valueLabel,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                color = MonoColors.Gray
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MonoColors.PureWhite,
                activeTrackColor = MonoColors.PureWhite,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f)
            )
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MonoColors.Gray)
        Text(value, fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = MonoColors.SoftWhite)
    }
}
