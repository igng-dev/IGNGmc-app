package net.igng.mcstatus.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.igng.mcstatus.core.auth.CustomTabsLauncher
import net.igng.mcstatus.core.designsystem.IgngAmbientCanvas
import net.igng.mcstatus.core.designsystem.IgngBadge
import net.igng.mcstatus.core.designsystem.IgngBadgeTone
import net.igng.mcstatus.core.designsystem.IgngCopyPill
import net.igng.mcstatus.core.designsystem.IgngInteractiveTile
import net.igng.mcstatus.core.designsystem.IgngSectionHeader
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.designsystem.IgngTile
import net.igng.mcstatus.core.designsystem.IgngTopBar
import net.igng.mcstatus.core.designsystem.igngStaggeredReveal
import net.igng.mcstatus.core.model.IgngSocialChannels

/**
 * Dedicated screen presenting official IGNGmc QQ groups (with one-tap copy)
 * and external community platforms (Discord, Bilibili, BBS forum).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunityChannelsScreen(
    onBack: () -> Unit,
) {
    val colors = IgngTheme.colors
    val semantic = chatSemanticColors(colors.isDark)
    val context = LocalContext.current

    IgngAmbientCanvas {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            IgngTopBar(
                title = "官方交流群与社区",
                subtitle = "QQ 交流群与官方社区平台",
                onBack = onBack,
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "qqHeader") {
                    IgngSectionHeader(
                        title = "官方 QQ 交流群",
                        subtitle = "点击群号一键复制，在 QQ 中搜索加入",
                    )
                }

                itemsIndexed(IgngSocialChannels.qqGroups, key = { _, g -> g.key }) { idx, group ->
                    IgngTile(
                        modifier = Modifier
                            .fillMaxWidth()
                            .igngStaggeredReveal(idx),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(IgngTheme.radii.sm)
                                    .background(semantic.qqSoft),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Groups,
                                    contentDescription = null,
                                    tint = semantic.qq,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                            Text(
                                text = group.label,
                                style = IgngTheme.type.cardTitle,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IgngBadge(group.badge, customColor = semantic.qq)
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = group.summary,
                            style = IgngTheme.type.body,
                            color = colors.textSecondary,
                        )

                        if (group.highlights.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                group.highlights.forEach { tag ->
                                    IgngBadge(tag, tone = IgngBadgeTone.Neutral)
                                }
                            }
                        }

                        group.copyValue?.let { num ->
                            Spacer(Modifier.height(12.dp))
                            IgngCopyPill(
                                value = num,
                                label = "群号 $num",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                item(key = "platformsSpacer") {
                    Spacer(Modifier.height(4.dp))
                }

                item(key = "platformsHeader") {
                    IgngSectionHeader(
                        title = "官方社区平台",
                        subtitle = "点击卡片前往对应官方渠道",
                    )
                }

                itemsIndexed(IgngSocialChannels.externalChannels, key = { _, ch -> ch.key }) { idx, ch ->
                    val (icon, tint) = when (ch.key) {
                        "mc-discord" -> Icons.Rounded.HeadsetMic to Color(0xFF5865F2)
                        "bilibili-official" -> Icons.Rounded.PlayCircle to Color(0xFFFB7299)
                        else -> Icons.Rounded.Forum to colors.accent
                    }
                    val displayUrl = ch.url
                        ?.removePrefix("https://")
                        ?.removePrefix("http://")
                        ?.trimEnd('/')

                    IgngInteractiveTile(
                        onClick = {
                            ch.url?.let { url ->
                                CustomTabsLauncher.openExternalUrl(context, url, colors.isDark)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .igngStaggeredReveal(IgngSocialChannels.qqGroups.size + idx),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(IgngTheme.radii.sm)
                                    .background(tint.copy(alpha = if (colors.isDark) 0.20f else 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = ch.label,
                                        style = IgngTheme.type.cardTitle,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    IgngBadge(ch.badge, customColor = tint)
                                }
                                if (!displayUrl.isNullOrBlank()) {
                                    Text(
                                        text = displayUrl,
                                        style = IgngTheme.type.badge,
                                        color = colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = "外部打开",
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = ch.summary,
                            style = IgngTheme.type.body,
                            color = colors.textSecondary,
                        )

                        if (ch.highlights.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                ch.highlights.forEach { tag ->
                                    IgngBadge(tag, tone = IgngBadgeTone.Neutral)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
