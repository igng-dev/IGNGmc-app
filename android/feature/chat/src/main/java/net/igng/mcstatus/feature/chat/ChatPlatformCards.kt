package net.igng.mcstatus.feature.chat

import android.content.ContentValues
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.AccountBox
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.igng.mcstatus.core.auth.CustomTabsLauncher
import net.igng.mcstatus.core.data.ChatAttachmentDto
import net.igng.mcstatus.core.data.ChatForwardChildDto
import net.igng.mcstatus.core.data.ChatForwardResponse
import net.igng.mcstatus.core.data.ChatMessageDto
import net.igng.mcstatus.core.data.ChatRepository
import net.igng.mcstatus.core.data.ChatStructurePartDto
import net.igng.mcstatus.core.designsystem.IgngButton
import net.igng.mcstatus.core.designsystem.IgngButtonStyle
import net.igng.mcstatus.core.designsystem.IgngDetailSheet
import net.igng.mcstatus.core.designsystem.IgngLoadingBlock
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.model.SiteEndpoints

// ==================== Single-Instance Voice Audio Player ====================

@Stable
class ChatAudioPlayerController {
    var activeKey by mutableStateOf<String?>(null)
        private set
    var loadingKey by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var durationMs by mutableStateOf(0)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var player: MediaPlayer? = null

    fun pollProgress() {
        val mp = player ?: return
        if (isPlaying && durationMs > 0) {
            progress = runCatching { (mp.currentPosition.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) }.getOrDefault(0f)
        }
    }

    suspend fun togglePlay(
        key: String,
        mediaUrl: String,
        fileName: String?,
        cacheDir: File,
        chatRepository: ChatRepository,
    ) {
        errorMessage = null
        if (activeKey == key && player != null) {
            val mp = player!!
            if (mp.isPlaying) {
                mp.pause()
                isPlaying = false
            } else {
                mp.start()
                isPlaying = true
            }
            return
        }
        stop()
        loadingKey = key
        try {
            val ext = fileName?.substringAfterLast('.', "")?.ifBlank { "mp3" } ?: "mp3"
            val file = chatRepository.downloadMediaToCache(
                cacheDir = cacheDir,
                cacheKey = "audio_$key",
                pathOrUrl = mediaUrl,
                extension = ext,
            )
            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    this@ChatAudioPlayerController.isPlaying = false
                    this@ChatAudioPlayerController.progress = 0f
                    this@ChatAudioPlayerController.activeKey = null
                }
            }
            player = mp
            activeKey = key
            durationMs = mp.duration.coerceAtLeast(0)
            progress = 0f
            mp.start()
            isPlaying = true
        } catch (e: Exception) {
            errorMessage = e.message ?: "语音暂无法播放"
            stop()
        } finally {
            loadingKey = null
        }
    }

    fun stop() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        activeKey = null
        isPlaying = false
        progress = 0f
        durationMs = 0
    }
}

// ==================== Image / Sticker / OCR Gallery ====================

