package net.igng.mcstatus.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlin.math.abs

/**
 * Full-screen gesture lightbox supporting pinch zoom, double-tap zoom, and vertical drag-to-dismiss.
 */
@Composable
fun IgngImageLightbox(
    imageUrl: String,
    title: String? = null,
    onDismiss: () -> Unit,
) {
    IgngImageLightbox(
        imageUrls = listOf(imageUrl),
        initialIndex = 0,
        titles = listOf(title),
        onDismiss = onDismiss,
    )
}

/**
 * Multi-image gesture lightbox supporting horizontal swipe paging, pinch/double-tap zoom, and drag-to-dismiss.
 */
@Composable
fun IgngImageLightbox(
    imageUrls: List<String>,
    initialIndex: Int = 0,
    titles: List<String?> = emptyList(),
    onDismiss: () -> Unit,
) {
    if (imageUrls.isEmpty()) return
    val safeInitial = initialIndex.coerceIn(0, imageUrls.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeInitial) { imageUrls.size }
    var dragY by remember { mutableFloatStateOf(0f) }

    val backdropAlpha by animateFloatAsState(
        targetValue = (1f - (abs(dragY) / 700f)).coerceIn(0.25f, 0.96f),
        animationSpec = IgngMotion.microTween(IgngTheme.motionReduced),
        label = "lightboxBackdrop",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backdropAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = imageUrls.size > 1,
            ) { page ->
                val url = imageUrls[page]
                val caption = titles.getOrNull(page)
                ZoomableLightboxPage(
                    imageUrl = url,
                    title = caption,
                    dragY = dragY,
                    onDragDelta = { dragY += it },
                    onDragEnd = {
                        if (abs(dragY) > 180f) onDismiss() else dragY = 0f
                    },
                    onDragCancel = { dragY = 0f },
                    onDismiss = onDismiss,
                )
            }

            val currentTitle = titles.getOrNull(pagerState.currentPage)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!currentTitle.isNullOrBlank()) {
                    Text(
                        text = currentTitle,
                        style = IgngTheme.type.bodyMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (imageUrls.size > 1) {
                    Box(
                        modifier = Modifier
                            .clip(IgngTheme.radii.pill)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                            style = IgngTheme.type.badge,
                            color = Color.White,
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = Color.White,
                    )
                }
            }

            if (imageUrls.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 18.dp)
                        .clip(IgngTheme.radii.pill)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "左右滑动切换 · 双击放大 · 下拉关闭",
                        style = IgngTheme.type.badge,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableLightboxPage(
    imageUrl: String,
    title: String?,
    dragY: Float,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
    var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(imageUrl) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.4f
                        }
                    },
                    onTap = {
                        if (scale <= 1.05f) onDismiss()
                    },
                )
            }
            .pointerInput(imageUrl, scale) {
                if (scale <= 1.05f) {
                    detectVerticalDragGestures(
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                    ) { _, delta ->
                        onDragDelta(delta)
                    }
                } else {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = nextScale
                        offset = if (nextScale <= 1.02f) Offset.Zero else offset + pan
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title ?: "图片预览",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val dismissScale = (1f - abs(dragY) / 1600f).coerceIn(0.8f, 1f)
                    scaleX = scale * dismissScale
                    scaleY = scale * dismissScale
                    translationX = offset.x
                    translationY = offset.y + dragY
                },
        )
    }
}
