package net.igng.mcstatus.feature.explore

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.igng.mcstatus.core.data.ExploreRepository
import net.igng.mcstatus.core.data.HallFakePlayerDto
import net.igng.mcstatus.core.data.HallLandDto
import net.igng.mcstatus.core.data.McCharacterProfileResponse
import net.igng.mcstatus.core.data.MedalProfileDto
import net.igng.mcstatus.core.data.PublicPerfectRosterResponse
import net.igng.mcstatus.core.data.PublicPlayerProfileResponse
import net.igng.mcstatus.core.data.SearchResultItem
import net.igng.mcstatus.core.data.ServerFamilyDto
import net.igng.mcstatus.core.data.TicketDetailDto
import net.igng.mcstatus.core.data.TicketSummaryDto
import net.igng.mcstatus.core.data.VanillaStatsResponse
import net.igng.mcstatus.core.designsystem.IgngAmbientCanvas
import net.igng.mcstatus.core.designsystem.IgngAvatar
import net.igng.mcstatus.core.designsystem.IgngBadge
import net.igng.mcstatus.core.designsystem.IgngBadgeTone
import net.igng.mcstatus.core.designsystem.IgngButton
import net.igng.mcstatus.core.designsystem.IgngButtonStyle
import net.igng.mcstatus.core.designsystem.IgngCopyPill
import net.igng.mcstatus.core.designsystem.IgngDetailSheet
import net.igng.mcstatus.core.designsystem.IgngInteractiveTile
import net.igng.mcstatus.core.designsystem.IgngLoadingBlock
import net.igng.mcstatus.core.designsystem.IgngSearchField
import net.igng.mcstatus.core.designsystem.IgngSectionHeader
import net.igng.mcstatus.core.designsystem.IgngSegmentedTabs
import net.igng.mcstatus.core.designsystem.IgngSkeletonRows
import net.igng.mcstatus.core.designsystem.IgngStateBlock
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.designsystem.IgngTile
import net.igng.mcstatus.core.designsystem.IgngTopBar
import net.igng.mcstatus.core.designsystem.igngStaggeredReveal
import net.igng.mcstatus.core.designsystem.serverFamilyVisual
import net.igng.mcstatus.core.model.IgngFormatters
import net.igng.mcstatus.core.model.ServerThemeKey
import net.igng.mcstatus.core.model.asBooleanFlag

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServerFamilyDetailScreen(
    family: ServerFamilyDto,
    onBack: () -> Unit,
    onOpenMap: (familySlug: String, seasonSlug: String) -> Unit,
) {
    val colors = IgngTheme.colors
    val themeKey = ServerThemeKey.resolve(family.themeKey)
    val visual = serverFamilyVisual(themeKey, colors.isDark)

    IgngAmbientCanvas(accentOverride = visual.accent) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(
                title = family.name,
                subtitle = family.tagline.ifBlank { null },
                onBack = onBack,
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (family.description.isNotBlank()) {
                    item {
                        IgngTile(modifier = Modifier.fillMaxWidth()) {
                            Text(family.description, style = IgngTheme.type.body, color = colors.textSecondary)
                        }
                    }
                }
                item {
                    IgngSectionHeader(title = "周目档案", subtitle = "包含存活周目与历史归档")
                }
                itemsIndexed(family.allKnownSeasons, key = { _, s -> s.slug }) { idx, season ->
                    IgngTile(
                        modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx),
                        borderColor = if (season.primary) visual.borderGlow else colors.line,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = season.label.ifBlank { season.slug },
                                style = IgngTheme.type.sectionTitle,
                                color = colors.textPrimary,
                            )
                            IgngBadge(
                                text = IgngFormatters.seasonStateLabel(season.state),
                                customColor = if (season.state in listOf("live", "persistent")) visual.accent else null,
                            )
                        }
                        if (season.description.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(season.description, style = IgngTheme.type.body, color = colors.textSecondary)
                        }
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            season.version?.takeIf { it.isNotBlank() }?.let { IgngBadge("版本 $it") }
                            season.core?.takeIf { it.isNotBlank() }?.let { IgngBadge("核心 $it") }
                            season.startedAt?.takeIf { it.isNotBlank() }?.let { IgngBadge("开启 $it") }
                            season.endedAt?.takeIf { it.isNotBlank() }?.let { IgngBadge("结束 $it") }
                            season.cpuSpec?.takeIf { it.isNotBlank() }?.let { IgngBadge(it) }
                            season.memorySpec?.takeIf { it.isNotBlank() }?.let { IgngBadge(it) }
                        }
                        season.address?.takeIf { it.isNotBlank() }?.let { addr ->
                            Spacer(Modifier.height(10.dp))
                            IgngCopyPill(value = addr, label = "进服地址：$addr", modifier = Modifier.fillMaxWidth())
                        }
                        season.seed?.takeIf { it.isNotBlank() }?.let { seed ->
                            Spacer(Modifier.height(8.dp))
                            IgngCopyPill(value = seed, label = "地图种子：$seed", modifier = Modifier.fillMaxWidth())
                        }
                        if (season.highlights.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("玩法亮点", style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                            season.highlights.forEach { hl ->
                                Text("· $hl", style = IgngTheme.type.caption, color = colors.textSecondary)
                            }
                        }
                        if (season.rules.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("游玩规则", style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                            season.rules.forEach { rule ->
                                Text("· $rule", style = IgngTheme.type.caption, color = colors.textSecondary)
                            }
                        }
                        if (season.hasMap || season.mapMode != "none" || season.state == "retired") {
                            Spacer(Modifier.height(12.dp))
                            IgngButton(
                                text = if (season.state == "retired") "查看历史快照地图" else "打开实时世界地图",
                                onClick = { onOpenMap(family.slug, season.slug) },
                                style = IgngButtonStyle.Secondary,
                                icon = Icons.Rounded.Map,
                                compact = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalSearchScreen(
    exploreRepository: ExploreRepository,
    onBack: () -> Unit,
    onOpenPlayerPassport: (Int) -> Unit,
    onOpenMcCharacter: (String) -> Unit,
) {
    val colors = IgngTheme.colors
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            results = emptyList()
            error = null
            hasSearched = false
            return@LaunchedEffect
        }
        delay(320L)
        loading = true
        error = null
        runCatching { exploreRepository.search(trimmed) }
            .onSuccess {
                results = it
                hasSearched = true
            }
            .onFailure { error = it.message ?: "搜索服务暂时不可用" }
        loading = false
    }

    IgngAmbientCanvas {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(title = "全站玩家搜索", onBack = onBack)
            IgngSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "输入至少 2 个字符，搜 IGNG 昵称或 MC 角色名",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))
            when {
                loading -> IgngLoadingBlock()
                error != null -> Box(Modifier.padding(16.dp)) {
                    IgngStateBlock(title = "搜索失败", description = error, isError = true)
                }
                !hasSearched -> Box(Modifier.padding(16.dp)) {
                    IgngStateBlock(
                        title = "检索玩家与角色档案",
                        description = "支持查询公开绑定的 IGNG 通行证与独立 Minecraft 角色统计。",
                    )
                }
                results.isEmpty() -> Box(Modifier.padding(16.dp)) {
                    IgngStateBlock(title = "没有找到匹配的玩家", description = "试试更短的关键词或角色原名。")
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(results, key = { i, r -> "${r.kind}:${r.igngId}:${r.mcUsername}:$i" }) { idx, item ->
                        IgngInteractiveTile(
                            onClick = {
                                val targetIgngId = item.igngId
                                if (item.kind == "igng" && targetIgngId != null) {
                                    onOpenPlayerPassport(targetIgngId)
                                } else {
                                    onOpenMcCharacter(item.mcUsername)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                IgngAvatar(
                                    name = item.igngName ?: item.mcUsername,
                                    avatarUrl = null,
                                    size = 40.dp,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = item.igngName ?: item.mcUsername,
                                            style = IgngTheme.type.cardTitle,
                                            color = colors.textPrimary,
                                        )
                                        IgngBadge(
                                            text = if (item.kind == "igng") "IGNG 账户" else "MC 角色",
                                            tone = if (item.kind == "igng") IgngBadgeTone.Accent else IgngBadgeTone.Neutral,
                                        )
                                    }
                                    if (item.hasBoundMc) {
                                        Text("角色：${item.mcUsername}", style = IgngTheme.type.caption, color = colors.textSecondary)
                                    }
                                    item.summary?.let { s ->
                                        Text(
                                            text = "游玩 ${IgngFormatters.formatPlaytimeSeconds(s.playTimeSeconds)} · 挖掘 ${IgngFormatters.formatNumber(s.blocksMined)}",
                                            style = IgngTheme.type.caption,
                                            color = colors.textMuted,
                                        )
                                    }
                                }
                                Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerPassportScreen(
    igngId: Int,
    exploreRepository: ExploreRepository,
    onBack: () -> Unit,
    onOpenMcCharacter: (String) -> Unit,
) {
    val colors = IgngTheme.colors
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<PublicPlayerProfileResponse?>(null) }
    var stats by remember { mutableStateOf<VanillaStatsResponse?>(null) }
    var medals by remember { mutableStateOf<MedalProfileDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(igngId) {
        loading = true
        error = null
        val pDef = scope.async { runCatching { exploreRepository.fetchPlayerProfile(igngId) } }
        val sDef = scope.async { runCatching { exploreRepository.fetchPlayerProfileStats(igngId) } }
        val mDef = scope.async { runCatching { exploreRepository.fetchPlayerProfileMedals(igngId) } }
        pDef.await().onSuccess { profile = it }.onFailure { error = it.message ?: "玩家主页没有读到" }
        sDef.await().onSuccess { stats = it }
        mDef.await().onSuccess { medals = it }
        loading = false
    }

    IgngAmbientCanvas {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(
                title = profile?.user?.displayName ?: "玩家主页",
                subtitle = "账户编号 #$igngId",
                onBack = onBack,
            )
            when {
                loading -> IgngLoadingBlock(fill = true)
                error != null -> Box(Modifier.padding(16.dp)) {
                    IgngStateBlock(title = "无法读取玩家主页", description = error, isError = true)
                }
                profile != null -> {
                    val data = profile!!
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            IgngTile(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    IgngAvatar(
                                        name = data.user?.displayName ?: "#$igngId",
                                        avatarUrl = data.user?.avatar_url,
                                        size = 56.dp,
                                    )
                                    Column {
                                        Text(data.user?.displayName ?: "未知玩家", style = IgngTheme.type.pageTitle, color = colors.textPrimary)
                                        Text("@${data.user?.username.orEmpty()}", style = IgngTheme.type.caption, color = colors.textSecondary)
                                    }
                                }
                            }
                        }

                        if (data.boundCharacters.isNotEmpty()) {
                            item {
                                IgngTile(modifier = Modifier.fillMaxWidth()) {
                                    IgngSectionHeader(title = "公开绑定的 MC 角色")
                                    Spacer(Modifier.height(10.dp))
                                    data.boundCharacters.forEach { mcName ->
                                        IgngInteractiveTile(
                                            onClick = { onOpenMcCharacter(mcName) },
                                            soft = true,
                                            contentPadding = PaddingValues(12.dp),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Icon(Icons.Rounded.Person, null, tint = colors.accent)
                                                    Text(mcName, style = IgngTheme.type.bodyMedium, color = colors.textPrimary)
                                                }
                                                Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        stats?.takeIf { it.available }?.summary?.let { s ->
                            item {
                                IgngTile(modifier = Modifier.fillMaxWidth()) {
                                    IgngSectionHeader(title = "公开游戏统计")
                                    Spacer(Modifier.height(10.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        MiniStat("游玩时长", IgngFormatters.formatPlaytimeSeconds(s.playTimeSeconds), Modifier.weight(1f))
                                        MiniStat("挖掘方块", IgngFormatters.formatNumber(s.blocksMined), Modifier.weight(1f))
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        MiniStat("移动里程", IgngFormatters.formatDistanceCm(s.distanceCm), Modifier.weight(1f))
                                        MiniStat("击败生物", IgngFormatters.formatNumber(s.mobKills), Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        val medalList = medals?.allActiveMedals.orEmpty()
                        if (medalList.isNotEmpty()) {
                            item {
                                IgngTile(modifier = Modifier.fillMaxWidth()) {
                                    IgngSectionHeader(title = "荣誉勋章", subtitle = "共 ${medalList.size} 枚")
                                    Spacer(Modifier.height(8.dp))
                                    medalList.forEach { m ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(m.displayName ?: m.name, style = IgngTheme.type.bodyMedium, color = colors.textPrimary)
                                                m.publicReason?.takeIf { it.isNotBlank() }?.let {
                                                    Text(it, style = IgngTheme.type.caption, color = colors.textSecondary)
                                                }
                                            }
                                            m.account?.let { IgngBadge(it) }
                                        }
                                    }
                                }
                            }
                        }

                        if (data.lands.isNotEmpty()) {
                            item {
                                IgngTile(modifier = Modifier.fillMaxWidth()) {
                                    IgngSectionHeader(title = "公开领地", subtitle = "${data.lands.size} 处")
                                    Spacer(Modifier.height(8.dp))
                                    data.lands.forEach { land ->
                                        Column(Modifier.padding(vertical = 5.dp)) {
                                            Text(land.land_name, style = IgngTheme.type.bodyMedium, color = colors.textPrimary)
                                            land.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                                Text(desc, style = IgngTheme.type.caption, color = colors.textSecondary)
                                            }
                                        }
                                    }
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
fun McCharacterPublicScreen(
    username: String,
    exploreRepository: ExploreRepository,
    onBack: () -> Unit,
    onOpenPassport: (Int) -> Unit,
) {
    val colors = IgngTheme.colors
    var profile by remember { mutableStateOf<McCharacterProfileResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(username) {
        loading = true
        runCatching { exploreRepository.fetchMcCharacter(username) }
            .onSuccess { profile = it }
            .onFailure { error = it.message ?: "角色档案没有读到" }
        loading = false
    }

    IgngAmbientCanvas {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(title = username, subtitle = "Minecraft 角色公开档案", onBack = onBack)
            when {
                loading -> IgngLoadingBlock(fill = true)
                error != null -> Box(Modifier.padding(16.dp)) {
                    IgngStateBlock(title = "读取失败", description = error, isError = true)
                }
                profile != null -> {
                    val data = profile!!
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            IgngTile(modifier = Modifier.fillMaxWidth()) {
                                Text(data.username.ifBlank { username }, style = IgngTheme.type.pageTitle, color = colors.textPrimary)
                                Spacer(Modifier.height(6.dp))
                                Text("最近登录：${IgngFormatters.formatIsoDateTime(data.lastLoginAt)}", style = IgngTheme.type.caption, color = colors.textSecondary)
                                data.binding?.takeIf { it.isPublic && it.igngId != null }?.let { b ->
                                    Spacer(Modifier.height(10.dp))
                                    IgngButton(
                                        text = "查看绑定的 IGNG 玩家主页",
                                        onClick = { onOpenPassport(b.igngId!!) },
                                        style = IgngButtonStyle.Secondary,
                                        compact = true,
                                    )
                                }
                            }
                        }
                        data.stats?.summary?.let { s ->
                            item {
                                IgngTile(modifier = Modifier.fillMaxWidth()) {
                                    IgngSectionHeader(title = "原版统计摘要")
                                    Spacer(Modifier.height(10.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        MiniStat("游玩时长", IgngFormatters.formatPlaytimeSeconds(s.playTimeSeconds), Modifier.weight(1f))
                                        MiniStat("挖掘方块", IgngFormatters.formatNumber(s.blocksMined), Modifier.weight(1f))
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        MiniStat("合成物品", IgngFormatters.formatNumber(s.itemsCrafted), Modifier.weight(1f))
                                        MiniStat("击败生物", IgngFormatters.formatNumber(s.mobKills), Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class HallTab(val label: String) {
    LANDS("公开领地"),
    FAKES("在线假人"),
    TICKETS("公开工单"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunityHallScreen(
    exploreRepository: ExploreRepository,
    onBack: () -> Unit,
) {
    val colors = IgngTheme.colors
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(HallTab.LANDS) }
    var search by remember { mutableStateOf("") }
    var lands by remember { mutableStateOf<List<HallLandDto>>(emptyList()) }
    var fakes by remember { mutableStateOf<List<HallFakePlayerDto>>(emptyList()) }
    var tickets by remember { mutableStateOf<List<TicketSummaryDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedLand by remember { mutableStateOf<HallLandDto?>(null) }
    var selectedTicketDetail by remember { mutableStateOf<TicketDetailDto?>(null) }
    var loadingTicketDetail by remember { mutableStateOf(false) }

    LaunchedEffect(tab) {
        loading = true
        error = null
        runCatching {
            when (tab) {
                HallTab.LANDS -> lands = exploreRepository.fetchPublicLands()
                HallTab.FAKES -> fakes = exploreRepository.fetchPublicFakePlayers()
                HallTab.TICKETS -> tickets = exploreRepository.fetchPublicTickets().resolvedTickets
            }
        }.onFailure { error = it.message ?: "社区大厅数据没有读到" }
        loading = false
    }

    IgngAmbientCanvas {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(title = "社区大厅", subtitle = "公开领地、实时假人与公开工单", onBack = onBack)
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IgngSegmentedTabs(
                    items = HallTab.entries,
                    selected = tab,
                    labelOf = { it.label },
                    onSelect = { tab = it; search = "" },
                )
                IgngSearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "筛选名称、玩家或服务器…",
                )
            }
            Spacer(Modifier.height(10.dp))
            when {
                loading -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(4) }
                error != null -> Box(Modifier.padding(16.dp)) {
                    IgngStateBlock(
                        title = "读取失败",
                        description = error,
                        isError = true,
                        actionLabel = "重试",
                        onAction = {
                            scope.launch {
                                loading = true
                                error = null
                                runCatching {
                                    when (tab) {
                                        HallTab.LANDS -> lands = exploreRepository.fetchPublicLands()
                                        HallTab.FAKES -> fakes = exploreRepository.fetchPublicFakePlayers()
                                        HallTab.TICKETS -> tickets = exploreRepository.fetchPublicTickets().resolvedTickets
                                    }
                                }.onFailure { error = it.message }
                                loading = false
                            }
                        },
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val q = search.trim().lowercase()
                    when (tab) {
                        HallTab.LANDS -> {
                            val filtered = lands.filter {
                                q.isEmpty() || it.name.lowercase().contains(q) || it.ownerName.lowercase().contains(q) || it.serverName.lowercase().contains(q)
                            }
                            if (filtered.isEmpty()) {
                                item { IgngStateBlock(title = "还没有匹配的公开领地") }
                            } else {
                                itemsIndexed(filtered, key = { _, l -> l.id }) { idx, land ->
                                    IgngInteractiveTile(
                                        onClick = { selectedLand = land },
                                        modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx),
                                    ) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(land.name.ifBlank { "未命名领地" }, style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                            IgngBadge(land.serverName)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "领主 ${land.ownerName} · ${IgngFormatters.worldLabel(land.world)} (${IgngFormatters.formatCoordinate(land.center.x, land.center.y, land.center.z)})",
                                            style = IgngTheme.type.caption,
                                            color = colors.textSecondary,
                                        )
                                        if (land.description.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(land.description, style = IgngTheme.type.body, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                        HallTab.FAKES -> {
                            val filtered = fakes.filter {
                                q.isEmpty() || it.name.lowercase().contains(q) || it.ownerName.lowercase().contains(q) || it.serverName.lowercase().contains(q)
                            }
                            if (filtered.isEmpty()) {
                                item { IgngStateBlock(title = "当前没有在线假人") }
                            } else {
                                itemsIndexed(filtered, key = { i, f -> "${f.serverName}:${f.name}:$i" }) { idx, fp ->
                                    IgngTile(modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text(fp.name, style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                                Text("召唤者：${fp.ownerName} · 在线 ${IgngFormatters.formatPlaytimeSeconds(fp.onlineSeconds)}", style = IgngTheme.type.caption, color = colors.textSecondary)
                                            }
                                            IgngBadge(fp.serverName, tone = IgngBadgeTone.Success, leadingDot = true, pulse = true)
                                        }
                                    }
                                }
                            }
                        }
                        HallTab.TICKETS -> {
                            val filtered = tickets.filter {
                                q.isEmpty() || it.title.lowercase().contains(q) || it.creatorName.lowercase().contains(q)
                            }
                            if (filtered.isEmpty()) {
                                item { IgngStateBlock(title = "还没有公开工单") }
                            } else {
                                itemsIndexed(filtered, key = { _, t -> t.id }) { idx, t ->
                                    IgngInteractiveTile(
                                        onClick = {
                                            scope.launch {
                                                loadingTicketDetail = true
                                                selectedTicketDetail = runCatching { exploreRepository.fetchPublicTicketDetail(t.id) }.getOrNull()
                                                loadingTicketDetail = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx),
                                    ) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("#${t.id} ${t.title}", style = IgngTheme.type.cardTitle, color = colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Spacer(Modifier.width(8.dp))
                                            val closed = t.status.equals("closed", ignoreCase = true)
                                            IgngBadge(
                                                text = t.statusLabel.ifBlank { if (closed) "已结单" else "处理中" },
                                                tone = if (closed) IgngBadgeTone.Neutral else IgngBadgeTone.Success,
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text("${t.creatorName} · ${t.typeLabel.ifBlank { t.type }} · ${IgngFormatters.formatRelativeTime(t.updatedAt)}", style = IgngTheme.type.caption, color = colors.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedLand?.let { land ->
        IgngDetailSheet(
            onDismiss = { selectedLand = null },
            title = land.name.ifBlank { "领地详情" },
            subtitle = "${land.serverName} · ${IgngFormatters.worldLabel(land.world)} · 领主 ${land.ownerName}",
        ) {
            val coord = IgngFormatters.formatCoordinate(land.center.x, land.center.y, land.center.z)
            IgngCopyPill(value = coord, label = "中心坐标：$coord", modifier = Modifier.fillMaxWidth())
            if (land.description.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(land.description, style = IgngTheme.type.body, color = colors.textSecondary)
            }
            if (land.playerPermissions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("访客权限", style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    land.playerPermissions.entries.take(16).forEach { (k, v) ->
                        val allowed = v.asBooleanFlag()
                        IgngBadge(k, tone = if (allowed) IgngBadgeTone.Success else IgngBadgeTone.Neutral)
                    }
                }
            }
        }
    }

    if (loadingTicketDetail || selectedTicketDetail != null) {
        IgngDetailSheet(
            onDismiss = { selectedTicketDetail = null; loadingTicketDetail = false },
            title = selectedTicketDetail?.let { "#${it.id} ${it.title}" } ?: "公开工单",
            subtitle = selectedTicketDetail?.let { "${it.creatorName} · ${it.typeLabel}" },
        ) {
            if (loadingTicketDetail) {
                IgngLoadingBlock(compact = true)
            } else {
                selectedTicketDetail?.messages?.forEach { msg ->
                    IgngTile(soft = true, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentPadding = PaddingValues(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(msg.authorName, style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                            Text(IgngFormatters.formatRelativeTime(msg.createdAt), style = IgngTheme.type.badge, color = colors.textMuted)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(msg.content, style = IgngTheme.type.body, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun PerfectRosterScreen(
    exploreRepository: ExploreRepository,
    onBack: () -> Unit,
    onOpenPassport: (Int) -> Unit,
) {
    val colors = IgngTheme.colors
    var roster by remember { mutableStateOf<PublicPerfectRosterResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        runCatching { exploreRepository.fetchPublicPerfectRoster() }
            .onSuccess { roster = it }
            .onFailure { error = it.message ?: "完美之人名录暂时不可用" }
        loading = false
    }

    IgngAmbientCanvas {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(
                title = "完美之人",
                subtitle = roster?.let { "共 ${it.total} 位认证成员" },
                onBack = onBack,
            )
            when {
                loading -> IgngLoadingBlock(fill = true)
                error != null -> Box(Modifier.padding(16.dp)) {
                    IgngStateBlock(title = "读取失败", description = error, isError = true)
                }
                roster != null -> {
                    val data = roster!!
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (data.benefits.isNotEmpty()) {
                            item {
                                IgngTile(modifier = Modifier.fillMaxWidth()) {
                                    IgngSectionHeader(title = "专属权益")
                                    Spacer(Modifier.height(8.dp))
                                    data.benefits.forEach { b ->
                                        Column(Modifier.padding(vertical = 5.dp)) {
                                            Text(b.title, style = IgngTheme.type.bodyMedium, color = colors.textPrimary)
                                            val desc = b.summary.ifBlank { b.description }
                                            if (desc.isNotBlank()) {
                                                Text(desc, style = IgngTheme.type.caption, color = colors.textSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { IgngSectionHeader(title = "公开名录", subtitle = "仅展示公开绑定的成员") }
                        itemsIndexed(data.members, key = { i, m -> "${m.username}:$i" }) { idx, member ->
                            IgngInteractiveTile(
                                onClick = { member.igngId?.let(onOpenPassport) },
                                modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx),
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            member.memberNoLabel?.let { IgngBadge(it, tone = IgngBadgeTone.Warning) }
                                            Text(member.username, style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                        }
                                        val sub = listOfNotNull(member.qualificationLabel, member.since?.let { "自 $it" }).joinToString(" · ")
                                        if (sub.isNotBlank()) {
                                            Spacer(Modifier.height(3.dp))
                                            Text(sub, style = IgngTheme.type.caption, color = colors.textSecondary)
                                        }
                                    }
                                    if (member.igngId != null) {
                                        Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary)
                                    }
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
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = IgngTheme.colors
    IgngTile(soft = true, modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Text(label, style = IgngTheme.type.badge, color = colors.textSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = IgngTheme.type.metricMedium, color = colors.textPrimary)
    }
}
