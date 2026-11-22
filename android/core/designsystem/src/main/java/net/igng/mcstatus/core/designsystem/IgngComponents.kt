package net.igng.mcstatus.core.designsystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import net.igng.mcstatus.core.model.McTextParser
import net.igng.mcstatus.core.model.ServerThemeKey

/**
 * Subtle ambient radial blue/violet background matching `packages/theme/globals.css`.
 */
@Composable
fun IgngAmbientCanvas(
    modifier: Modifier = Modifier,
    accentOverride: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = IgngTheme.colors
    val topLeftColor by animateColorAsState(
        targetValue = (accentOverride ?: colors.accent).copy(alpha = if (colors.isDark) 0.11f else 0.06f),
        animationSpec = tween(if (IgngTheme.motionReduced) 0 else IgngMotion.DURATION_AMBIENT),
        label = "ambientTopGlow",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(topLeftColor, Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.08f),
                    radius = size.maxDimension * 0.55f,
                ),
                center = Offset(size.width * 0.12f, size.height * 0.08f),
                radius = size.maxDimension * 0.55f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.auraViolet, Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.88f),
                    radius = size.maxDimension * 0.5f,
                ),
                center = Offset(size.width * 0.9f, size.height * 0.88f),
                radius = size.maxDimension * 0.5f,
            )
        }
        content()
    }
}

/**
 * Procedural Canvas texture matching `packages/minecraft/lib/server-backgrounds.js`
 * for all 5 IGNG server families. Zero bitmap bloat, crisp at every DPI.
 */