@Composable
fun ChatImageGalleryBlock(
    messageId: String,
    images: List<ChatAttachmentDto>,
    missingCount: Int,
    endpoints: SiteEndpoints,
    onPreviewGallery: (urls: List<String>, initialIndex: Int, titles: List<String?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    if (images.isEmpty() && missingCount <= 0) return

    val fullUrls = remember(messageId, images) {
        images.mapNotNull { att ->
            endpoints.resolveApiMediaUrl(att.url ?: att.thumbUrl ?: "/api/v1/chat/media/$messageId/${att.index}")
        }
    }
    val titles = remember(images) { images.map { friendlyMediaDisplayName(it.name, "聊天图片") } }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (images.size == 1 && missingCount == 0) {
            val att = images.first()
            SingleChatImage(
                messageId = messageId,
                att = att,
                endpoints = endpoints,
                onClick = {
                    if (fullUrls.isNotEmpty()) onPreviewGallery(fullUrls, 0, titles)
                },
            )
        } else if (images.isNotEmpty()) {
            val cols = if (images.size == 2 || images.size == 4) 2 else 3
            val rows = images.chunked(cols)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                rows.forEachIndexed { rowIndex, rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowItems.forEachIndexed { colIndex, att ->
                            val flatIndex = rowIndex * cols + colIndex
                            val isFace = att.kind.equals("face", ignoreCase = true)
                            val cellSize = if (isFace) 52.dp else if (cols == 2) 104.dp else 72.dp
                            val imageCandidates = remember(messageId, att, endpoints) {
                                buildList {
                                    endpoints.resolveApiMediaUrl(att.thumbUrl)?.let { add(it) }
                                    endpoints.resolveApiMediaUrl(att.url)?.let { add(it) }
                                    endpoints.resolveApiMediaUrl("/api/v1/chat/media/$messageId/${att.index}?variant=thumb")?.let { add(it) }
                                    endpoints.resolveApiMediaUrl("/api/v1/chat/media/$messageId/${att.index}")?.let { add(it) }
                                }.distinct()
                            }
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.tileSoft)
                                    .border(0.5.dp, colors.line, RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (fullUrls.isNotEmpty()) {
                                            onPreviewGallery(fullUrls, flatIndex.coerceAtMost(fullUrls.lastIndex), titles)
                                        }
                                    },
                            ) {
                                ResilientChatImage(
                                    candidates = imageCandidates,
                                    contentDescription = friendlyMediaDisplayName(att.name, if (isFace) "表情" else "聊天图片"),
                                    contentScale = if (isFace) ContentScale.Fit else ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (att.isAnimated) {
                                    AnimatedGifBadge(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp))
                                }
                            }
                        }
                    }
                }
            }
            // Any OCR texts attached to multi-image messages
            images.forEachIndexed { idx, att ->
                val ocr = att.ocrText
                if (!ocr.isNullOrBlank()) {
                    CollapsibleOcrChip(ocrText = ocr, label = "图 ${idx + 1} 文字")
                }
            }
        }

        if (missingCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(missingCount.coerceAtMost(4)) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.tileSoft)
                            .border(1.dp, colors.line, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HideImage,
                            contentDescription = "图片不可显示",
                            tint = colors.textMuted,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResilientChatImage(
    candidates: List<String>,
    contentDescription: String,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    var attemptIndex by remember(candidates) { mutableIntStateOf(0) }
    val activeUrl = candidates.getOrNull(attemptIndex)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (activeUrl != null) {
            AsyncImage(
                model = activeUrl,
                contentDescription = contentDescription,
                contentScale = contentScale,
                onError = {
                    if (attemptIndex < candidates.size) {
                        attemptIndex += 1
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.HideImage,
                contentDescription = "图片加载失败",
                tint = colors.textMuted,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { attemptIndex = 0 },
            )
        }
    }
}

@Composable
private fun SingleChatImage(
    messageId: String,
    att: ChatAttachmentDto,
    endpoints: SiteEndpoints,
    onClick: () -> Unit,
) {
    val colors = IgngTheme.colors
    val kind = att.kind.lowercase()
    val isFace = kind == "face" || kind == "emoji"
    val isMface = kind == "mface" || kind == "sticker"
    val imageCandidates = remember(messageId, att, endpoints) {
        buildList {
            if (att.isAnimated) {
                endpoints.resolveApiMediaUrl(att.url)?.let { add(it) }
                endpoints.resolveApiMediaUrl(att.thumbUrl)?.let { add(it) }
                endpoints.resolveApiMediaUrl("/api/v1/chat/media/$messageId/${att.index}")?.let { add(it) }
                endpoints.resolveApiMediaUrl("/api/v1/chat/media/$messageId/${att.index}?variant=thumb")?.let { add(it) }
            } else {
                endpoints.resolveApiMediaUrl(att.thumbUrl)?.let { add(it) }
                endpoints.resolveApiMediaUrl(att.url)?.let { add(it) }
                endpoints.resolveApiMediaUrl("/api/v1/chat/media/$messageId/${att.index}?variant=thumb")?.let { add(it) }
                endpoints.resolveApiMediaUrl("/api/v1/chat/media/$messageId/${att.index}")?.let { add(it) }
            }
        }.distinct()
    }

    val imgWidth = att.width
    val imgHeight = att.height
    val aspect = if (imgWidth != null && imgHeight != null && imgWidth > 0 && imgHeight > 0) {
        (imgWidth.toFloat() / imgHeight.toFloat()).coerceIn(0.45f, 2.2f)
    } else {
        null
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val boxMod = when {
            isFace -> Modifier.size(48.dp)
            isMface -> Modifier.size(120.dp)
            aspect != null -> Modifier.widthIn(min = 100.dp, max = 220.dp).heightIn(max = 240.dp).aspectRatio(aspect)
            else -> Modifier.widthIn(min = 120.dp, max = 210.dp).heightIn(min = 96.dp, max = 210.dp)
        }
        val corner = if (isFace || isMface) RoundedCornerShape(10.dp) else RoundedCornerShape(14.dp)
        Box(
            modifier = boxMod
                .clip(corner)
                .background(if (isFace || isMface) Color.Transparent else colors.tileSoft)
                .then(if (isFace || isMface) Modifier else Modifier.border(0.5.dp, colors.line, corner))
                .clickable(onClick = onClick),
        ) {
            ResilientChatImage(
                candidates = imageCandidates,
                contentDescription = friendlyMediaDisplayName(att.name, if (isFace) "表情" else "聊天图片"),
                contentScale = if (isFace || isMface) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (att.isAnimated) {
                AnimatedGifBadge(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp))
            }
        }
        val singleOcr = att.ocrText
        if (!singleOcr.isNullOrBlank()) {
            CollapsibleOcrChip(ocrText = singleOcr)
        }
    }
}

@Composable
private fun AnimatedGifBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text("动图", style = IgngTheme.type.badge, color = Color.White)
    }
}

