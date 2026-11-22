package net.igng.mcstatus.feature.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.igng.mcstatus.core.auth.CustomTabsLauncher
import net.igng.mcstatus.core.data.ChatMessageDto
import net.igng.mcstatus.core.data.ChatReplyPreviewDto
import net.igng.mcstatus.core.data.ChatRepository
import net.igng.mcstatus.core.data.ChatSenderBadgeDto
import net.igng.mcstatus.core.designsystem.IgngBadge
import net.igng.mcstatus.core.designsystem.IgngBadgeTone
import net.igng.mcstatus.core.designsystem.IgngMcFormattedText
import net.igng.mcstatus.core.designsystem.IgngMotion
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.model.SiteEndpoints

// ==================== Centered Dividers & System Notices ====================

@Composable
fun ChatDayHeaderChip(label: String, modifier: Modifier = Modifier) {
    val colors = IgngTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(IgngTheme.radii.pill)
                .background(colors.tile.copy(alpha = if (colors.isDark) 0.72f else 0.88f))
                .border(0.5.dp, colors.line, IgngTheme.radii.pill)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = label,
                style = IgngTheme.type.badge,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
fun ChatTimeDividerChip(label: String, modifier: Modifier = Modifier) {
    val colors = IgngTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = IgngTheme.type.badge,
            color = colors.textMuted,
        )
    }
}

@Composable
fun ChatSystemNoticePill(text: String, modifier: Modifier = Modifier) {
    val colors = IgngTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(IgngTheme.radii.pill)
                .background(colors.tileSoft.copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = text,
                style = IgngTheme.type.caption,
                color = colors.textMuted,
            )
        }
    }
}

// ==================== Sender Avatar ====================

