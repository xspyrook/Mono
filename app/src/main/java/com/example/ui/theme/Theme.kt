package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MonoDarkColorScheme = darkColorScheme(
    primary = MonoColors.PureWhite,
    onPrimary = MonoColors.PureBlack,
    primaryContainer = MonoColors.GlassWhiteStrong,
    onPrimaryContainer = MonoColors.SoftWhite,
    secondary = MonoColors.SoftWhite,
    onSecondary = MonoColors.PureBlack,
    secondaryContainer = MonoColors.GlassWhite,
    onSecondaryContainer = MonoColors.SoftWhite,
    tertiary = MonoColors.Gray,
    onTertiary = MonoColors.PureBlack,
    background = MonoColors.PureBlack,
    onBackground = MonoColors.SoftWhite,
    surface = MonoColors.DeepBlack,
    onSurface = MonoColors.SoftWhite,
    surfaceVariant = MonoColors.ElevatedBlack,
    onSurfaceVariant = MonoColors.Gray,
    outline = MonoColors.GlassBorder,
    outlineVariant = MonoColors.DimGray,
    error = MonoColors.SoftWhite,
    onError = MonoColors.PureBlack
)

val MonoShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun MonoGlassTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MonoDarkColorScheme,
        typography = MonoTypography,
        shapes = MonoShapes,
        content = content
    )
}
