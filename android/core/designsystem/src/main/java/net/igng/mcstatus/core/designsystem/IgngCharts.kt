package net.igng.mcstatus.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class IgngChartPoint(
    val label: String,
    val value: Double,
    val detail: String = "",
)

/**
 * Interactive smooth Bezier line chart with finger-drag crosshair snapping & tick haptics.
 */
@Composable
fun IgngLineChart(
    points: List<IgngChartPoint>,
    lineColor: Color,
    valueFormatter: (Double) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 156.dp,
    minY: Double? = null,
    maxY: Double? = null,
) {
    val colors = IgngTheme.colors
    val reduced = IgngTheme.motionReduced
    val haptic = rememberIgngHaptics()
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

    val drawProgress = remember(points) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(points, reduced) {
        if (!reduced) {
            drawProgress.snapTo(0f)
            drawProgress.animateTo(1f, tween(380, easing = IgngMotion.Ease))
        } else {
            drawProgress.snapTo(1f)
        }
    }

    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(IgngTheme.radii.md)
                .background(colors.tileSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text("暂无采样点", style = IgngTheme.type.caption, color = colors.textMuted)
        }
        return
    }

    val rawMin = minY ?: points.minOf { it.value }
    val rawMax = maxY ?: points.maxOf { it.value }
    val paddedMax = if (rawMax <= rawMin) rawMin + 1.0 else rawMax
    val paddedMin = if (minY != null) rawMin else (rawMin - (paddedMax - rawMin) * 0.1).coerceAtLeast(0.0)
    val span = (paddedMax - paddedMin).coerceAtLeast(0.001)

    val active = selectedIndex?.let { points.getOrNull(it) } ?: points.last()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = valueFormatter(active.value),
                style = IgngTheme.type.metricMedium,
                color = lineColor,
            )
            Text(
                text = if (active.detail.isNotBlank()) "${active.label} · ${active.detail}" else active.label,
                style = IgngTheme.type.caption,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(IgngTheme.radii.md)
                .background(colors.tileSoft)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.size > 1) {
                            val idx = ((offset.x / size.width) * (points.size - 1)).roundToInt()
                                .coerceIn(0, points.lastIndex)
                            if (selectedIndex != idx) {
                                selectedIndex = idx
                                haptic(IgngHapticKind.Tick)
                            }
                        }
                    }
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null },
                    ) { change, _ ->
                        if (points.size > 1) {
                            val idx = ((change.position.x / size.width) * (points.size - 1)).roundToInt()
                                .coerceIn(0, points.lastIndex)
                            if (selectedIndex != idx) {
                                selectedIndex = idx
                                haptic(IgngHapticKind.Tick)
                            }
                        }
                    }
                },
        ) {
            val padX = 12.dp.toPx()
            val padY = 14.dp.toPx()
            val w = (size.width - padX * 2).coerceAtLeast(1f)
            val h = (size.height - padY * 2).coerceAtLeast(1f)

            // Horizontal reference lines
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            repeat(3) { step ->
                val y = padY + h * (step / 2f)
                drawLine(
                    color = colors.line,
                    start = Offset(padX, y),
                    end = Offset(size.width - padX, y),
                    strokeWidth = 1f,
                    pathEffect = dash,
                )
            }

            val coords = points.mapIndexed { idx, pt ->
                val x = if (points.size == 1) padX + w / 2f else padX + w * (idx.toFloat() / (points.size - 1))
                val norm = ((pt.value - paddedMin) / span).toFloat().coerceIn(0f, 1f)
                val animatedNorm = norm * drawProgress.value
                val y = padY + h * (1f - animatedNorm)
                Offset(x, y)
            }

            val linePath = Path()
            val fillPath = Path()
            coords.forEachIndexed { idx, pt ->
                if (idx == 0) {
                    linePath.moveTo(pt.x, pt.y)
                    fillPath.moveTo(pt.x, padY + h)
                    fillPath.lineTo(pt.x, pt.y)
                } else {
                    val prev = coords[idx - 1]
                    val midX = (prev.x + pt.x) * 0.5f
                    linePath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                    fillPath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                }
            }
            coords.lastOrNull()?.let { last ->
                fillPath.lineTo(last.x, padY + h)
                fillPath.close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.22f), Color.Transparent),
                    startY = padY,
                    endY = padY + h,
                ),
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            val focusIndex = selectedIndex ?: coords.lastIndex
            coords.getOrNull(focusIndex)?.let { pt ->
                if (selectedIndex != null) {
                    drawLine(
                        color = lineColor.copy(alpha = 0.45f),
                        start = Offset(pt.x, padY),
                        end = Offset(pt.x, padY + h),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash,
                    )
                }
                drawCircle(color = colors.tile, radius = 5.5.dp.toPx(), center = pt)
                drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = pt)
            }
        }
    }
}