@Composable
fun IgngServerBackdropCanvas(
    themeKey: ServerThemeKey,
    modifier: Modifier = Modifier,
) {
    val isDark = IgngTheme.colors.isDark
    val visual = serverFamilyVisual(themeKey, isDark)
    val accent by animateColorAsState(
        targetValue = visual.accent,
        animationSpec = tween(if (IgngTheme.motionReduced) 0 else IgngMotion.DURATION_AMBIENT, easing = IgngMotion.Ease),
        label = "serverAccent",
    )
    val secondary by animateColorAsState(
        targetValue = visual.secondaryGlow,
        animationSpec = tween(if (IgngTheme.motionReduced) 0 else IgngMotion.DURATION_AMBIENT, easing = IgngMotion.Ease),
        label = "serverSecondary",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Primary atmospheric glow in upper-right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = if (isDark) 0.24f else 0.17f),
                    secondary.copy(alpha = if (isDark) 0.10f else 0.08f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.82f, size.height * 0.18f),
                radius = size.maxDimension * 0.85f,
            ),
            center = Offset(size.width * 0.82f, size.height * 0.18f),
            radius = size.maxDimension * 0.85f,
        )

        val patternAlpha = if (isDark) 0.09f else 0.07f
        val strokeColor = accent.copy(alpha = patternAlpha)

        when (themeKey) {
            ServerThemeKey.CLEAR -> {
                // Subtle diamond grid + warm sunlight halo
                val step = 28.dp.toPx()
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(strokeColor, Offset(x, 0f), Offset(x + size.height, size.height), strokeWidth = 1f)
                    drawLine(strokeColor, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 1f)
                    x += step
                }
            }
            ServerThemeKey.LITE_SERVER -> {
                // Warm angled stripes
                val step = 18.dp.toPx()
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(strokeColor, Offset(x, size.height), Offset(x + size.height * 0.7f, 0f), strokeWidth = 2f)
                    x += step
                }
            }
            ServerThemeKey.MODS -> {
                // Engineering coordinate grid
                val step = 24.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(strokeColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(strokeColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            }
            ServerThemeKey.BAN -> {
                // Bold diagonal hazard bands
                val step = 22.dp.toPx()
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(strokeColor, Offset(x, 0f), Offset(x + size.height, size.height), strokeWidth = 6f)
                    x += step
                }
            }
            ServerThemeKey.MCSERVERS -> {
                // Star atlas dot matrix
                val step = 22.dp.toPx()
                var y = step / 2f
                while (y < size.height) {
                    var x = step / 2f
                    while (x < size.width) {
                        drawCircle(accent.copy(alpha = patternAlpha * 1.4f), radius = 1.6f, center = Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }
        }
    }
}

/**
 * Standard Bento 2.0 Tile card: crisp 1dp border, clean surface, zero visual noise.
 */
@Composable
fun IgngTile(
    modifier: Modifier = Modifier,
    shape: Shape = IgngTheme.radii.lg,
    soft: Boolean = false,
    borderColor: Color = IgngTheme.colors.line,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = IgngTheme.colors
    Surface(
        modifier = modifier,
        shape = shape,
        color = if (soft) colors.tileSoft else colors.tile,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (colors.isDark) 0.dp else 0.5.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

/**
 * Interactive Bento Tile with press micro-scale, subtle accent border highlight,
 * and haptic tap feedback — the mobile counterpart of IGNG Web's `AnimatedBlock`.
 */
@Composable
fun IgngInteractiveTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = IgngTheme.radii.lg,
    soft: Boolean = false,
    accentBorder: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = IgngTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptic = rememberIgngHaptics()

    val border by animateColorAsState(
        targetValue = when {
            pressed -> (accentBorder ?: colors.accent).copy(alpha = 0.55f)
            accentBorder != null -> accentBorder.copy(alpha = 0.28f)
            else -> colors.line
        },
        animationSpec = IgngMotion.microTween(IgngTheme.motionReduced),
        label = "tileBorder",
    )
    val bg by animateColorAsState(
        targetValue = when {
            pressed -> colors.tilePressed
            soft -> colors.tileSoft
            else -> colors.tile
        },
        animationSpec = IgngMotion.microTween(IgngTheme.motionReduced),
        label = "tileBg",
    )

    Surface(
        modifier = modifier
            .igngPressScale(interactionSource)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic(IgngHapticKind.Tap)
                    onClick()
                },
            ),
        shape = shape,
        color = bg,
        border = BorderStroke(1.dp, border),
        shadowElevation = if (colors.isDark) 0.dp else 0.5.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

enum class IgngButtonStyle {
    Primary,
    Secondary,
    Ghost,
    Danger,
}

@Composable
fun IgngButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IgngButtonStyle = IgngButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    compact: Boolean = false,
) {
    val colors = IgngTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptic = rememberIgngHaptics()

    val containerColor = when (style) {
        IgngButtonStyle.Primary -> if (pressed) colors.accentHover else colors.accent
        IgngButtonStyle.Secondary -> if (pressed) colors.tilePressed else colors.tile
        IgngButtonStyle.Ghost -> if (pressed) colors.accentSoft else Color.Transparent
        IgngButtonStyle.Danger -> if (pressed) colors.danger else colors.dangerBg
    }
    val contentColor = when (style) {
        IgngButtonStyle.Primary -> colors.onAccent
        IgngButtonStyle.Secondary -> colors.textPrimary
        IgngButtonStyle.Ghost -> colors.accent
        IgngButtonStyle.Danger -> if (pressed) colors.onAccent else colors.danger
    }
    val border = when (style) {
        IgngButtonStyle.Secondary -> BorderStroke(1.dp, colors.lineStrong)
        IgngButtonStyle.Danger -> BorderStroke(1.dp, colors.danger.copy(alpha = 0.35f))
        else -> null
    }

    val effectiveAlpha = if (enabled) 1f else 0.45f
    Surface(
        modifier = modifier
            .igngPressScale(interactionSource, pressedScale = 0.97f)
            .clip(IgngTheme.radii.sm)
            .clickable(
                enabled = enabled && !loading,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic(if (style == IgngButtonStyle.Danger) IgngHapticKind.Warning else IgngHapticKind.Tap)
                    onClick()
                },
            ),
        shape = IgngTheme.radii.sm,
        color = containerColor.copy(alpha = containerColor.alpha * effectiveAlpha),
        border = border,
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = if (compact) 12.dp else 16.dp,
                vertical = if (compact) 8.dp else 11.dp,
            ),
            contentAlignment = Alignment.Center,
        ) {
            // Keep intrinsic width locked while showing Folded Core loader (per console-design.md)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.offset(y = 0.dp),
            ) {
                val textAlpha = if (loading) 0f else 1f
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = textAlpha),
                        modifier = Modifier.size(if (compact) 15.dp else 17.dp),
                    )
                }
                Text(
                    text = text,
                    style = if (compact) IgngTheme.type.caption.copy(fontWeight = FontWeight.SemiBold)
                    else IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor.copy(alpha = textAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (loading) {
                IgngFoldedCoreGlyph(
                    size = FoldedCoreSize.Sm,
                    color = contentColor,
                )
            }
        }
    }
}

