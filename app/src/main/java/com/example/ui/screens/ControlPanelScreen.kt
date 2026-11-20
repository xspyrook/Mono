package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalAppStrings
import com.example.ui.components.GlassSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoColors
import com.example.viewmodel.LauncherUiState
import kotlin.math.roundToInt

@Composable
fun ControlPanelScreen(
    state: LauncherUiState,
    onClose: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenDisplay: () -> Unit,
    onOpenSound: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenDefaultLauncher: () -> Unit,
    onMediaVolumeChange: (Int) -> Unit,
    onRingVolumeChange: (Int) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val status = state.deviceStatus
    val opacity = state.settings.glassOpacity
    val blur = state.settings.enableBlur

    BackHandler(enabled = true, onBack = onClose)

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

            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassSurface(
                    shape = CircleShape,
                    glassOpacity = opacity,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("control_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.doneEditing,
                            tint = MonoColors.PureWhite
                        )
                    }
                }
                Text(
                    text = strings.controlTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MonoColors.PureWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Default Launcher Banner
            GlassSurface(
                shape = RoundedCornerShape(22.dp),
                glassOpacity = if (state.isDefaultLauncher) opacity else opacity + 0.06f,
                enableBlur = blur,
                elevated = !state.isDefaultLauncher,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onOpenDefaultLauncher)
                    .testTag("control_default_launcher_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = null,
                        tint = MonoColors.PureWhite,
                        modifier = Modifier.size(26.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.defaultLauncherTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MonoColors.PureWhite
                        )
                        Text(
                            text = if (state.isDefaultLauncher) strings.defaultLauncherActive else strings.defaultLauncherInactive,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MonoColors.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Telemetry Row: Battery + Network
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassSurface(
                    shape = RoundedCornerShape(22.dp),
                    glassOpacity = opacity,
                    enableBlur = blur,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (status.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                                contentDescription = strings.battery,
                                tint = MonoColors.PureWhite
                            )
                            Text(
                                text = strings.battery,
                                style = MaterialTheme.typography.labelMedium,
                                color = MonoColors.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${status.batteryLevel}%",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = MonoColors.PureWhite
                        )
                        Text(
                            text = if (status.isCharging) strings.charging else "${status.batteryTempC}°C",
                            style = MaterialTheme.typography.labelSmall,
                            color = MonoColors.Gray
                        )
                    }
                }

                GlassSurface(
                    shape = RoundedCornerShape(22.dp),
                    glassOpacity = opacity,
                    enableBlur = blur,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onOpenWifi)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    status.isWifiConnected -> Icons.Rounded.Wifi
                                    status.isCellularConnected -> Icons.Rounded.SignalCellularAlt
                                    else -> Icons.Rounded.WifiOff
                                },
                                contentDescription = strings.wifi,
                                tint = MonoColors.PureWhite
                            )
                            Text(
                                text = strings.wifi,
                                style = MaterialTheme.typography.labelMedium,
                                color = MonoColors.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = when {
                                status.isWifiConnected -> "Wi-Fi"
                                status.isCellularConnected -> "4G / 5G"
                                else -> "OFF"
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MonoColors.PureWhite
                        )
                        Text(
                            text = when {
                                status.isWifiConnected -> strings.connectedWifi
                                status.isCellularConnected -> strings.connectedMobile
                                else -> strings.offline
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MonoColors.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Toggles / System Panels Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ControlTileButton(
                    icon = Icons.Rounded.Wifi,
                    label = strings.wifi,
                    active = status.isWifiConnected,
                    glassOpacity = opacity,
                    onClick = onOpenWifi,
                    testTag = "control_wifi_btn",
                    modifier = Modifier.weight(1f)
                )
                ControlTileButton(
                    icon = Icons.Rounded.Bluetooth,
                    label = strings.bluetooth,
                    active = false,
                    glassOpacity = opacity,
                    onClick = onOpenBluetooth,
                    testTag = "control_bt_btn",
                    modifier = Modifier.weight(1f)
                )
                ControlTileButton(
                    icon = Icons.Rounded.PhoneAndroid,
                    label = strings.display,
                    active = false,
                    glassOpacity = opacity,
                    onClick = onOpenDisplay,
                    testTag = "control_display_btn",
                    modifier = Modifier.weight(1f)
                )
                ControlTileButton(
                    icon = Icons.Rounded.Settings,
                    label = strings.systemSettings,
                    active = false,
                    glassOpacity = opacity,
                    onClick = onOpenSystemSettings,
                    testTag = "control_system_btn",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sliders Card (Brightness + Media Volume + Ring Volume)
            GlassSurface(
                shape = RoundedCornerShape(24.dp),
                glassOpacity = opacity,
                enableBlur = blur,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ControlSliderRow(
                        icon = Icons.Rounded.Brightness6,
                        label = strings.brightness,
                        valueText = "${(status.windowBrightness * 100).roundToInt()}%",
                        value = status.windowBrightness,
                        range = 0.15f..1.0f,
                        onValueChange = onBrightnessChange,
                        onIconClick = onOpenDisplay
                    )

                    ControlSliderRow(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        label = strings.mediaVolume,
                        valueText = "${status.mediaVolume}/${status.maxMediaVolume}",
                        value = status.mediaVolume.toFloat(),
                        range = 0f..status.maxMediaVolume.toFloat(),
                        onValueChange = { onMediaVolumeChange(it.roundToInt()) },
                        onIconClick = onOpenSound
                    )

                    ControlSliderRow(
                        icon = Icons.Rounded.NotificationsActive,
                        label = strings.ringVolume,
                        valueText = "${status.ringVolume}/${status.maxRingVolume}",
                        value = status.ringVolume.toFloat(),
                        range = 0f..status.maxRingVolume.toFloat(),
                        onValueChange = { onRingVolumeChange(it.roundToInt()) },
                        onIconClick = onOpenSound
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ControlTileButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    glassOpacity: Float,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        shape = RoundedCornerShape(20.dp),
        glassOpacity = if (active) (glassOpacity + 0.10f).coerceAtMost(0.32f) else glassOpacity,
        elevated = active,
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) MonoColors.PureWhite else MonoColors.SoftWhite,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) MonoColors.PureWhite else MonoColors.Gray
            )
        }
    }
}

@Composable
private fun ControlSliderRow(
    icon: ImageVector,
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onIconClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onIconClick, modifier = Modifier.size(32.dp)) {
                    Icon(icon, contentDescription = label, tint = MonoColors.PureWhite, modifier = Modifier.size(20.dp))
                }
                Text(label, style = MaterialTheme.typography.bodyLarge, color = MonoColors.SoftWhite)
            }
            Text(
                text = valueText,
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