enum class HealthBucketLevel {
    Healthy,
    Degraded,
    Down,
    NoData,
}

data class HealthBucketItem(
    val level: HealthBucketLevel,
    val title: String,
    val detail: String,
)

/**
 * Interactive uptime / status health strip (e.g. 48~90 buckets) with scrub inspection.
 */
@Composable
fun IgngStatusHealthStrip(
    buckets: List<HealthBucketItem>,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    val haptic = rememberIgngHaptics()
    var inspected by remember(buckets) { mutableStateOf<Int?>(null) }

    if (buckets.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .pointerInput(buckets) {
                    detectTapGestures { offset ->
                        val idx = ((offset.x / size.width) * buckets.size).toInt().coerceIn(0, buckets.lastIndex)
                        inspected = if (inspected == idx) null else idx
                        haptic(IgngHapticKind.Tick)
                    }
                }
                .pointerInput(buckets) {
                    detectDragGestures(
                        onDragEnd = { inspected = null },
                        onDragCancel = { inspected = null },
                    ) { change, _ ->
                        val idx = ((change.position.x / size.width) * buckets.size).toInt().coerceIn(0, buckets.lastIndex)
                        if (inspected != idx) {
                            inspected = idx
                            haptic(IgngHapticKind.Tick)
                        }
                    }
                },
        ) {
            val gap = 2.dp.toPx()
            val barWidth = ((size.width - gap * (buckets.size - 1)) / buckets.size).coerceAtLeast(2f)
            buckets.forEachIndexed { idx, bucket ->
                val x = idx * (barWidth + gap)
                val active = inspected == idx
                val barColor = when (bucket.level) {
                    HealthBucketLevel.Healthy -> colors.success
                    HealthBucketLevel.Degraded -> colors.warning
                    HealthBucketLevel.Down -> colors.danger
                    HealthBucketLevel.NoData -> colors.lineStrong
                }
                val topPad = if (active) 0f else 3.dp.toPx()
                drawRoundRect(
                    color = if (inspected == null || active) barColor else barColor.copy(alpha = 0.45f),
                    topLeft = Offset(x, topPad),
                    size = Size(barWidth, size.height - topPad * 2f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                )
            }
        }

        val current = inspected?.let { buckets.getOrNull(it) }
        if (current != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(IgngTheme.radii.sm)
                    .background(colors.tileSoft)
                    .border(1.dp, colors.line, IgngTheme.radii.sm)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(current.title, style = IgngTheme.type.caption.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                Spacer(Modifier.width(8.dp))
                Text(current.detail, style = IgngTheme.type.caption, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

data class IgngBarMetric(
    val label: String,
    val formattedValue: String,
    val ratio: Float,
    val tint: Color? = null,
)

/**
 * Clean horizontal distribution bar chart for Vanilla Stats & Traffic Top IPs.
 */
@Composable
fun IgngHorizontalBarList(
    items: List<IgngBarMetric>,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    val reduced = IgngTheme.motionReduced
    val anim = remember(items) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(items, reduced) {
        if (!reduced) {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(340, easing = IgngMotion.Ease))
        } else {
            anim.snapTo(1f)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            val barTint = item.tint ?: colors.accent
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.label,
                        style = IgngTheme.type.bodyMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.formattedValue,
                        style = IgngTheme.type.mono,
                        color = colors.textSecondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.tileSoft),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((item.ratio.coerceIn(0.02f, 1f) * anim.value))
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(barTint),
                    )
                }
            }
        }
    }
}
