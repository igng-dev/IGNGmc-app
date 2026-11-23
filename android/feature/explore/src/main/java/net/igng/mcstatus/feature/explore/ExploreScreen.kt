package net.igng.mcstatus.feature.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.igng.mcstatus.core.auth.CustomTabsLauncher
import net.igng.mcstatus.core.data.ExploreRepository
import net.igng.mcstatus.core.data.PublicServerStatusDto
import net.igng.mcstatus.core.data.ServerFamilyDto
import net.igng.mcstatus.core.data.ServerSeasonDto
import net.igng.mcstatus.core.data.StatusRepository
import net.igng.mcstatus.core.designsystem.IgngAmbientCanvas
import net.igng.mcstatus.core.designsystem.IgngAnimatedCounter
import net.igng.mcstatus.core.designsystem.IgngBadge
import net.igng.mcstatus.core.designsystem.IgngBadgeTone
import net.igng.mcstatus.core.designsystem.IgngButton
import net.igng.mcstatus.core.designsystem.IgngButtonStyle
import net.igng.mcstatus.core.designsystem.IgngCopyPill
import net.igng.mcstatus.core.designsystem.IgngHapticKind
import net.igng.mcstatus.core.designsystem.IgngInteractiveTile
import net.igng.mcstatus.core.designsystem.IgngLoadingBlock
import net.igng.mcstatus.core.designsystem.IgngMotion
import net.igng.mcstatus.core.designsystem.IgngPullToRefreshBox
import net.igng.mcstatus.core.designsystem.IgngSectionHeader
import net.igng.mcstatus.core.designsystem.IgngServerBackdropCanvas
import net.igng.mcstatus.core.designsystem.IgngStateBlock
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.designsystem.IgngTile
import net.igng.mcstatus.core.designsystem.igngStaggeredReveal
import net.igng.mcstatus.core.designsystem.rememberIgngHaptics
import net.igng.mcstatus.core.designsystem.serverFamilyVisual
import net.igng.mcstatus.core.model.IgngFormatters
import net.igng.mcstatus.core.model.IgngSocialChannels
import net.igng.mcstatus.core.model.SavedAccount
import net.igng.mcstatus.core.model.ServerThemeKey
import net.igng.mcstatus.core.model.SessionAuthStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    exploreRepository: ExploreRepository,
    statusRepository: StatusRepository,
    activeAccount: SavedAccount?,
    sessionAuthStatus: SessionAuthStatus = if (activeAccount != null) SessionAuthStatus.ACTIVE else SessionAuthStatus.SIGNED_OUT,
    bottomPadding: PaddingValues,
    onOpenSearch: () -> Unit,
    onOpenStatusWall: () -> Unit,
    onOpenMap: (familySlug: String?, seasonSlug: String?) -> Unit,
    onOpenFamilyDetail: (ServerFamilyDto) -> Unit,
    onOpenCommunityHall: () -> Unit,
    onOpenPerfectWall: () -> Unit,
    onOpenMyProfile: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val colors = IgngTheme.colors
    val haptic = rememberIgngHaptics()
    val context = LocalContext.current

    var families by remember { mutableStateOf<List<ServerFamilyDto>>(emptyList()) }
    var statuses by remember { mutableStateOf<List<PublicServerStatusDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedFamilySlug by remember { mutableStateOf<String?>(null) }
    var selectedSeasonSlug by remember { mutableStateOf<String?>(null) }

    suspend fun loadData(isRefresh: Boolean) {
        if (isRefresh) refreshing = true else if (families.isEmpty()) loading = true
        error = null
        try {
            if (!isRefresh && families.isEmpty()) {
                val cached = exploreRepository.readCachedFamilies()
                if (cached.isNotEmpty()) {
                    families = cached
                    loading = false
                }
            }
            val famDef = scope.async { runCatching { exploreRepository.fetchHomeFamilies() } }
            val statusDef = scope.async { runCatching { statusRepository.fetchPublicStatus() } }
            val famRes = famDef.await()
            val statusRes = statusDef.await()
            famRes.onSuccess { list -> if (list.isNotEmpty()) families = list }
            statusRes.onSuccess { statuses = it.servers }
            if (families.isEmpty() && famRes.isFailure) {
                error = famRes.exceptionOrNull()?.message ?: "服务器目录暂时没有读到"
            }
        } finally {
            loading = false
            refreshing = false
        }
    }

    LaunchedEffect(Unit) { loadData(isRefresh = false) }

    val selectedFamily = remember(families, selectedFamilySlug) {
        families.firstOrNull { it.slug == selectedFamilySlug } ?: families.firstOrNull()
    }
    val selectedSeason = remember(selectedFamily, selectedSeasonSlug) {
        val alive = selectedFamily?.aliveSeasons.orEmpty()
        alive.firstOrNull { it.slug == selectedSeasonSlug }
            ?: selectedFamily?.primarySeason
            ?: alive.firstOrNull()
            ?: selectedFamily?.allKnownSeasons?.firstOrNull()
    }
    val activeThemeKey = remember(selectedFamily, selectedSeason) {
        ServerThemeKey.resolve(selectedSeason?.backgroundKey ?: selectedFamily?.themeKey)
    }
    val activeVisual = serverFamilyVisual(activeThemeKey, colors.isDark)

    val statusByServerId = remember(statuses) { statuses.associateBy { it.id } }
    val selectedServerStatus = selectedSeason?.serverId?.let { statusByServerId[it] }
    val totalOnline = remember(statuses) { statuses.sumOf { it.onlinePlayers ?: 0 } }
    val onlineServers = remember(statuses) { statuses.count { it.online } }

    IgngAmbientCanvas(accentOverride = activeVisual.accent) {
        IgngPullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { loadData(isRefresh = true) } },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = bottomPadding.calculateBottomPadding() + 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Brand header + quick search + account entry
                item(key = "header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "IGNG Minecraft",
                                style = IgngTheme.type.heroTitle,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = if (statuses.isNotEmpty()) {
                                    "$onlineServers/${statuses.size} 台存活周目在线 · $totalOnline 名玩家在线"
                                } else {
                                    "官方服务器与社区平台"
                                },
                                style = IgngTheme.type.caption,
                                color = colors.textSecondary,
                            )
                        }
                        val needsReauth = activeAccount != null && sessionAuthStatus.requiresReauth
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (needsReauth) colors.warningBg else colors.tile)
                                .border(1.dp, if (needsReauth) colors.warning else colors.lineStrong, CircleShape)
                                .clickable {
                                    haptic(IgngHapticKind.Tap)
                                    onOpenMyProfile()
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = when {
                                    needsReauth -> "需重新授权"
                                    activeAccount != null -> activeAccount.displayName
                                    else -> "登录"
                                },
                                style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    needsReauth -> colors.warning
                                    activeAccount != null -> colors.textPrimary
                                    else -> colors.accent
                                },
                                maxLines = 1,
                            )
                        }
                    }
                }

                // 2. Global search entry pill
                item(key = "searchBar") {
                    IgngInteractiveTile(
                        onClick = onOpenSearch,
                        shape = IgngTheme.radii.md,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "搜索玩家、MC 角色名或公开档案…",
                                style = IgngTheme.type.body,
                                color = colors.textMuted,
                            )
                        }
                    }
                }

                // 3. Server Family Switcher + Immersive Hero Showcase
                item(key = "hero") {
                    when {
                        loading && families.isEmpty() -> IgngLoadingBlock()
                        error != null && families.isEmpty() -> IgngStateBlock(
                            title = "服务器目录暂时不可用",
                            description = error,
                            isError = true,
                            actionLabel = "重试",
                            onAction = { scope.launch { loadData(isRefresh = false) } },
                        )
                        selectedFamily != null -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Family horizontal pills
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    families.forEach { family ->
                                        val isSelected = family.slug == selectedFamily.slug
                                        val key = ServerThemeKey.resolve(family.themeKey)
                                        val visual = serverFamilyVisual(key, colors.isDark)
                                        val pillBg by animateColorAsState(
                                            targetValue = if (isSelected) visual.softTint else colors.tile,
                                            animationSpec = IgngMotion.microTween(IgngTheme.motionReduced),
                                            label = "famPillBg",
                                        )
                                        val pillBorder by animateColorAsState(
                                            targetValue = if (isSelected) visual.accent else colors.line,
                                            animationSpec = IgngMotion.microTween(IgngTheme.motionReduced),
                                            label = "famPillBorder",
                                        )
                                        Row(
                                            modifier = Modifier
                                                .clip(IgngTheme.radii.pill)
                                                .background(pillBg)
                                                .border(1.dp, pillBorder, IgngTheme.radii.pill)
                                                .clickable {
                                                    haptic(IgngHapticKind.Tick)
                                                    selectedFamilySlug = family.slug
                                                    selectedSeasonSlug = null
                                                }
                                                .padding(horizontal = 13.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(visual.accent),
                                            )
                                            Text(
                                                text = family.name,
                                                style = IgngTheme.type.caption.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                ),
                                                color = if (isSelected) visual.accent else colors.textPrimary,
                                            )
                                        }
                                    }
                                }

                                // Immersive Family Hero Card
                                ServerFamilyHeroCard(
                                    family = selectedFamily,
                                    season = selectedSeason,
                                    status = selectedServerStatus,
                                    themeKey = activeThemeKey,
                                    onSelectSeason = { selectedSeasonSlug = it.slug },
                                    onOpenMap = { onOpenMap(selectedFamily.slug, selectedSeason?.slug) },
                                    onOpenFamilyDetail = { onOpenFamilyDetail(selectedFamily) },
                                )
                            }
                        }
                    }
                }

                // 4. Live Status Summary Banner
                item(key = "statusStrip") {
                    IgngInteractiveTile(
                        onClick = onOpenStatusWall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .igngStaggeredReveal(1),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(IgngTheme.radii.sm)
                                        .background(colors.successBg),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Timeline,
                                        contentDescription = null,
                                        tint = colors.success,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Column {
                                    Text("服务器运行状态墙", style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                    Text(
                                        text = if (statuses.isNotEmpty()) {
                                            "实时 TPS、延迟趋势曲线与 90 格健康记录"
                                        } else {
                                            "查看全服运行状态与历史波动"
                                        },
                                        style = IgngTheme.type.caption,
                                        color = colors.textSecondary,
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (statuses.isNotEmpty()) {
                                    IgngAnimatedCounter(
                                        text = "$totalOnline 人",
                                        style = IgngTheme.type.metricMedium,
                                        color = colors.accent,
                                    )
                                }
                                Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary)
                            }
                        }
                    }
                }

                // 5. Bento Quick Discovery Grid
                item(key = "bentoGrid") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        IgngSectionHeader(title = "发现与社区")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ExploreBentoTile(
                                title = "世界地图",
                                subtitle = "实时在线与历史快照",
                                icon = Icons.Rounded.Map,
                                tint = Color(0xFF06B6D4),
                                onClick = { onOpenMap(selectedFamily?.slug, selectedSeason?.slug) },
                                modifier = Modifier.weight(1f).igngStaggeredReveal(2),
                            )
                            ExploreBentoTile(
                                title = "社区大厅",
                                subtitle = "领地、假人与公开工单",
                                icon = Icons.Rounded.Groups,
                                tint = Color(0xFF10B981),
                                onClick = onOpenCommunityHall,
                                modifier = Modifier.weight(1f).igngStaggeredReveal(3),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ExploreBentoTile(
                                title = "完美之人",
                                subtitle = "荣誉成员名录与权益",
                                icon = Icons.Rounded.WorkspacePremium,
                                tint = Color(0xFFF59E0B),
                                onClick = onOpenPerfectWall,
                                modifier = Modifier.weight(1f).igngStaggeredReveal(4),
                            )
                            ExploreBentoTile(
                                title = "全服目录",
                                subtitle = "${families.size} 个服务器家族档案",
                                icon = Icons.Rounded.Dns,
                                tint = colors.accent,
                                onClick = { selectedFamily?.let(onOpenFamilyDetail) ?: onOpenStatusWall() },
                                modifier = Modifier.weight(1f).igngStaggeredReveal(5),
                            )
                        }
                    }
                }

                // 6. Official QQ Groups & Community Channels (migrated from Chat tab)
                item(key = "socialChannels") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        IgngSectionHeader(
                            title = "官方交流群与社区",
                            subtitle = "点击群号一键复制，或前往官方社区平台",
                        )

                        // Merged compact QQ groups card
                        IgngTile(
                            modifier = Modifier
                                .fillMaxWidth()
                                .igngStaggeredReveal(6),
                            contentPadding = PaddingValues(14.dp),
                        ) {
                            IgngSocialChannels.qqGroups.forEachIndexed { idx, group ->
                                if (idx > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        thickness = 1.dp,
                                        color = colors.line,
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = group.label,
                                        style = IgngTheme.type.cardTitle,
                                        color = colors.textPrimary,
                                    )
                                    IgngBadge(group.badge, tone = IgngBadgeTone.Accent)
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = group.summary,
                                    style = IgngTheme.type.caption,
                                    color = colors.textSecondary,
                                )
                                if (group.highlights.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
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
                                    Spacer(Modifier.height(10.dp))
                                    IgngCopyPill(
                                        value = num,
                                        label = "群号 $num",
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }

                        // Compact external community platforms row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            IgngSocialChannels.externalChannels.forEachIndexed { idx, ch ->
                                val (icon, tint, shortTitle) = when (ch.key) {
                                    "mc-discord" -> Triple(Icons.Rounded.HeadsetMic, Color(0xFF5865F2), "Discord")
                                    "bilibili-official" -> Triple(Icons.Rounded.PlayCircle, Color(0xFFFB7299), "B站官方")
                                    else -> Triple(Icons.Rounded.Forum, colors.accent, "BBS 论坛")
                                }
                                val shortDesc = ch.highlights.take(2).joinToString(" · ")
                                    .ifBlank { ch.badge }
                                IgngInteractiveTile(
                                    onClick = {
                                        ch.url?.let { url ->
                                            CustomTabsLauncher.openExternalUrl(context, url, colors.isDark)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .igngStaggeredReveal(7 + idx),
                                    contentPadding = PaddingValues(12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(IgngTheme.radii.sm)
                                                .background(tint.copy(alpha = if (colors.isDark) 0.2f else 0.12f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
                                        }
                                        Icon(
                                            imageVector = Icons.Rounded.OpenInNew,
                                            contentDescription = "外部打开",
                                            tint = colors.textMuted,
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = shortTitle,
                                        style = IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = shortDesc,
                                        style = IgngTheme.type.badge,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
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

@Composable
private fun ServerFamilyHeroCard(
    family: ServerFamilyDto,
    season: ServerSeasonDto?,
    status: PublicServerStatusDto?,
    themeKey: ServerThemeKey,
    onSelectSeason: (ServerSeasonDto) -> Unit,
    onOpenMap: () -> Unit,
    onOpenFamilyDetail: () -> Unit,
) {
    val colors = IgngTheme.colors
    val motionReduced = IgngTheme.motionReduced
    val visual = serverFamilyVisual(themeKey, colors.isDark)
    val haptic = rememberIgngHaptics()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(IgngTheme.radii.hero)
            .background(colors.tile)
            .border(1.dp, visual.borderGlow, IgngTheme.radii.hero),
    ) {
        IgngServerBackdropCanvas(themeKey = themeKey, modifier = Modifier.matchParentSize())

        AnimatedContent(
            targetState = "${family.slug}:${season?.slug}",
            transitionSpec = {
                fadeIn(IgngMotion.normalTween(motionReduced)) togetherWith
                    fadeOut(IgngMotion.microTween(motionReduced))
            },
            label = "heroSwap",
        ) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IgngBadge(
                        text = season?.label?.ifBlank { themeKey.badge } ?: themeKey.badge,
                        customColor = visual.accent,
                        leadingDot = true,
                        pulse = status?.online == true,
                    )
                    val statusText = when {
                        status == null -> IgngFormatters.seasonStateLabel(season?.state)
                        status.online -> "在线 · ${IgngFormatters.formatDecimal(status.pingMs, 0, " ms")}"
                        status.status == "degraded" -> "波动"
                        status.status == "offline" -> "离线"
                        else -> "状态未知"
                    }
                    IgngBadge(
                        text = statusText,
                        tone = when {
                            status?.online == true -> IgngBadgeTone.Success
                            status?.status == "degraded" -> IgngBadgeTone.Warning
                            status?.status == "offline" -> IgngBadgeTone.Danger
                            else -> IgngBadgeTone.Neutral
                        },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = buildString {
                            append(family.name)
                            if (!season?.label.isNullOrBlank()) {
                                append(" · ")
                                append(season?.label)
                            }
                        },
                        style = IgngTheme.type.pageTitle,
                        color = colors.textPrimary,
                    )
                    val desc = season?.cardDescription?.ifBlank { season.description }
                        ?.ifBlank { family.tagline }
                        ?.ifBlank { family.description }
                        .orEmpty()
                    if (desc.isNotBlank()) {
                        Text(
                            text = desc,
                            style = IgngTheme.type.body,
                            color = colors.textSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Multi-season tabs when family has > 1 alive season
                if (family.aliveSeasons.size > 1) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        family.aliveSeasons.forEach { s ->
                            val picked = s.slug == season?.slug
                            Box(
                                modifier = Modifier
                                    .clip(IgngTheme.radii.sm)
                                    .background(if (picked) visual.accent else colors.tileSoft)
                                    .border(1.dp, if (picked) visual.accent else colors.line, IgngTheme.radii.sm)
                                    .clickable {
                                        haptic(IgngHapticKind.Tick)
                                        onSelectSeason(s)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    text = s.label.ifBlank { s.slug },
                                    style = IgngTheme.type.badge,
                                    color = if (picked) Color.White else colors.textSecondary,
                                )
                            }
                        }
                    }
                }

                // Live metrics strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HeroMetricCell(
                        label = "在线玩家",
                        value = IgngFormatters.formatNumber(status?.onlinePlayers),
                        modifier = Modifier.weight(1f),
                    )
                    HeroMetricCell(
                        label = "TPS",
                        value = IgngFormatters.formatDecimal(status?.tps, 1),
                        modifier = Modifier.weight(1f),
                    )
                    HeroMetricCell(
                        label = "版本",
                        value = season?.version?.ifBlank { "—" } ?: "—",
                        modifier = Modifier.weight(1.2f),
                    )
                }

                // Address copy pill
                val address = season?.address ?: status?.address
                if (!address.isNullOrBlank()) {
                    IgngCopyPill(
                        value = address,
                        label = address,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Primary & secondary actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IgngButton(
                        text = "查看周目档案",
                        onClick = onOpenFamilyDetail,
                        style = IgngButtonStyle.Primary,
                        icon = Icons.Rounded.Explore,
                        modifier = Modifier.weight(1f),
                    )
                    if (season?.hasViewableMap == true) {
                        IgngButton(
                            text = "打开地图",
                            onClick = onOpenMap,
                            style = IgngButtonStyle.Secondary,
                            icon = Icons.Rounded.Map,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    Column(
        modifier = modifier
            .clip(IgngTheme.radii.sm)
            .background(colors.tileSoft.copy(alpha = 0.85f))
            .border(1.dp, colors.line, IgngTheme.radii.sm)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = IgngTheme.type.badge, color = colors.textSecondary)
        IgngAnimatedCounter(
            text = value,
            style = IgngTheme.type.metricMedium,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun ExploreBentoTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    IgngInteractiveTile(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(IgngTheme.radii.sm)
                .background(tint.copy(alpha = if (colors.isDark) 0.18f else 0.11f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = IgngTheme.type.cardTitle, color = colors.textPrimary)
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = IgngTheme.type.caption,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