@Composable
private fun CollapsibleOcrChip(
    ocrText: String,
    label: String = "图中文字",
) {
    val colors = IgngTheme.colors
    var expanded by remember(ocrText) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .clip(IgngTheme.radii.pill)
                .background(colors.tileSoft)
                .border(0.5.dp, colors.line, IgngTheme.radii.pill)
                .clickable { expanded = !expanded }
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.DocumentScanner,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = if (expanded) "收起识别文字" else label,
                style = IgngTheme.type.badge,
                color = colors.textSecondary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.tileSoft)
                    .border(0.5.dp, colors.line, RoundedCornerShape(8.dp))
                    .padding(8.dp),
            ) {
                Text(
                    text = ocrText,
                    style = IgngTheme.type.caption,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

// ==================== Voice Audio Bubble + Transcript ====================

@Composable
fun ChatVoiceCard(
    messageId: String,
    att: ChatAttachmentDto,
    audioController: ChatAudioPlayerController,
    chatRepository: ChatRepository,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val key = "${messageId}_${att.index}"
    val isActive = audioController.activeKey == key
    val isLoading = audioController.loadingKey == key
    val isPlaying = isActive && audioController.isPlaying
    val mediaPath = att.url ?: "/api/v1/chat/media/$messageId/${att.index}"
    var expandTranscript by remember(att.transcript) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .widthIn(min = 164.dp, max = 240.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.tileSoft.copy(alpha = 0.7f))
            .border(0.5.dp, colors.line, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    scope.launch {
                        audioController.togglePlay(
                            key = key,
                            mediaUrl = mediaPath,
                            fileName = att.name,
                            cacheDir = context.cacheDir,
                            chatRepository = chatRepository,
                        )
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colors.accent,
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "暂停语音" else "播放语音",
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = if (isPlaying) colors.accent else colors.textSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = if (isActive && audioController.durationMs > 0) {
                            "${(audioController.durationMs / 1000).coerceAtLeast(1)}″"
                        } else {
                            friendlyMediaDisplayName(att.name, "语音消息")
                        },
                        style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Medium),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isActive) {
                    LinearProgressIndicator(
                        progress = { audioController.progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(IgngTheme.radii.pill),
                        color = colors.accent,
                        trackColor = colors.line,
                    )
                }
            }
            if (isActive) {
                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = "停止",
                    tint = colors.textMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { audioController.stop() },
                )
            }
        }

        val transcriptText = att.transcript
        if (!transcriptText.isNullOrBlank()) {
            HorizontalDivider(thickness = 0.5.dp, color = colors.line)
            Text(
                text = "转写：$transcriptText",
                style = IgngTheme.type.caption,
                color = colors.textSecondary,
                maxLines = if (expandTranscript) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { expandTranscript = !expandTranscript },
            )
        }
    }
}

// ==================== Video Preview Card + Native Player Dialog ====================

