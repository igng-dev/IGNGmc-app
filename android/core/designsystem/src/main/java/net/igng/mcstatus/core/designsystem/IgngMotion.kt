package net.igng.mcstatus.core.designsystem

import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay

/**
 * Unified IGNG Motion language synced with `packages/theme/dashboard.css` & `loading.css`.
 * Fast initial response with long, serene deceleration — zero cartoonish bounce.
 */
object IgngMotion {
    val Ease = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val LoadEase = CubicBezierEasing(0.33f, 0f, 0.2f, 1f)

    const val DURATION_MICRO = 150
    const val DURATION_NORMAL = 220
    const val DURATION_MACRO = 300
    const val DURATION_AMBIENT = 450

    const val LOADING_DELAY_MS = 140L
    const val LOADING_FADE_IN_MS = 160
    const val LOADING_MIN_SHOW_MS = 240L
    const val LOADING_FADE_OUT_MS = 180

    fun <T> microTween(reduced: Boolean = false): TweenSpec<T> =
        tween(durationMillis = if (reduced) 0 else DURATION_MICRO, easing = Ease)

    fun <T> normalTween(reduced: Boolean = false): TweenSpec<T> =
        tween(durationMillis = if (reduced) 0 else DURATION_NORMAL, easing = Ease)

    fun <T> macroTween(reduced: Boolean = false): TweenSpec<T> =
        tween(durationMillis = if (reduced) 0 else DURATION_MACRO, easing = Ease)

    fun <T> standardSpring(reduced: Boolean = false): SpringSpec<T> =
        if (reduced) spring(dampingRatio = 1f, stiffness = 10_000f)
        else spring(dampingRatio = 0.86f, stiffness = 420f)

    fun <T> spatialSpring(reduced: Boolean = false): SpringSpec<T> =
        if (reduced) spring(dampingRatio = 1f, stiffness = 10_000f)
        else spring(dampingRatio = 0.88f, stiffness = 360f)

    fun <T> quickSpring(reduced: Boolean = false): SpringSpec<T> =
        if (reduced) spring(dampingRatio = 1f, stiffness = 10_000f)
        else spring(dampingRatio = 0.84f, stiffness = 620f)
}

@Composable
fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

enum class IgngHapticKind {
    Tap,
    Tick,
    Confirm,
    Warning,
}

@Composable
fun rememberIgngHaptics(): (IgngHapticKind) -> Unit {
    val view = LocalView.current
    val enabled = LocalHapticsEnabled.current
    return remember(view, enabled) {
        { kind ->
            if (enabled) {
                val constant = when (kind) {
                    IgngHapticKind.Tap -> HapticFeedbackConstants.KEYBOARD_TAP
                    IgngHapticKind.Tick -> HapticFeedbackConstants.CLOCK_TICK
                    IgngHapticKind.Confirm -> HapticFeedbackConstants.CONFIRM
                    IgngHapticKind.Warning -> HapticFeedbackConstants.REJECT
                }
                view.performHapticFeedback(constant)
            }
        }
    }
}

/**
 * Tactile micro-scale feedback on press — subtle 0.985f compression with IGNG ease.
 */
fun Modifier.igngPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.982f,
): Modifier = composed {
    val reduced = IgngTheme.motionReduced
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduced) pressedScale else 1f,
        animationSpec = IgngMotion.quickSpring(reduced),
        label = "igngPressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Gentle, calm list entrance choreography: capped to first 6 visible items,
 * 28ms stagger, 8dp subtle rise, zero lingering delay.
 */
fun Modifier.igngStaggeredReveal(
    index: Int,
    key: Any? = Unit,
): Modifier = composed {
    val reduced = IgngTheme.motionReduced
    if (reduced || index > 5) return@composed this
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        delay(index * 28L)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = IgngMotion.DURATION_MACRO, easing = IgngMotion.Ease),
        )
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 20f
    }
}