@Composable
fun ChatSenderAvatar(
    sender: ResolvedChatSender,
    size: Dp = 36.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    val semantic = chatSemanticColors(colors.isDark)
    val tone = semantic.tones[sender.toneIndex.coerceIn(0, semantic.tones.lastIndex)]
    val shape = when {
        sender.isMc -> RoundedCornerShape(7.dp)
        else -> RoundedCornerShape(10.dp)
    }
    val bg = when {
        sender.isBot -> semantic.botSoft
        sender.isAccountSelf -> colors.tileSoft
        else -> tone.avatarBg
    }
    val fg = when {
        sender.isBot -> semantic.bot
        sender.isAccountSelf -> colors.textSecondary
        else -> tone.avatarFg
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bg)
            .border(0.5.dp, colors.line, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (sender.isBot) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "云萤",
                tint = fg,
                modifier = Modifier.size(size * 0.52f),
            )
        } else {
            Text(
                text = sender.initials,
                style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold),
                color = fg,
            )
        }
        if (!sender.avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = sender.avatarUrl,
                contentDescription = sender.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ==================== Message Group (WeChat/QQ Mobile Bubbles) ====================

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChatMessageGroupBlock(
    group: ChatTimelineItem.MessageGroup,
    isPrivateChat: Boolean,
    focusedMessageId: String?,
    highlightKeyword: String,
    endpoints: SiteEndpoints,
    chatRepository: ChatRepository,
    audioController: ChatAudioPlayerController,
    onSenderClick: (ResolvedChatSender) -> Unit,
    onLongPressMessage: (ChatMessageDto, ResolvedChatSender) -> Unit,
    onJumpToMessage: (String) -> Unit,
    onPreviewGallery: (urls: List<String>, initialIndex: Int, titles: List<String?>) -> Unit,
    onOpenForward: (ChatMessageDto) -> Unit,
    onPlayVideo: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    val semantic = chatSemanticColors(colors.isDark)
    val sender = group.sender
    val tone = semantic.tones[sender.toneIndex.coerceIn(0, semantic.tones.lastIndex)]
    val canOpenSender = sender.boundIgngId != null || !sender.mcUsername.isNullOrBlank()
    val showHeader = !sender.isMine && (!isPrivateChat || sender.isAccountSelf || sender.badges.isNotEmpty() || sender.relationship != null)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        group.messages.forEachIndexed { index, msg ->
            val isFirst = index == 0
            val isLast = index == group.messages.lastIndex
            val isFocused = !focusedMessageId.isNullOrBlank() && msg.id == focusedMessageId
            val parsed = remember(msg) { extractMessageDisplay(msg) }
            val isRecalled = msg.source == "qq" && msg.isRecalled

            val focusBg by animateColorAsState(
                targetValue = if (isFocused) colors.accentSoft else Color.Transparent,
                animationSpec = IgngMotion.normalTween(IgngTheme.motionReduced),
                label = "msgFocusBg",
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(focusBg)
                    .padding(vertical = if (isFocused) 4.dp else 0.dp),
                horizontalArrangement = if (sender.isMine) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Top,
            ) {
                if (!sender.isMine) {
                    if (isFirst) {
                        ChatSenderAvatar(
                            sender = sender,
                            onClick = if (canOpenSender) ({ onSenderClick(sender) }) else null,
                        )
                    } else {
                        Spacer(Modifier.width(36.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }

                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalAlignment = if (sender.isMine) Alignment.End else Alignment.Start,
                ) {
                    if (isFirst && showHeader) {
                        FlowRow(
                            modifier = Modifier
                                .padding(start = 2.dp, bottom = 4.dp)
                                .then(if (canOpenSender) Modifier.clickable { onSenderClick(sender) } else Modifier),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = sender.displayName,
                                style = IgngTheme.type.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = if (sender.isBot) semantic.bot else tone.nameText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!sender.secondaryName.isNullOrBlank()) {
                                Text(
                                    text = sender.secondaryName,
                                    style = IgngTheme.type.badge,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            prioritizedInlineBadges(sender.badges, limit = 2).forEach { badge ->
                                ChatInlineBadgeChip(badge = badge, semantic = semantic)
                            }
                            if (sender.relationship != null) {
                                CompactTagPill(
                                    text = "关系: ${sender.relationship.label}",
                                    bg = semantic.relationshipBg,
                                    fg = semantic.relationshipFg,
                                )
                            }
                            if (sender.isAccountSelf && !sender.sourceLabel.isNullOrBlank()) {
                                CompactTagPill(
                                    text = sender.sourceLabel,
                                    bg = if (sender.isBot) semantic.botSoft else colors.tileSoft,
                                    fg = if (sender.isBot) semantic.bot else colors.textMuted,
                                )
                            }
                        }
                    }

                    // Bubble corner shaping for consecutive grouped messages
                    val bubbleShape = bubbleCornerShape(isMine = sender.isMine, isFirst = isFirst, isLast = isLast)
                    val bubbleBg = when {
                        sender.isMine -> semantic.selfBubbleBg
                        sender.isBot -> semantic.botBubbleBg
                        sender.isService -> semantic.serviceBubbleBg
                        else -> semantic.otherBubbleBg
                    }
                    val bubbleBorder = when {
                        sender.isMine -> semantic.selfBubbleBorder
                        sender.isBot -> semantic.botBubbleBorder
                        else -> semantic.otherBubbleBorder
                    }

                    val hasTopFlags = msg.is_at_all || isRecalled
                    val isPureBorderlessImages = parsed.isPureImages && msg.replyTo == null && !hasTopFlags && !msg.has_forward

                    if (isPureBorderlessImages) {
                        Box(
                            modifier = Modifier
                                .alpha(if (isRecalled) 0.78f else 1f)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { onLongPressMessage(msg, sender) },
                                ),
                        ) {
                            ChatImageGalleryBlock(
                                messageId = msg.id,
                                images = parsed.images,
                                missingCount = 0,
                                endpoints = endpoints,
                                onPreviewGallery = onPreviewGallery,
                            )
                        }
                    } else {
                        val dashColor = colors.textMuted.copy(alpha = 0.65f)
                        Column(
                            modifier = Modifier
                                .widthIn(max = 278.dp)
                                .alpha(if (isRecalled) 0.78f else 1f)
                                .clip(bubbleShape)
                                .background(bubbleBg)
                                .then(
                                    if (isRecalled) {
                                        Modifier.drawBehind {
                                            drawRoundRect(
                                                color = dashColor,
                                                cornerRadius = CornerRadius(14.dp.toPx()),
                                                style = Stroke(
                                                    width = 1.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                                                ),
                                            )
                                        }
                                    } else {
                                        Modifier.border(0.8.dp, bubbleBorder, bubbleShape)
                                    },
                                )
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { onLongPressMessage(msg, sender) },
                                )
                                .padding(horizontal = 11.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            // Top status flags (@全体 / 已撤回)
                            if (hasTopFlags) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (msg.is_at_all) {
                                        val isWechat = ChatPlatformRules.resolvePlatform(msg.platform, msg.groupId, msg.source) == "wechat"
                                        IgngBadge(if (isWechat) "@所有人" else "@全体成员", tone = IgngBadgeTone.Warning)
                                    }
                                    if (isRecalled) {
                                        val recallSuffix = msg.recalledAt?.let { " · ${formatMessageClock(it)}" }.orEmpty()
                                        CompactTagPill(
                                            text = "已撤回$recallSuffix",
                                            bg = colors.tileSoft,
                                            fg = colors.textMuted,
                                        )
                                    }
                                }
                            }

                            // Reply / Quote bar inside bubble top
                            msg.replyTo?.let { reply ->
                                ChatReplyQuoteBlock(
                                    reply = reply,
                                    onClick = if (reply.id.isNotBlank()) ({ onJumpToMessage(reply.id) }) else null,
                                )
                            }

                            // Main text body
                            if (parsed.text.isNotBlank()) {
                                if (sender.isMc) {
                                    IgngMcFormattedText(
                                        raw = parsed.text,
                                        style = IgngTheme.type.body,
                                        defaultColor = colors.textPrimary,
                                    )
                                } else {
                                    ChatRichMessageText(
                                        text = parsed.text,
                                        highlight = highlightKeyword,
                                    )
                                }
                            }

                            // Images / Stickers / OCR / Missing placeholders
                            if (parsed.images.isNotEmpty() || parsed.missingImages > 0) {
                                ChatImageGalleryBlock(
                                    messageId = msg.id,
                                    images = parsed.images,
                                    missingCount = parsed.missingImages,
                                    endpoints = endpoints,
                                    onPreviewGallery = onPreviewGallery,
                                )
                            }

                            // Voice messages
                            parsed.audios.forEach { audio ->
                                ChatVoiceCard(
                                    messageId = msg.id,
                                    att = audio,
                                    audioController = audioController,
                                    chatRepository = chatRepository,
                                )
                            }

                            // Video messages
                            parsed.videos.forEach { video ->
                                ChatVideoCard(
                                    messageId = msg.id,
                                    att = video,
                                    endpoints = endpoints,
                                    onPlayVideo = onPlayVideo,
                                )
                            }

                            // Files
                            if (parsed.files.isNotEmpty()) {
                                ChatFileCardList(
                                    messageId = msg.id,
                                    files = parsed.files,
                                    chatRepository = chatRepository,
                                )
                            }

                            // Platform-specific cards (WeChat red_packet / transfer / channels / miniapp / link / contact / QQ json)
                            parsed.cards.forEach { card ->
                                ChatStructuredCard(
                                    messageId = msg.id,
                                    part = card,
                                    endpoints = endpoints,
                                    onPreviewGallery = onPreviewGallery,
                                )
                            }

                            // Merged forward cards
                            if (parsed.forwards.isNotEmpty()) {
                                parsed.forwards.forEach { fwd ->
                                    ChatForwardSummaryCard(
                                        part = fwd,
                                        fallbackSummary = msg.forwardSummary,
                                        onClick = { onOpenForward(msg) },
                                    )
                                }
                            } else if (msg.has_forward || msg.content.contains("[合并转发")) {
                                ChatForwardSummaryCard(
                                    part = null,
                                    fallbackSummary = msg.forwardSummary,
                                    onClick = { onOpenForward(msg) },
                                )
                            }
                        }
                    }
                }

                if (sender.isMine) {
                    Spacer(Modifier.width(8.dp))
                    if (isFirst) {
                        ChatSenderAvatar(
                            sender = sender,
                            onClick = if (canOpenSender) ({ onSenderClick(sender) }) else null,
                        )
                    } else {
                        Spacer(Modifier.width(36.dp))
                    }
                }
            }
        }
    }
}

