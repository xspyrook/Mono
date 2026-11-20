package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.IconProcessor
import com.example.model.AppRef
import com.example.model.BackgroundStyle
import com.example.ui.theme.MonoColors

@Composable
fun MonoWallpaperBackground(
    style: BackgroundStyle,
    dimmed: Boolean,
    enableBlur: Boolean,
    modifier: Modifier = Modifier
) {
    val blurModifier = if (dimmed && enableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(18.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MonoColors.PureBlack)
            .then(blurModifier)
    ) {
        when (style) {
            BackgroundStyle.PURE_BLACK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MonoColors.PureBlack)
                )
            }
            BackgroundStyle.OBSIDIAN_DEPTH -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(MonoColors.PureBlack)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x24FFFFFF),
                                        Color(0x0BFFFFFF),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.5f, size.height * 0.22f),
                                    radius = size.maxDimension * 0.65f
                                )
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x14FFFFFF),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.8f, size.height * 0.85f),
                                    radius = size.maxDimension * 0.5f
                                )
                            )
                        }
                )
            }
            BackgroundStyle.OBSIDIAN_WAVE -> {
                Image(
                    painter = painterResource(id = R.drawable.img_bg_obsidian_wave_1794733225546),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x88000000),
                                    Color(0x44000000),
                                    Color(0xAA000000)
                                )
                            )
                        )
                )
            }
            BackgroundStyle.MONOLITH_GRID -> {
                Image(
                    painter = painterResource(id = R.drawable.img_bg_monolith_grid_1794733225745),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x77000000),
                                    Color(0x44000000),
                                    Color(0xBB000000)
                                )
                            )
                        )
                )
            }
        }

        if (dimmed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
            )
        }
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    glassOpacity: Float = 0.12f,
    enableBlur: Boolean = true,
    elevated: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val topAlpha = (glassOpacity * 1.45f).coerceIn(0.05f, 0.34f)
    val bottomAlpha = (glassOpacity * 0.45f).coerceIn(0.02f, 0.16f)
    val borderTop = if (elevated) 0.32f else 0.22f
    val borderBottom = if (elevated) 0.10f else 0.06f

    Box(
        modifier = modifier
            .shadow(
                elevation = if (elevated) 16.dp else 8.dp,
                shape = shape,
                ambientColor = MonoColors.GlassShadow,
                spotColor = MonoColors.GlassShadow
            )
            .clip(shape)
            .background(Color(0xB8080808))
            .then(
                if (enableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Subtle frosted inner sheen without blurring foreground children
                    Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = topAlpha),
                                Color.White.copy(alpha = bottomAlpha)
                            )
                        )
                    )
                } else {
                    Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = topAlpha * 0.92f),
                                Color.White.copy(alpha = bottomAlpha * 0.92f)
                            )
                        )
                    )
                }
            )
            .border(
                width = 0.85.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderTop),
                        Color.White.copy(alpha = borderBottom),
                        Color.White.copy(alpha = borderTop * 0.65f)
                    )
                ),
                shape = shape
            ),
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonoAppIconTile(
    app: AppRef,
    iconProcessor: IconProcessor,
    iconSize: Dp,
    glassOpacity: Float,
    showLabel: Boolean = true,
    editMode: Boolean = false,
    onRemoveClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val iconBitmap by produceState<ImageBitmap?>(
        initialValue = iconProcessor.getCached(app.componentKey),
        key1 = app.componentKey
    ) {
        if (value == null) {
            value = iconProcessor.getOrLoadMonochromeIcon(app.packageName, app.activityName)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
        label = "icon_press_scale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.32f else 0f,
        label = "icon_glow_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.22f)),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            GlassSurface(
                shape = RoundedCornerShape(18.dp),
                glassOpacity = glassOpacity,
                enableBlur = false,
                modifier = Modifier
                    .size(iconSize)
                    .drawBehind {
                        if (glowAlpha > 0.01f) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = glowAlpha),
                                        Color.Transparent
                                    )
                                ),
                                radius = size.maxDimension * 0.75f
                            )
                        }
                    }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = iconBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = app.label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    } else {
                        val monogram = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
                        Text(
                            text = monogram,
                            color = MonoColors.SoftWhite,
                            fontSize = (iconSize.value * 0.36f).sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (editMode && onRemoveClick != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MonoColors.PureWhite)
                        .clickable(onClick = onRemoveClick)
                        .testTag("remove_item_${app.packageName}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove",
                        tint = MonoColors.PureBlack,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelMedium,
                color = MonoColors.SoftWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonoFolderTile(
    folderName: String,
    apps: List<AppRef>,
    iconProcessor: IconProcessor,
    iconSize: Dp,
    glassOpacity: Float,
    editMode: Boolean = false,
    onRemoveClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
        label = "folder_press_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.22f)),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            GlassSurface(
                shape = RoundedCornerShape(18.dp),
                glassOpacity = (glassOpacity + 0.04f).coerceAtMost(0.30f),
                enableBlur = false,
                elevated = true,
                modifier = Modifier.size(iconSize)
            ) {
                val preview = apps.take(4)
                if (preview.isEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = folderName,
                        tint = MonoColors.SoftWhite,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(iconSize * 0.45f)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        for (row in 0..1) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                for (col in 0..1) {
                                    val idx = row * 2 + col
                                    val app = preview.getOrNull(idx)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color.White.copy(alpha = 0.07f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (app != null) {
                                            MiniMonoIcon(
                                                app = app,
                                                iconProcessor = iconProcessor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (editMode && onRemoveClick != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MonoColors.PureWhite)
                        .clickable(onClick = onRemoveClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Delete Folder",
                        tint = MonoColors.PureBlack,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = folderName,
            style = MaterialTheme.typography.labelMedium,
            color = MonoColors.SoftWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MiniMonoIcon(
    app: AppRef,
    iconProcessor: IconProcessor
) {
    val iconBitmap by produceState<ImageBitmap?>(
        initialValue = iconProcessor.getCached(app.componentKey),
        key1 = app.componentKey
    ) {
        if (value == null) {
            value = iconProcessor.getOrLoadMonochromeIcon(app.packageName, app.activityName)
        }
    }

    val bmp = iconBitmap
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
        )
    } else {
        Text(
            text = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "M",
            color = MonoColors.SoftWhite,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