enum class IgngBadgeTone {
    Neutral,
    Accent,
    Success,
    Warning,
    Danger,
}

@Composable
fun IgngBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: IgngBadgeTone = IgngBadgeTone.Neutral,
    customColor: Color? = null,
    leadingDot: Boolean = false,
    pulse: Boolean = false,
) {
    val colors = IgngTheme.colors
    val fg = customColor ?: when (tone) {
        IgngBadgeTone.Neutral -> colors.textSecondary
        IgngBadgeTone.Accent -> colors.accent
        IgngBadgeTone.Success -> colors.success
        IgngBadgeTone.Warning -> colors.warning
        IgngBadgeTone.Danger -> colors.danger
    }
    val bg = when {
        customColor != null -> customColor.copy(alpha = if (colors.isDark) 0.18f else 0.11f)
        tone == IgngBadgeTone.Neutral -> colors.tileSoft
        tone == IgngBadgeTone.Accent -> colors.accentSoft
        tone == IgngBadgeTone.Success -> colors.successBg
        tone == IgngBadgeTone.Warning -> colors.warningBg
        else -> colors.dangerBg
    }

    Row(
        modifier = modifier
            .clip(IgngTheme.radii.pill)
            .background(bg)
            .border(1.dp, fg.copy(alpha = if (tone == IgngBadgeTone.Neutral) 0.16f else 0.24f), IgngTheme.radii.pill)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (leadingDot) {
            IgngLivePulseDot(color = fg, active = pulse)
        }
        Text(
            text = text,
            style = IgngTheme.type.badge,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun IgngLivePulseDot(
    color: Color,
    active: Boolean = true,
    size: Dp = 7.dp,
) {
    val reduced = IgngTheme.motionReduced
    val pulseAlpha = if (active && !reduced) {
        val transition = rememberInfiniteTransition(label = "livePulse")
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "livePulseAlpha",
        ).value
    } else {
        0f
    }
    Box(modifier = Modifier.size(size + 4.dp), contentAlignment = Alignment.Center) {
        if (active && !reduced) {
            Box(
                modifier = Modifier
                    .size(size + 4.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = pulseAlpha * 0.45f)),
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
        )
    }
}

/**
 * Interactive copy pill with animated Copy -> Check morph + success glow.
 */
@Composable
fun IgngCopyPill(
    value: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    mono: Boolean = true,
) {
    val context = LocalContext.current
    val colors = IgngTheme.colors
    val haptic = rememberIgngHaptics()
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1800L)
            copied = false
        }
    }

    val border by animateColorAsState(
        targetValue = if (copied) colors.success else colors.lineStrong,
        animationSpec = IgngMotion.microTween(IgngTheme.motionReduced),
        label = "copyBorder",
    )
    val bg by animateColorAsState(
        targetValue = if (copied) colors.successBg else colors.tileSoft,
        animationSpec = IgngMotion.microTween(IgngTheme.motionReduced),
        label = "copyBg",
    )
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .igngPressScale(interaction)
            .clip(IgngTheme.radii.sm)
            .background(bg)
            .border(1.dp, border, IgngTheme.radii.sm)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    copyTextToClipboard(context, label ?: "IGNG", value)
                    haptic(IgngHapticKind.Confirm)
                    copied = true
                },
            )
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (copied) "已复制" else (label ?: value),
            style = if (mono && !copied) IgngTheme.type.mono else IgngTheme.type.caption.copy(fontWeight = FontWeight.SemiBold),
            color = if (copied) colors.success else colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        AnimatedContent(
            targetState = copied,
            transitionSpec = {
                (fadeIn(IgngMotion.microTween()) + scaleIn(initialScale = 0.72f)) togetherWith
                    (fadeOut(IgngMotion.microTween()) + scaleOut(targetScale = 0.72f))
            },
            label = "copyIconMorph",
        ) { isCopied ->
            Icon(
                imageVector = if (isCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                contentDescription = if (isCopied) "已复制" else "复制",
                tint = if (isCopied) colors.success else colors.textSecondary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

fun copyTextToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

/**
 * Vertical rolling numeric text with tabular numerals (`tnum`) so live metrics glide without jitter.
 */
@Composable
fun IgngAnimatedCounter(
    text: String,
    style: TextStyle = IgngTheme.type.metricLarge,
    color: Color = IgngTheme.colors.textPrimary,
    modifier: Modifier = Modifier,
) {
    val reduced = IgngTheme.motionReduced
    if (reduced) {
        Text(text = text, style = style, color = color, modifier = modifier, maxLines = 1)
        return
    }
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(IgngMotion.standardSpring()) { it / 3 } + fadeIn(IgngMotion.microTween())) togetherWith
                (slideOutVertically(IgngMotion.standardSpring()) { -it / 3 } + fadeOut(IgngMotion.microTween()))
        },
        label = "igngCounter",
        modifier = modifier,
    ) { value ->
        Text(text = value, style = style, color = color, maxLines = 1)
    }
}

