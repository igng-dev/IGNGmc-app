package net.igng.mcstatus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.igng.mcstatus.core.data.IgngAppContainer
import net.igng.mcstatus.core.designsystem.IgngAmbientCanvas
import net.igng.mcstatus.core.designsystem.IgngAvatar
import net.igng.mcstatus.core.designsystem.IgngBadge
import net.igng.mcstatus.core.designsystem.IgngBadgeTone
import net.igng.mcstatus.core.designsystem.IgngButton
import net.igng.mcstatus.core.designsystem.IgngButtonStyle
import net.igng.mcstatus.core.designsystem.IgngConfirmDialog
import net.igng.mcstatus.core.designsystem.IgngInteractiveTile
import net.igng.mcstatus.core.designsystem.IgngSectionHeader
import net.igng.mcstatus.core.designsystem.IgngSegmentedTabs
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.designsystem.IgngTile
import net.igng.mcstatus.core.designsystem.IgngTopBar
import net.igng.mcstatus.core.model.AppPreferences
import net.igng.mcstatus.core.model.IgngFormatters
import net.igng.mcstatus.core.model.MotionIntensity
import net.igng.mcstatus.core.model.SavedAccount
import net.igng.mcstatus.core.model.ThemeMode

@Composable
fun SettingsScreen(
    container: IgngAppContainer,
    preferences: AppPreferences,
    activeAccount: SavedAccount?,
    accounts: List<SavedAccount>,
    onBack: () -> Unit,
    onStartOAuthLogin: (forceLogin: Boolean) -> Unit,
    onSwitchAccount: (Int) -> Unit,
    onLogoutActive: () -> Unit,
) {
    val colors = IgngTheme.colors
    val scope = rememberCoroutineScope()
    var cacheBytes by remember { mutableLongStateOf(0L) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cacheBytes = container.cacheSizeBytes()
    }

    IgngAmbientCanvas {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(title = "设置", subtitle = "账户安全、外观画布与动效偏好", onBack = onBack)
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Account & Multi-Account Vault
                item(key = "accounts") {
                    IgngTile(modifier = Modifier.fillMaxWidth()) {
                        IgngSectionHeader(
                            title = "IGNG 账户",
                            subtitle = "令牌由 Android Keystore AES-256-GCM 硬件加密保护",
                        )
                        Spacer(Modifier.height(12.dp))
                        if (activeAccount == null) {
                            IgngButton(
                                text = "通过 IGNG SSO 登录",
                                onClick = { onStartOAuthLogin(false) },
                                style = IgngButtonStyle.Primary,
                                icon = Icons.AutoMirrored.Rounded.Login,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                IgngAvatar(activeAccount.displayName, activeAccount.avatarUrl, size = 46.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(activeAccount.displayName, style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                    Text(
                                        text = buildString {
                                            append("@${activeAccount.username}")
                                            activeAccount.expiresAt?.let {
                                                append(" · 有效期至 ")
                                                append(IgngFormatters.formatIsoDateTime(it, short = true))
                                            }
                                        },
                                        style = IgngTheme.type.caption,
                                        color = colors.textSecondary,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IgngButton(
                                    text = "添加其他账户",
                                    onClick = { onStartOAuthLogin(true) },
                                    style = IgngButtonStyle.Secondary,
                                    icon = Icons.Rounded.PersonAdd,
                                    compact = true,
                                    modifier = Modifier.weight(1f),
                                )
                                IgngButton(
                                    text = "退出当前账户",
                                    onClick = { showLogoutConfirm = true },
                                    style = IgngButtonStyle.Danger,
                                    icon = Icons.AutoMirrored.Rounded.Logout,
                                    compact = true,
                                )
                            }

                            if (accounts.size > 1) {
                                Spacer(Modifier.height(12.dp))
                                Text("已保存的账户（点击切换）", style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = colors.textSecondary)
                                Spacer(Modifier.height(6.dp))
                                accounts.forEach { acc ->
                                    val isCurrent = acc.userId == activeAccount.userId
                                    IgngInteractiveTile(
                                        onClick = { if (!isCurrent) onSwitchAccount(acc.userId) },
                                        soft = true,
                                        accentBorder = if (isCurrent) colors.accent else null,
                                        contentPadding = PaddingValues(11.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    ) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text(acc.displayName, style = IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                                                Text("#${acc.userId} · @${acc.username}", style = IgngTheme.type.badge, color = colors.textSecondary)
                                            }
                                            if (isCurrent) IgngBadge("当前使用", tone = IgngBadgeTone.Accent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Appearance & Motion
                item(key = "appearance") {
                    IgngTile(modifier = Modifier.fillMaxWidth()) {
                        IgngSectionHeader(title = "IGNG 画布与动效")
                        Spacer(Modifier.height(12.dp))
                        Text("界面主题", style = IgngTheme.type.caption.copy(fontWeight = FontWeight.SemiBold), color = colors.textSecondary)
                        Spacer(Modifier.height(6.dp))
                        IgngSegmentedTabs(
                            items = ThemeMode.entries,
                            selected = preferences.themeMode,
                            labelOf = { it.label },
                            onSelect = { mode -> scope.launch { container.settingsRepository.setThemeMode(mode) } },
                        )

                        Spacer(Modifier.height(14.dp))
                        Text("动效强度", style = IgngTheme.type.caption.copy(fontWeight = FontWeight.SemiBold), color = colors.textSecondary)
                        Spacer(Modifier.height(6.dp))
                        IgngSegmentedTabs(
                            items = MotionIntensity.entries,
                            selected = preferences.motionIntensity,
                            labelOf = { it.label },
                            onSelect = { level -> scope.launch { container.settingsRepository.setMotionIntensity(level) } },
                        )

                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("触感反馈", style = IgngTheme.type.bodyMedium, color = colors.textPrimary)
                                Text("图表滑动、下拉折角刷新与复制反馈微震动", style = IgngTheme.type.caption, color = colors.textSecondary)
                            }
                            Switch(
                                checked = preferences.hapticsEnabled,
                                onCheckedChange = { enabled -> scope.launch { container.settingsRepository.setHapticsEnabled(enabled) } },
                            )
                        }
                    }
                }

                // 3. Cache & About
                item(key = "cache") {
                    IgngTile(modifier = Modifier.fillMaxWidth()) {
                        IgngSectionHeader(
                            title = "本地离线缓存",
                            subtitle = "Wiki 离线文章与服务器目录快照：${IgngFormatters.formatBytes(cacheBytes)}",
                        )
                        Spacer(Modifier.height(10.dp))
                        IgngButton(
                            text = "清理离线缓存",
                            onClick = {
                                scope.launch {
                                    container.clearAllCaches()
                                    cacheBytes = container.cacheSizeBytes()
                                }
                            },
                            style = IgngButtonStyle.Secondary,
                            icon = Icons.Rounded.DeleteOutline,
                            compact = true,
                        )
                    }
                }

                item(key = "about") {
                    IgngTile(modifier = Modifier.fillMaxWidth()) {
                        Text("IGNGmc", style = IgngTheme.type.sectionTitle, color = colors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Kotlin + Jetpack Compose 原生构建",
                            style = IgngTheme.type.caption,
                            color = colors.textSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "服务拓扑：mc.igng.net · dash.igng.net · sso.igng.net · chat.igng.net · docs.igng.net",
                            style = IgngTheme.type.mono,
                            color = colors.textMuted,
                        )
                    }
                }
            }
        }
    }

    if (showLogoutConfirm && activeAccount != null) {
        IgngConfirmDialog(
            title = "退出账户 ${activeAccount.displayName}",
            message = "将吊销本机当前会话令牌并从本地安全存储移除；不会影响你的游戏数据或其他设备的登录状态。",
            confirmLabel = "退出登录",
            danger = true,
            onDismiss = { showLogoutConfirm = false },
            onConfirm = {
                showLogoutConfirm = false
                onLogoutActive()
            },
        )
    }
}