@Composable
fun ChatVideoCard(
    messageId: String,
    att: ChatAttachmentDto,
    endpoints: SiteEndpoints,
    onPlayVideo: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    val fullUrl = att.url ?: "/api/v1/chat/media/$messageId/${att.index}"
    val thumbUrl = endpoints.resolveApiMediaUrl(att.thumbUrl)
    val title = friendlyMediaDisplayName(att.name, "视频消息")

    Box(
        modifier = modifier
            .widthIn(min = 180.dp, max = 220.dp)
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF10151E))
            .border(0.5.dp, colors.line, RoundedCornerShape(12.dp))
            .clickable { onPlayVideo(fullUrl, title) },
        contentAlignment = Alignment.Center,
    ) {
        if (thumbUrl != null && thumbUrl != endpoints.resolveApiMediaUrl(fullUrl)) {
            AsyncImage(
                model = thumbUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "播放视频",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.52f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Rounded.VideoFile, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(13.dp))
            Text(
                text = title,
                style = IgngTheme.type.badge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ChatVideoPlayerDialog(
    mediaPath: String,
    title: String,
    chatRepository: ChatRepository,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var localFile by remember(mediaPath) { mutableStateOf<File?>(null) }
    var loading by remember(mediaPath) { mutableStateOf(true) }
    var error by remember(mediaPath) { mutableStateOf<String?>(null) }

    LaunchedEffect(mediaPath) {
        loading = true
        error = null
        runCatching {
            val ext = title.substringAfterLast('.', "").ifBlank { "mp4" }
            chatRepository.downloadMediaToCache(
                cacheDir = context.cacheDir,
                cacheKey = "video_${mediaPath.hashCode()}",
                pathOrUrl = mediaPath,
                extension = ext,
            )
        }.onSuccess {
            localFile = it
        }.onFailure {
            error = it.message ?: "视频加载失败"
        }
        loading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            val errMsg = error
            val videoFile = localFile
            when {
                loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
                errMsg != null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(errMsg, style = IgngTheme.type.body, color = Color.White)
                    IgngButton(text = "关闭", onClick = onDismiss, style = IgngButtonStyle.Secondary, compact = true)
                }
                videoFile != null -> AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val controller = MediaController(ctx)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setVideoPath(videoFile.absolutePath)
                            setOnPreparedListener { start() }
                        }
                    },
                    modifier = Modifier.fillMaxSize().align(Alignment.Center),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = IgngTheme.type.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Color.White)
                }
            }
        }
    }
}

// ==================== File Attachment Card ====================

@Composable
fun ChatFileCardList(
    messageId: String,
    files: List<ChatFileEntry>,
    chatRepository: ChatRepository,
    modifier: Modifier = Modifier,
) {
    if (files.isEmpty()) return
    val colors = IgngTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloadingIdx by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        files.forEachIndexed { idx, file ->
            val ext = file.name.substringAfterLast('.', "").lowercase()
            val (icon, tint) = fileIconAndTint(ext, colors.accent)
            val available = !file.url.isNullOrBlank()
            val metaText = buildList {
                if (ext.isNotBlank() && ext.length <= 8) add(ext.uppercase())
                val sizeStr = formatFileSize(file.sizeBytes)
                if (sizeStr.isNotBlank()) add(sizeStr)
                if (!available) add("已失效 · 无法下载")
            }.joinToString(" · ").ifBlank { if (available) "点击下载" else "无法下载" }

            Row(
                modifier = Modifier
                    .widthIn(min = 196.dp, max = 250.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (available) colors.tileSoft else colors.tileSoft.copy(alpha = 0.5f))
                    .border(0.5.dp, colors.line, RoundedCornerShape(12.dp))
                    .then(
                        if (available && downloadingIdx == null) {
                            Modifier.clickable {
                                scope.launch {
                                    downloadingIdx = idx
                                    runCatching {
                                        val cached = chatRepository.downloadMediaToCache(
                                            cacheDir = context.cacheDir,
                                            cacheKey = "file_${messageId}_${file.index}_$idx",
                                            pathOrUrl = file.url!!,
                                            extension = ext.ifBlank { "bin" },
                                        )
                                        saveFileToDownloads(context, cached, file.name.ifBlank { "attachment_$idx" }, file.mimeType)
                                    }.onSuccess { savedName ->
                                        Toast.makeText(context, "已保存至下载目录：$savedName", Toast.LENGTH_SHORT).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, err.message ?: "文件下载失败", Toast.LENGTH_SHORT).show()
                                    }
                                    downloadingIdx = null
                                }
                            }
                        } else {
                            Modifier
                        },
                    )
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(tint.copy(alpha = if (colors.isDark) 0.22f else 0.13f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = file.name.ifBlank { "未命名文件" },
                        style = IgngTheme.type.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = if (available) colors.textPrimary else colors.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = metaText,
                        style = IgngTheme.type.badge,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (downloadingIdx == idx) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.accent)
                } else if (available) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "下载文件",
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

private fun fileIconAndTint(ext: String, defaultAccent: Color): Pair<ImageVector, Color> = when (ext) {
    "mp4", "mov", "mkv", "avi", "webm" -> Icons.Rounded.VideoFile to Color(0xFF8B5CF6)
    "mp3", "wav", "flac", "aac", "ogg", "m4a", "amr", "silk" -> Icons.Rounded.AudioFile to Color(0xFFEC4899)
    "zip", "rar", "7z", "tar", "gz", "mrpack", "jar", "apk" -> Icons.Rounded.FolderZip to Color(0xFFF59E0B)
    "txt", "md", "log", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv" -> Icons.Rounded.Description to defaultAccent
    "json", "yml", "yaml", "toml", "xml", "js", "ts", "py", "java", "kt", "sh" -> Icons.Rounded.Code to Color(0xFF10B981)
    else -> Icons.AutoMirrored.Rounded.InsertDriveFile to Color(0xFF64748B)
}

private suspend fun saveFileToDownloads(
    context: Context,
    sourceFile: File,
    displayName: String,
    mimeType: String?,
): String = withContext(Dispatchers.IO) {
    val cleanName = displayName.replace(Regex("[/\\\\]"), "_").ifBlank { "igng_file" }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, cleanName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType ?: "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/IGNGmc")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("无法创建下载目标文件")
        context.contentResolver.openOutputStream(uri)?.use { out ->
            sourceFile.inputStream().use { inp -> inp.copyTo(out) }
        } ?: throw IllegalStateException("无法写入下载文件")
        cleanName
    } else {
        cleanName
    }
}