/**
 * Spring-sliding segmented control matching IGNG Console's `Segmented`.
 */
@Composable
fun <T> IgngSegmentedTabs(
    items: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val colors = IgngTheme.colors
    val reduced = IgngTheme.motionReduced
    val haptic = rememberIgngHaptics()
    val selectedIndex = items.indexOf(selected).coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(IgngTheme.radii.md)
            .background(colors.tileSoft)
            .border(1.dp, colors.line, IgngTheme.radii.md)
            .padding(4.dp),
    ) {
        val segmentWidth = maxWidth / items.size
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = IgngMotion.standardSpring(reduced),
            label = "segmentIndicator",
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .height(34.dp)
                .clip(IgngTheme.radii.sm)
                .background(colors.tile)
                .border(1.dp, colors.lineStrong, IgngTheme.radii.sm),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            items.forEach { item ->
                val isSelected = item == selected
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) colors.textPrimary else colors.textSecondary,
                    animationSpec = IgngMotion.microTween(reduced),
                    label = "segmentText",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(IgngTheme.radii.sm)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!isSelected) {
                                    haptic(IgngHapticKind.Tick)
                                    onSelect(item)
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labelOf(item),
                        style = IgngTheme.type.caption.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

data class IgngNavTabItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
)

/**
 * Floating translucent pill bottom navigation bar with spring-sliding active capsule.
 */