private fun bubbleCornerShape(isMine: Boolean, isFirst: Boolean, isLast: Boolean): RoundedCornerShape {
    val big = 16.dp
    val tail = 4.dp
    val mid = 7.dp
    return if (isMine) {
        RoundedCornerShape(
            topStart = big,
            topEnd = if (isFirst) tail else mid,
            bottomEnd = if (isLast) big else mid,
            bottomStart = big,
        )
    } else {
        RoundedCornerShape(
            topStart = if (isFirst) tail else mid,
            topEnd = big,
            bottomEnd = big,
            bottomStart = if (isLast) big else mid,
        )
    }
}

@Composable
private fun ChatReplyQuoteBlock(
    reply: ChatReplyPreviewDto,
    onClick: (() -> Unit)?,
) {
    val colors = IgngTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.canvas.copy(alpha = if (colors.isDark) 0.55f else 0.72f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(26.dp)
                .clip(IgngTheme.radii.pill)
                .background(colors.accent.copy(alpha = 0.75f)),
        )
        Column(modifier = Modifier.weight(1f)) {
            if (reply.senderName.isNotBlank()) {
                Text(
                    text = reply.senderName,
                    style = IgngTheme.type.badge,
                    color = colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = reply.preview.ifBlank { "引用消息" },
                style = IgngTheme.type.caption,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val RICH_TOKEN_REGEX = Regex("(https?://[^\\s<]+)|(@(?:全体成员|所有人|全体|[^\\s@]{1,24}))")

@Composable
fun ChatRichMessageText(
    text: String,
    highlight: String = "",
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    val context = LocalContext.current
    val markBg = if (colors.isDark) Color(0x4DFACC15) else Color(0x61FFC400)

    val annotated = remember(text, highlight, colors.accent, colors.textPrimary, colors.isDark) {
        buildAnnotatedString {
            var cursor = 0
            val matches = RICH_TOKEN_REGEX.findAll(text)
            for (match in matches) {
                if (match.range.first > cursor) {
                    appendHighlighted(text.substring(cursor, match.range.first), highlight, markBg)
                }
                val token = match.value
                if (token.startsWith("http://", ignoreCase = true) || token.startsWith("https://", ignoreCase = true)) {
                    val safe = ChatPlatformRules.safeExternalUrl(token)
                    if (safe != null) {
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = "url",
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = colors.accent,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                ),
                            ) {
                                CustomTabsLauncher.openExternalUrl(context, safe, colors.isDark)
                            },
                        ) {
                            append(token)
                        }
                    } else {
                        append(token)
                    }
                } else {
                    withStyle(
                        SpanStyle(
                            color = colors.accent,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    ) {
                        append(token)
                    }
                }
                cursor = match.range.last + 1
            }
            if (cursor < text.length) {
                appendHighlighted(text.substring(cursor), highlight, markBg)
            }
        }
    }

    Text(
        text = annotated,
        style = IgngTheme.type.body,
        color = colors.textPrimary,
        modifier = modifier,
    )
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendHighlighted(
    segment: String,
    keyword: String,
    markBg: Color,
) {
    if (keyword.isBlank()) {
        append(segment)
        return
    }
    val lowerSeg = segment.lowercase()
    val lowerKey = keyword.lowercase()
    var pos = 0
    while (pos < segment.length) {
        val found = lowerSeg.indexOf(lowerKey, pos)
        if (found < 0) {
            append(segment.substring(pos))
            break
        }
        if (found > pos) append(segment.substring(pos, found))
        withStyle(SpanStyle(background = markBg, fontWeight = FontWeight.SemiBold)) {
            append(segment.substring(found, found + keyword.length))
        }
        pos = found + keyword.length
    }
}

@Composable
private fun ChatInlineBadgeChip(
    badge: ChatSenderBadgeDto,
    semantic: ChatSemanticColors,
) {
    val colors = IgngTheme.colors
    val (bg, fg, label, mono) = when (badge.tone) {
        "danger" -> Quad(colors.dangerBg, colors.danger, badge.label, false)
        "success" -> Quad(colors.successBg, colors.success, badge.label, false)
        "pro" -> Quad(semantic.proBg, semantic.proFg, badge.label, false)
        "plus" -> Quad(semantic.plusBg, semantic.plusFg, badge.label, false)
        "lp" -> Quad(colors.tileSoft, colors.textSecondary, "[${badge.label}]", true)
        else -> Quad(colors.tileSoft, colors.textSecondary, badge.label, false)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text = label,
            style = IgngTheme.type.badge.copy(
                fontSize = 10.sp,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            ),
            color = fg,
            maxLines = 1,
        )
    }
}

@Composable
private fun CompactTagPill(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text = text,
            style = IgngTheme.type.badge.copy(fontSize = 10.sp),
            color = fg,
            maxLines = 1,
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