// ==================== Platform-Specific Structured Cards (WeChat / QQ) ====================

@Composable
fun ChatStructuredCard(
    messageId: String,
    part: ChatStructurePartDto,
    endpoints: SiteEndpoints,
    onPreviewGallery: (urls: List<String>, initialIndex: Int, titles: List<String?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    val semantic = chatSemanticColors(colors.isDark)
    val context = LocalContext.current
    val cardType = part.cardType?.lowercase().orEmpty()
    val safeUrl = ChatPlatformRules.safeExternalUrl(part.url)

    when (cardType) {
        "red_packet" -> {
            // WeChat Red Packet Card
            Column(
                modifier = modifier
                    .widthIn(min = 210.dp, max = 250.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.5.dp, colors.line, RoundedCornerShape(12.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(semantic.redPacketTop, semantic.redPacketBottom)))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FDE047)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.CardGiftcard, null, tint = Color(0xFFFDE047), modifier = Modifier.size(21.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = part.title.ifBlank { "恭喜发财，大吉大利" },
                            style = IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val sub = part.description.ifBlank { part.summary }
                        if (sub.isNotBlank() && sub != part.title) {
                            Text(
                                text = sub,
                                style = IgngTheme.type.badge,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.tile)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("微信红包 · 档案记录", style = IgngTheme.type.badge, color = colors.textMuted)
                }
            }
        }

        "transfer" -> {
            // WeChat Transfer Card
            Column(
                modifier = modifier
                    .widthIn(min = 210.dp, max = 250.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.5.dp, colors.line, RoundedCornerShape(12.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(semantic.transferAccent)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = part.title.ifBlank { "微信转账" },
                            style = IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val sub = part.description.ifBlank { part.summary }
                        if (sub.isNotBlank() && sub != part.title) {
                            Text(
                                text = sub,
                                style = IgngTheme.type.badge,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.tile)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("微信转账 · 档案记录", style = IgngTheme.type.badge, color = colors.textMuted)
                }
            }
        }

        "contact" -> {
            // Personal Contact Card
            Column(
                modifier = modifier
                    .widthIn(min = 196.dp, max = 240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.tileSoft)
                    .border(0.5.dp, colors.line, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.AccountBox, null, tint = colors.accent, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = part.title.ifBlank { "个人名片" },
                            style = IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val sub = part.description.ifBlank { part.summary }
                        if (sub.isNotBlank()) {
                            Text(sub, style = IgngTheme.type.badge, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = colors.line)
                Text("个人名片", style = IgngTheme.type.badge, color = colors.textMuted)
            }
        }

        else -> {
            // miniapp / channels / link / json / generic share card
            val (kindLabel, kindIcon, kindTint) = when (cardType) {
                "miniapp" -> Triple("小程序", Icons.Rounded.Widgets, semantic.wechat)
                "channels" -> Triple("微信视频号", Icons.Rounded.SmartDisplay, semantic.transferAccent)
                "link" -> Triple("链接分享", Icons.Rounded.Link, colors.accent)
                "file" -> Triple("文件卡片", Icons.Rounded.Description, colors.accent)
                else -> Triple("分享卡片", Icons.Rounded.Widgets, colors.accent)
            }
            val desc = part.description.ifBlank { part.summary }
            val imageAtts = part.attachments.filter(::isImageAttachment)

            Column(
                modifier = modifier
                    .widthIn(min = 200.dp, max = 256.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.tileSoft)
                    .border(0.5.dp, colors.line, RoundedCornerShape(12.dp))
                    .then(
                        if (safeUrl != null) {
                            Modifier.clickable { CustomTabsLauncher.openExternalUrl(context, safeUrl, colors.isDark) }
                        } else {
                            Modifier
                        },
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(kindIcon, null, tint = kindTint, modifier = Modifier.size(14.dp))
                    Text(kindLabel, style = IgngTheme.type.badge, color = kindTint)
                }
                if (part.title.isNotBlank()) {
                    Text(
                        text = part.title,
                        style = IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (desc.isNotBlank() && desc != part.title) {
                    Text(
                        text = desc,
                        style = IgngTheme.type.caption,
                        color = colors.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (imageAtts.isNotEmpty()) {
                    ChatImageGalleryBlock(
                        messageId = messageId,
                        images = imageAtts,
                        missingCount = 0,
                        endpoints = endpoints,
                        onPreviewGallery = onPreviewGallery,
                    )
                }
                if (safeUrl != null) {
                    HorizontalDivider(thickness = 0.5.dp, color = colors.line)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("打开链接", style = IgngTheme.type.badge, color = colors.accent)
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = colors.accent, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }
    }
}

// ==================== Merged Forward Summary Card & Multi-Layer Viewer Sheet ====================

@Composable
fun ChatForwardSummaryCard(
    part: ChatStructurePartDto?,
    fallbackSummary: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    val previewChildren = part?.children.orEmpty().take(3)
    val count = part?.childCount?.takeIf { it > 0 } ?: part?.children?.size ?: 0

    Column(
        modifier = modifier
            .widthIn(min = 210.dp, max = 256.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.tileSoft)
            .border(0.5.dp, colors.line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.Forum, null, tint = colors.accent, modifier = Modifier.size(15.dp))
            Text(
                text = part?.summary?.takeIf { it.isNotBlank() } ?: "聊天记录",
                style = IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (previewChildren.isNotEmpty()) {
            previewChildren.forEach { child ->
                val lineText = child.content.replace(Regex("\\s+"), " ").trim()
                    .ifBlank { if (child.attachments.isNotEmpty()) "[媒体消息]" else "[转发内容]" }
                Text(
                    text = if (child.nickname.isNotBlank()) "${child.nickname}：$lineText" else lineText,
                    style = IgngTheme.type.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else if (!fallbackSummary.isNullOrBlank()) {
            Text(
                text = fallbackSummary,
                style = IgngTheme.type.caption,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = colors.line)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (count > 0) "查看 $count 条转发消息" else "查看聊天记录",
                style = IgngTheme.type.badge,
                color = colors.textMuted,
            )
            Icon(Icons.Rounded.ChevronRight, null, tint = colors.textMuted, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
fun ForwardViewerSheet(
    messageId: String,
    loading: Boolean,
    detail: ChatForwardResponse?,
    error: String?,
    endpoints: SiteEndpoints,
    chatRepository: ChatRepository,
    onRetry: () -> Unit,
    onPreviewGallery: (urls: List<String>, initialIndex: Int, titles: List<String?>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = IgngTheme.colors
    val semantic = chatSemanticColors(colors.isDark)

    val rootChildren: List<ChatForwardChildDto> = remember(detail) {
        if (detail == null) return@remember emptyList()
        val fromStructure = detail.structure.firstOrNull(::isForwardPart)?.children.orEmpty()
        if (fromStructure.isNotEmpty()) {
            fromStructure
        } else {
            detail.resolvedNodes.map { node ->
                ChatForwardChildDto(
                    senderId = node.senderId,
                    nickname = node.author,
                    content = node.content,
                    createdAt = node.sent_at,
                )
            }
        }
    }

    var stack by remember(detail) { mutableStateOf<List<Pair<String, List<ChatForwardChildDto>>>>(emptyList()) }
    val currentTitle = stack.lastOrNull()?.first ?: detail?.title ?: "聊天记录"
    val currentEntries = stack.lastOrNull()?.second ?: rootChildren
    val depth = stack.size

    IgngDetailSheet(
        onDismiss = onDismiss,
        title = currentTitle,
        subtitle = buildString {
            if (depth > 0) append("第 ${depth + 1} 层 · ")
            if (!loading && error == null) append("共 ${currentEntries.size} 条消息")
        }.ifBlank { null },
    ) {
        if (depth > 0) {
            Row(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .clip(IgngTheme.radii.pill)
                    .background(colors.tileSoft)
                    .border(1.dp, colors.line, IgngTheme.radii.pill)
                    .clickable { stack = stack.dropLast(1) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = colors.accent, modifier = Modifier.size(15.dp))
                Text("返回上一层", style = IgngTheme.type.caption.copy(fontWeight = FontWeight.SemiBold), color = colors.accent)
            }
        }

        when {
            loading -> IgngLoadingBlock(compact = true)
            error != null -> Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(error, style = IgngTheme.type.body, color = colors.danger)
                IgngButton(text = "重试", onClick = onRetry, style = IgngButtonStyle.Secondary, compact = true)
            }
            currentEntries.isEmpty() -> Text(
                text = "该转发消息没有可展示的子消息。",
                style = IgngTheme.type.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(currentEntries) { idx, child ->
                    val name = child.nickname.ifBlank { "匿名" }
                    val toneKey = child.senderId?.takeIf { ChatPlatformRules.isNumericQqId(it) }
                        ?.let { "qq:$it" } ?: "fwd:$name"
                    val tone = semantic.tones[ChatPlatformRules.identityToneIndex(toneKey)]
                    val avatarUrl = child.senderId?.takeIf { ChatPlatformRules.isNumericQqId(it) }
                        ?.let { "https://q1.qlogo.cn/g?b=qq&nk=$it&s=100" }
                    val childParsed = remember(child) {
                        extractMessageDisplay(
                            ChatMessageDto(
                                id = messageId,
                                source = "qq",
                                platform = "qq",
                                content = child.content,
                                attachments = child.attachments,
                                messageStructure = child.children,
                            ),
                        )
                    }
                    val nestedCards = childParsed.cards
                    val nestedForwards = childParsed.forwards
                    val childImages = childParsed.images
                    val childFiles = childParsed.files

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(tone.avatarBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold),
                                color = tone.avatarFg,
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
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = name,
                                    style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = tone.nameText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (!child.createdAt.isNullOrBlank()) {
                                    Text(
                                        text = formatMessageClock(child.createdAt),
                                        style = IgngTheme.type.badge,
                                        color = colors.textMuted,
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                                    .background(colors.tileSoft)
                                    .border(0.5.dp, colors.line, RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                                    .padding(horizontal = 11.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (childParsed.text.isNotBlank() && nestedForwards.isEmpty()) {
                                    Text(
                                        text = childParsed.text,
                                        style = IgngTheme.type.body,
                                        color = colors.textPrimary,
                                    )
                                }
                                if (childImages.isNotEmpty() || childParsed.missingImages > 0) {
                                    ChatImageGalleryBlock(
                                        messageId = messageId,
                                        images = childImages,
                                        missingCount = childParsed.missingImages,
                                        endpoints = endpoints,
                                        onPreviewGallery = onPreviewGallery,
                                    )
                                }
                                if (childFiles.isNotEmpty()) {
                                    ChatFileCardList(
                                        messageId = messageId,
                                        files = childFiles,
                                        chatRepository = chatRepository,
                                    )
                                }
                                nestedCards.forEach { card ->
                                    ChatStructuredCard(
                                        messageId = messageId,
                                        part = card,
                                        endpoints = endpoints,
                                        onPreviewGallery = onPreviewGallery,
                                    )
                                }
                                nestedForwards.forEach { fwd ->
                                    ChatForwardSummaryCard(
                                        part = fwd,
                                        fallbackSummary = fwd.summary,
                                        onClick = {
                                            stack = stack + ((fwd.summary.ifBlank { "嵌套聊天记录" }) to fwd.children)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