@Composable
fun IgngFloatingNavBar(
    items: List<IgngNavTabItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    val reduced = IgngTheme.motionReduced
    val haptic = rememberIgngHaptics()
    val selectedIndex = items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.glassBar,
            border = BorderStroke(1.dp, colors.lineStrong),
            shadowElevation = if (colors.isDark) 8.dp else 10.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            ) {
                val tabWidth = maxWidth / items.size
                val indicatorOffset by animateDpAsState(
                    targetValue = tabWidth * selectedIndex,
                    animationSpec = IgngMotion.standardSpring(reduced),
                    label = "navPillOffset",
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(50.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.accentSoft),
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    items.forEach { item ->
                        val isSelected = item.id == selectedId
                        val tint by animateColorAsState(
                            targetValue = if (isSelected) colors.accent else colors.textSecondary,
                            animationSpec = IgngMotion.microTween(reduced),
                            label = "navTint",
                        )
                        val iconY by animateDpAsState(
                            targetValue = if (isSelected && !reduced) (-1).dp else 0.dp,
                            animationSpec = IgngMotion.quickSpring(reduced),
                            label = "navIconY",
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (!isSelected) {
                                            haptic(IgngHapticKind.Tick)
                                            onSelect(item.id)
                                        }
                                    },
                                ),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = tint,
                                    modifier = Modifier
                                        .offset(y = iconY)
                                        .size(21.dp),
                                )
                                if (item.badgeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 5.dp, y = (-3).dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colors.danger),
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                style = IgngTheme.type.badge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = tint,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IgngTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    val colors = IgngTheme.colors
    val haptic = rememberIgngHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (onBack != null) {
            Surface(
                shape = IgngTheme.radii.sm,
                color = colors.tile,
                border = BorderStroke(1.dp, colors.line),
                modifier = Modifier
                    .size(38.dp)
                    .clip(IgngTheme.radii.sm)
                    .clickable {
                        haptic(IgngHapticKind.Tap)
                        onBack()
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = IgngTheme.type.pageTitle,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = IgngTheme.type.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            actions()
        }
    }
}

@Composable
fun IgngSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = IgngTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = IgngTheme.type.sectionTitle,
                color = colors.textPrimary,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = IgngTheme.type.caption,
                    color = colors.textSecondary,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun IgngSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null,
) {
    val colors = IgngTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(IgngTheme.radii.md)
            .background(colors.tile)
            .border(1.dp, colors.lineStrong, IgngTheme.radii.md)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = IgngTheme.type.body,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = IgngTheme.type.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions.Default,
                keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "清空",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun IgngTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    isPassword: Boolean = false,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    val colors = IgngTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = IgngTheme.type.caption.copy(fontWeight = FontWeight.SemiBold),
            color = if (isError) colors.danger else colors.textSecondary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(IgngTheme.radii.sm)
                .background(colors.tileSoft)
                .border(
                    width = 1.dp,
                    color = if (isError) colors.danger else colors.lineStrong,
                    shape = IgngTheme.radii.sm,
                )
                .padding(horizontal = 12.dp, vertical = 11.dp),
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = IgngTheme.type.body,
                    color = colors.textMuted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                minLines = minLines,
                textStyle = IgngTheme.type.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                visualTransformation = if (isPassword) {
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                } else {
                    androidx.compose.ui.text.input.VisualTransformation.None
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!supportingText.isNullOrBlank()) {
            Text(
                text = supportingText,
                style = IgngTheme.type.caption,
                color = if (isError) colors.danger else colors.textSecondary,
            )
        }
    }
}

/**
 * Calm skeleton rows per `docs/console-design.md`: no flashy gradient sweep,
 * only gentle low-amplitude opacity breathing to preview layout height without CLS.
 */
@Composable
fun IgngSkeletonRows(
    count: Int = 4,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    val reduced = IgngTheme.motionReduced
    val alpha = if (reduced) {
        0.55f
    } else {
        val transition = rememberInfiniteTransition(label = "skeletonPulse")
        transition.animateFloat(
            initialValue = 0.42f,
            targetValue = 0.78f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = IgngMotion.LoadEase),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "skeletonAlpha",
        ).value
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(count) {
            IgngTile(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(14.dp)
                        .clip(IgngTheme.radii.sm)
                        .background(colors.lineStrong.copy(alpha = alpha)),
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(11.dp)
                        .clip(IgngTheme.radii.sm)
                        .background(colors.line.copy(alpha = alpha)),
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(11.dp)
                        .clip(IgngTheme.radii.sm)
                        .background(colors.line.copy(alpha = alpha)),
                )
            }
        }
    }
}

