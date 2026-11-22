package net.igng.mcstatus.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

enum class FoldedCoreSize(val dp: Dp, val strokeRatio: Float) {
    Sm(16.dp, 5.4f / 48f),
    Md(28.dp, 4.0f / 48f),
    Lg(56.dp, 3.2f / 48f),
}

/**
 * 1:1 Compose Canvas port of IGNG's signature Folded Core ("折角核心", packages/ui/LoadingGlyph.js).
 * Four rounded corner brackets fold sequentially inward (0 / 425 / 850 / 1275ms across 1700ms)
 * while the center diamond breathes smoothly.
 *
 * When [manualProgress] is provided (0f..1f, e.g. during pull-to-refresh), the 4 corners
 * respond directly to gesture tension!
 */
@Composable
fun IgngFoldedCoreGlyph(
    modifier: Modifier = Modifier,
    size: FoldedCoreSize = FoldedCoreSize.Md,
    color: Color = IgngTheme.colors.accent,
    manualProgress: Float? = null,
) {
    val reduced = IgngTheme.motionReduced
    val phase = if (reduced || manualProgress != null) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "foldedCore")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1700, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "foldedCorePhase",
        ).value
    }

    Canvas(modifier = modifier.size(size.dp)) {
        val unit = this.size.minDimension / 48f
        val strokeWidth = this.size.minDimension * size.strokeRatio
        val center = Offset(24f * unit, 24f * unit)

        val corners = listOf(
            cornerPathTopLeft(unit),
            cornerPathTopRight(unit),
            cornerPathBottomRight(unit),
            cornerPathBottomLeft(unit),
        )

        corners.forEachIndexed { idx, path ->
            val (scale, alpha) = when {
                manualProgress != null -> {
                    val p = manualProgress.coerceIn(0f, 1.25f)
                    val staggered = ((p - idx * 0.08f) / 0.76f).coerceIn(0f, 1f)
                    val s = 1f - 0.345f * staggered
                    val a = 0.34f + 0.66f * staggered
                    s to a
                }
                reduced -> 0.88f to 0.85f
                else -> {
                    val localPhase = (phase + 1f - idx * 0.25f) % 1f
                    evalFoldCurve(localPhase)
                }
            }
            withTransform({
                scale(scale, scale, pivot = center)
            }) {
                drawPath(
                    path = path,
                    color = color.copy(alpha = alpha),
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }

        val (coreScale, coreAlpha) = when {
            manualProgress != null -> {
                val p = manualProgress.coerceIn(0f, 1f)
                (0.78f + 0.26f * p) to (0.50f + 0.50f * p)
            }
            reduced -> 0.94f to 0.9f
            else -> {
                val wave = (1f - cos(phase * 2f * PI.toFloat())) * 0.5f
                (0.82f + 0.22f * wave) to (0.55f + 0.45f * wave)
            }
        }
        val diamond = Path().apply {
            moveTo(24f * unit, 16.8f * unit)
            lineTo(31.2f * unit, 24f * unit)
            lineTo(24f * unit, 31.2f * unit)
            lineTo(16.8f * unit, 24f * unit)
            close()
        }
        withTransform({
            scale(coreScale, coreScale, pivot = center)
        }) {
            drawPath(path = diamond, color = color.copy(alpha = coreAlpha))
        }
    }
}

private fun evalFoldCurve(t: Float): Pair<Float, Float> {
    // 0% -> 42%: fold inward (1 -> 0.655, alpha 0.34 -> 1.0); 42% -> 100%: unfold smoothly
    val factor = if (t <= 0.42f) {
        val u = t / 0.42f
        sin(u * PI.toFloat() * 0.5f)
    } else {
        val u = (t - 0.42f) / 0.58f
        cos(u * PI.toFloat() * 0.5f)
    }
    val scale = 1f - 0.345f * factor
    val alpha = 0.34f + 0.66f * factor
    return scale to alpha
}

private fun DrawScope.cornerPathTopLeft(u: Float) = Path().apply {
    moveTo(8f * u, 17.5f * u)
    lineTo(8f * u, 11.5f * u)
    quadraticTo(8f * u, 8f * u, 11.5f * u, 8f * u)
    lineTo(17.5f * u, 8f * u)
}

private fun DrawScope.cornerPathTopRight(u: Float) = Path().apply {
    moveTo(30.5f * u, 8f * u)
    lineTo(36.5f * u, 8f * u)
    quadraticTo(40f * u, 8f * u, 40f * u, 11.5f * u)
    lineTo(40f * u, 17.5f * u)
}

private fun DrawScope.cornerPathBottomRight(u: Float) = Path().apply {
    moveTo(40f * u, 30.5f * u)
    lineTo(40f * u, 36.5f * u)
    quadraticTo(40f * u, 40f * u, 36.5f * u, 40f * u)
    lineTo(30.5f * u, 40f * u)
}

private fun DrawScope.cornerPathBottomLeft(u: Float) = Path().apply {
    moveTo(17.5f * u, 40f * u)
    lineTo(11.5f * u, 40f * u)
    quadraticTo(8f * u, 40f * u, 8f * u, 36.5f * u)
    lineTo(8f * u, 30.5f * u)
}

/**
 * Enforces IGNG's zero-flash loading choreography:
 * 140ms delay -> 160ms fade-in -> minimum 240ms dwell -> 180ms fade-out.
 */
@Composable
fun rememberIgngLoadingVisibility(loading: Boolean): Float {
    val reduced = IgngTheme.motionReduced
    var visible by remember { mutableStateOf(false) }
    var shownAtMs by remember { mutableLongStateOf(0L) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(loading, reduced) {
        if (loading) {
            if (!reduced) delay(IgngMotion.LOADING_DELAY_MS)
            visible = true
            shownAtMs = System.currentTimeMillis()
            alpha.animateTo(
                1f,
                tween(if (reduced) 0 else IgngMotion.LOADING_FADE_IN_MS, easing = IgngMotion.LoadEase),
            )
        } else if (visible) {
            val elapsed = System.currentTimeMillis() - shownAtMs
            val remaining = IgngMotion.LOADING_MIN_SHOW_MS - elapsed
            if (!reduced && remaining > 0L) delay(remaining)
            alpha.animateTo(
                0f,
                tween(if (reduced) 0 else IgngMotion.LOADING_FADE_OUT_MS, easing = IgngMotion.LoadEase),
            )
            visible = false
        }
    }
    return alpha.value
}

/** B-tier block loader: graphic only, no noisy "加载中…" copy. */
@Composable
fun IgngLoadingBlock(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    fill: Boolean = false,
) {
    val alpha = rememberIgngLoadingVisibility(loading = true)
    Box(
        modifier = modifier
            .then(
                when {
                    fill -> Modifier.fillMaxSize()
                    compact -> Modifier.fillMaxWidth().heightIn(min = 96.dp)
                    else -> Modifier.fillMaxWidth().heightIn(min = 180.dp)
                },
            )
            .graphicsLayer { this.alpha = alpha }
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "正在读取"
            },
        contentAlignment = Alignment.Center,
    ) {
        IgngFoldedCoreGlyph(size = if (compact) FoldedCoreSize.Md else FoldedCoreSize.Md)
    }
}

/** A-tier full-screen veil loader matching `.ld-veil`. */
@Composable
fun IgngLoadingVeil(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha = rememberIgngLoadingVisibility(loading = visible)
    if (alpha <= 0.005f) return
    val colors = IgngTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(colors.canvas)
            .background(
                Brush.radialGradient(
                    colors = listOf(colors.accent.copy(alpha = 0.08f), Color.Transparent),
                    radius = 540f,
                ),
            )
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "正在读取"
            },
        contentAlignment = Alignment.Center,
    ) {
        IgngFoldedCoreGlyph(size = FoldedCoreSize.Lg)
    }
}

/**
 * Custom IGNG Pull-to-Refresh where drag distance physically folds the 4 corners inward.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IgngPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    val haptic = rememberIgngHaptics()
    var thresholdCrossed by remember { mutableStateOf(false) }

    LaunchedEffect(state.distanceFraction, isRefreshing) {
        if (!isRefreshing && state.distanceFraction >= 1f && !thresholdCrossed) {
            thresholdCrossed = true
            haptic(IgngHapticKind.Confirm)
        } else if (state.distanceFraction < 0.7f) {
            thresholdCrossed = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {
            val fraction = state.distanceFraction.coerceIn(0f, 1.3f)
            val show = isRefreshing || fraction > 0.04f
            val alpha by animateFloatAsState(
                targetValue = if (show) 1f else 0f,
                animationSpec = IgngMotion.microTween(IgngTheme.motionReduced),
                label = "ptrAlpha",
            )
            if (alpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .graphicsLayer {
                            this.alpha = alpha
                            translationY = if (isRefreshing) 12f else (fraction * 42f).coerceAtMost(56f)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    IgngFoldedCoreGlyph(
                        size = FoldedCoreSize.Md,
                        manualProgress = if (isRefreshing) null else fraction,
                    )
                }
            }
        },
        content = content,
    )
}
