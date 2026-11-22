package net.igng.mcstatus.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import net.igng.mcstatus.core.data.ChatMessageDto
import net.igng.mcstatus.core.data.ChatRepository
import net.igng.mcstatus.core.data.ChatSenderProfileDto
import net.igng.mcstatus.core.designsystem.IgngButton
import net.igng.mcstatus.core.designsystem.IgngButtonStyle
import net.igng.mcstatus.core.designsystem.IgngDetailSheet
import net.igng.mcstatus.core.designsystem.IgngLoadingBlock
import net.igng.mcstatus.core.designsystem.IgngSearchField
import net.igng.mcstatus.core.designsystem.IgngStateBlock
import net.igng.mcstatus.core.designsystem.IgngTextField
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.model.SavedAccount

data class ChatFilterState(
    val dayKey: String = "",
    val senderId: String = "",
    val atAll: Boolean = false,
    val kind: String = "",
    val focusedId: String = "",
) {
    val isActive: Boolean
        get() = dayKey.isNotBlank() || senderId.isNotBlank() || atAll || kind.isNotBlank() || focusedId.isNotBlank()
}

// ==================== In-Conversation Search Sheet ====================

@Composable
fun ChatSearchSheet(
    source: String,
    platform: String,
    targetId: String,
    channelTitle: String,
    chatRepository: ChatRepository,
    activeAccount: SavedAccount?,
    boundMcUsernames: Set<String>,
    onSelectMessage: (messageId: String, keyword: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = IgngTheme.colors
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var submittedKeyword by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, ChatSenderProfileDto>>(emptyMap()) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val directLocateId = remember(query, source) { ChatPlatformRules.parseLocateMessageId(query, source) }

    suspend fun runSearch(term: String, beforeId: String? = null) {
        if (term.isBlank()) return
        loading = true
        error = null
        runCatching {
            chatRepository.fetchMessages(
                source = source,
                serverId = if (source == "server") targetId.toIntOrNull() else null,
                groupId = if (source == "qq") targetId else null,
                query = term,
                beforeId = beforeId,
                limit = 30,
            )
        }.onSuccess { resp ->
            val descending = resp.messages.reversed()
            profiles = profiles + resp.senderProfiles
            results = if (beforeId == null) descending else (results + descending).distinctBy { it.id }
            hasMore = resp.hasMore
            submittedKeyword = term
        }.onFailure { err ->
            error = err.message ?: "搜索失败"
        }
        loading = false
    }

    IgngDetailSheet(
        onDismiss = onDismiss,
        title = "搜索聊天记录",
        subtitle = "当前会话：$channelTitle",
    ) {
        IgngSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = when {
                source == "server" -> "搜索关键词，或输入 #编号"
                platform == "wechat" -> "搜索关键词，或输入 #编号"
                else -> "搜索关键词，或输入 qq-编号 / #编号"
            },
            onSubmit = {
                val loc = ChatPlatformRules.parseLocateMessageId(query, source)
                if (loc != null) {
                    onSelectMessage(loc, "")
                } else if (query.isNotBlank()) {
                    scope.launch { runSearch(query.trim()) }
                }
            },
        )
        Spacer(Modifier.height(10.dp))

        if (directLocateId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(IgngTheme.radii.md)
                    .background(colors.accentSoft)
                    .border(1.dp, colors.accent.copy(alpha = 0.35f), IgngTheme.radii.md)
                    .clickable { onSelectMessage(directLocateId, "") }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Tag, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                Text(
                    text = "直接定位到消息 $directLocateId",
                    style = IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent,
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        when {
            loading && results.isEmpty() -> IgngLoadingBlock(compact = true)
            error != null -> IgngStateBlock(title = "搜索失败", description = error, isError = true)
            submittedKeyword.isNotBlank() && results.isEmpty() -> IgngStateBlock(
                title = "未找到匹配消息",
                description = "没有找到包含「$submittedKeyword」的消息记录。",
            )
            results.isNotEmpty() -> {
                Text(
                    text = if (hasMore) "已显示最近 ${results.size} 条匹配" else "共 ${results.size} 条匹配",
                    style = IgngTheme.type.badge,
                    color = colors.textMuted,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.id }) { msg ->
                        val sender = remember(msg, profiles) {
                            resolveChatSender(msg, profiles, activeAccount, boundMcUsernames)
                        }
                        val parsed = remember(msg) { extractMessageDisplay(msg) }
                        val preview = parsed.text.ifBlank {
                            when {
                                parsed.forwards.isNotEmpty() -> "[聊天记录]"
                                parsed.images.isNotEmpty() -> "[图片]"
                                parsed.files.isNotEmpty() -> "[文件] ${parsed.files.first().name}"
                                else -> msg.content
                            }
                        }.replace(Regex("\\s+"), " ")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(IgngTheme.radii.md)
                                .background(colors.tileSoft)
                                .border(0.5.dp, colors.line, IgngTheme.radii.md)
                                .clickable { onSelectMessage(msg.id, submittedKeyword) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            ChatSenderAvatar(sender = sender, size = 34.dp)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = sender.displayName,
                                        style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = formatFullDateTime(msg.createdAt).take(16),
                                        style = IgngTheme.type.badge,
                                        color = colors.textMuted,
                                    )
                                }
                                Text(
                                    text = preview,
                                    style = IgngTheme.type.caption,
                                    color = colors.textSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    if (hasMore) {
                        item(key = "more") {
                            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                                IgngButton(
                                    text = "加载更早的结果",
                                    onClick = { scope.launch { runSearch(submittedKeyword, results.lastOrNull()?.id) } },
                                    style = IgngButtonStyle.Secondary,
                                    loading = loading,
                                    compact = true,
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Search, null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                    Text(
                        text = "输入关键词后回车搜索当前会话，点击结果可精准定位上下文。",
                        style = IgngTheme.type.caption,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

// ==================== Conversation Filter Sheet ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatFilterSheet(
    source: String,
    platform: String,
    isPrivate: Boolean,
    initial: ChatFilterState,
    onApply: (ChatFilterState) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = IgngTheme.colors
    var dayKey by remember(initial) { mutableStateOf(initial.dayKey) }
    var senderId by remember(initial) { mutableStateOf(initial.senderId) }
    var atAll by remember(initial) { mutableStateOf(initial.atAll) }
    var kind by remember(initial) { mutableStateOf(initial.kind) }
    var showDatePicker by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val dayFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val todayStr = remember(today) { today.format(dayFmt) }
    val yesterdayStr = remember(today) { today.minusDays(1).format(dayFmt) }
    val twoDaysAgoStr = remember(today) { today.minusDays(2).format(dayFmt) }

    IgngDetailSheet(
        onDismiss = onDismiss,
        title = "筛选消息",
        subtitle = "支持按日期、发送者与消息类型精准过滤",
    ) {
        // 1. Day selection
        Text("按天浏览", style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterOptionPill("不限日期", dayKey.isBlank()) { dayKey = "" }
            FilterOptionPill("今天", dayKey == todayStr) { dayKey = todayStr }
            FilterOptionPill("昨天", dayKey == yesterdayStr) { dayKey = yesterdayStr }
            FilterOptionPill("前天", dayKey == twoDaysAgoStr) { dayKey = twoDaysAgoStr }
            FilterOptionPill(
                label = if (dayKey.isNotBlank() && dayKey !in setOf(todayStr, yesterdayStr, twoDaysAgoStr)) {
                    formatShortDayLabel(dayKey)
                } else {
                    "选择日期…"
                },
                selected = dayKey.isNotBlank() && dayKey !in setOf(todayStr, yesterdayStr, twoDaysAgoStr),
                icon = Icons.Rounded.CalendarMonth,
                onClick = { showDatePicker = true },
            )
        }

        // 2. Sender filter (hidden in private chats)
        if (!isPrivate) {
            Spacer(Modifier.height(14.dp))
            val placeholder = when {
                source == "server" -> "输入 MC 玩家名，例如 Steve"
                platform == "wechat" -> "输入微信标识 / wxid"
                else -> "输入 QQ 号，例如 123456789"
            }
            IgngTextField(
                value = senderId,
                onValueChange = { senderId = it },
                label = "按发送者筛选",
                placeholder = placeholder,
            )
        }

        // 3. IM-only filters (@全体 + kind)
        if (source == "qq") {
            Spacer(Modifier.height(14.dp))
            Text("消息类型", style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!isPrivate) {
                    FilterOptionPill(
                        label = if (platform == "wechat") "@所有人" else "@全体成员",
                        selected = atAll,
                        icon = Icons.Rounded.AlternateEmail,
                        onClick = { atAll = !atAll },
                    )
                }
                FilterOptionPill(
                    label = "含图片",
                    selected = kind == "media",
                    icon = Icons.Rounded.Image,
                    onClick = { kind = if (kind == "media") "" else "media" },
                )
                FilterOptionPill(
                    label = "含转发",
                    selected = kind == "forward",
                    icon = Icons.Rounded.Forum,
                    onClick = { kind = if (kind == "forward") "" else "forward" },
                )
                if (!isPrivate) {
                    FilterOptionPill(
                        label = "云萤回复",
                        selected = kind == "bot",
                        icon = Icons.Rounded.AutoAwesome,
                        onClick = { kind = if (kind == "bot") "" else "bot" },
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IgngButton(
                text = "清除筛选",
                onClick = {
                    onReset()
                    onDismiss()
                },
                style = IgngButtonStyle.Secondary,
                modifier = Modifier.weight(1f),
            )
            IgngButton(
                text = "应用筛选",
                onClick = {
                    onApply(
                        ChatFilterState(
                            dayKey = dayKey,
                            senderId = senderId.trim(),
                            atAll = source == "qq" && !isPrivate && atAll,
                            kind = if (source == "qq") kind else "",
                            focusedId = "",
                        ),
                    )
                    onDismiss()
                },
                style = IgngButtonStyle.Primary,
                modifier = Modifier.weight(1.3f),
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            dayKey = picked.format(dayFmt)
                        }
                        showDatePicker = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun FilterOptionPill(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    val colors = IgngTheme.colors
    Row(
        modifier = Modifier
            .clip(IgngTheme.radii.pill)
            .background(if (selected) colors.accentSoft else colors.tileSoft)
            .border(1.dp, if (selected) colors.accent else colors.line, IgngTheme.radii.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.accent else colors.textSecondary,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = label,
            style = IgngTheme.type.caption.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (selected) colors.accent else colors.textSecondary,
        )
    }
}