@Composable
fun IgngStateBlock(
    title: String,
    description: String? = null,
    isError: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    IgngTile(
        modifier = modifier.fillMaxWidth(),
        borderColor = if (isError) colors.danger.copy(alpha = 0.3f) else colors.line,
        contentPadding = PaddingValues(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(IgngTheme.radii.md)
                    .background(if (isError) colors.dangerBg else colors.tileSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.Inbox,
                    contentDescription = null,
                    tint = if (isError) colors.danger else colors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = title,
                style = IgngTheme.type.cardTitle,
                color = colors.textPrimary,
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = IgngTheme.type.caption,
                    color = colors.textSecondary,
                )
            }
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Spacer(Modifier.height(4.dp))
                IgngButton(
                    text = actionLabel,
                    onClick = onAction,
                    style = if (isError) IgngButtonStyle.Primary else IgngButtonStyle.Secondary,
                    icon = if (isError) Icons.Rounded.Refresh else null,
                    compact = true,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IgngDetailSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = IgngTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.tile,
        contentColor = colors.textPrimary,
        scrimColor = colors.scrim,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            if (!title.isNullOrBlank()) {
                Text(text = title, style = IgngTheme.type.pageTitle, color = colors.textPrimary)
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = subtitle, style = IgngTheme.type.caption, color = colors.textSecondary)
                }
                Spacer(Modifier.height(16.dp))
            }
            content()
        }
    }
}

@Composable
fun IgngConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
) {
    val colors = IgngTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.tile,
        shape = IgngTheme.radii.lg,
        title = { Text(title, style = IgngTheme.type.sectionTitle, color = colors.textPrimary) },
        text = { Text(message, style = IgngTheme.type.body, color = colors.textSecondary) },
        confirmButton = {
            IgngButton(
                text = confirmLabel,
                onClick = onConfirm,
                style = if (danger) IgngButtonStyle.Danger else IgngButtonStyle.Primary,
                compact = true,
            )
        },
        dismissButton = {
            IgngButton(
                text = "取消",
                onClick = onDismiss,
                style = IgngButtonStyle.Secondary,
                compact = true,
            )
        },
    )
}

@Composable
fun IgngAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val colors = IgngTheme.colors
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.accentSoft)
            .border(1.dp, colors.lineStrong, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = IgngTheme.type.cardTitle,
            color = colors.accent,
        )
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun IgngMcFormattedText(
    raw: String,
    modifier: Modifier = Modifier,
    style: TextStyle = IgngTheme.type.body,
    defaultColor: Color = IgngTheme.colors.textPrimary,
    maxLines: Int = Int.MAX_VALUE,
) {
    val isDark = IgngTheme.colors.isDark
    val spans = remember(raw) { McTextParser.parse(raw) }
    val annotated = remember(spans, isDark, defaultColor) {
        buildAnnotatedString {
            spans.forEach { span ->
                val resolvedColor = span.colorHex?.let { hex ->
                    // Ensure dark/light legibility for white/black Minecraft color codes
                    when {
                        !isDark && hex == 0xFFFFFFFFL -> defaultColor
                        !isDark && hex == 0xFFFFFF55L -> Color(0xFFB45309)
                        isDark && hex == 0xFF000000L -> defaultColor
                        else -> Color(hex)
                    }
                } ?: defaultColor
                withStyle(
                    SpanStyle(
                        color = resolvedColor,
                        fontWeight = if (span.bold) FontWeight.Bold else null,
                        fontStyle = if (span.italic) FontStyle.Italic else null,
                        textDecoration = when {
                            span.underline && span.strikethrough -> TextDecoration.combine(
                                listOf(TextDecoration.Underline, TextDecoration.LineThrough),
                            )
                            span.underline -> TextDecoration.Underline
                            span.strikethrough -> TextDecoration.LineThrough
                            else -> null
                        },
                    ),
                ) {
                    append(span.text)
                }
            }
        }
    }
    Text(
        text = annotated,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
