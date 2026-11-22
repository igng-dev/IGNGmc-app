package net.igng.mcstatus.feature.wiki

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.launch
import net.igng.mcstatus.core.data.SettingsRepository
import net.igng.mcstatus.core.data.WikiCatalogResponse
import net.igng.mcstatus.core.data.WikiPageDetail
import net.igng.mcstatus.core.data.WikiPageSummary
import net.igng.mcstatus.core.data.WikiRepository
import net.igng.mcstatus.core.designsystem.IgngAmbientCanvas
import net.igng.mcstatus.core.designsystem.IgngBadge
import net.igng.mcstatus.core.designsystem.IgngBadgeTone
import net.igng.mcstatus.core.designsystem.IgngButton
import net.igng.mcstatus.core.designsystem.IgngButtonStyle
import net.igng.mcstatus.core.designsystem.IgngCopyPill
import net.igng.mcstatus.core.designsystem.IgngDetailSheet
import net.igng.mcstatus.core.designsystem.IgngInteractiveTile
import net.igng.mcstatus.core.designsystem.IgngLoadingBlock
import net.igng.mcstatus.core.designsystem.IgngPullToRefreshBox
import net.igng.mcstatus.core.designsystem.IgngSearchField
import net.igng.mcstatus.core.designsystem.IgngSectionHeader
import net.igng.mcstatus.core.designsystem.IgngSkeletonRows
import net.igng.mcstatus.core.designsystem.IgngStateBlock
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.designsystem.IgngTile
import net.igng.mcstatus.core.designsystem.IgngTopBar
import net.igng.mcstatus.core.designsystem.igngStaggeredReveal
import net.igng.mcstatus.core.model.IgngFormatters

@Composable
fun WikiCatalogScreen(
    wikiRepository: WikiRepository,
    settingsRepository: SettingsRepository,
    bottomPadding: PaddingValues,
    onOpenArticle: (Int) -> Unit,
) {
    val colors = IgngTheme.colors
    val scope = rememberCoroutineScope()
    val recentIds by settingsRepository.recentWikiIds.collectAsState(initial = emptyList())

    var catalog by remember { mutableStateOf<WikiCatalogResponse?>(null) }
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var usingCache by remember { mutableStateOf(false) }

    suspend fun load(isRefresh: Boolean) {
        if (isRefresh) refreshing = true else loading = true
        error = null
        if (!isRefresh && catalog == null) {
            wikiRepository.readCachedCatalog()?.let { cached ->
                catalog = cached
                usingCache = true
                loading = false
            }
        }
        runCatching { wikiRepository.fetchCatalog() }
            .onSuccess {
                catalog = it
                usingCache = false
            }
            .onFailure {
                if (catalog == null) error = it.message ?: "Wiki 目录没有读到"
            }
        loading = false
        refreshing = false
    }

    LaunchedEffect(Unit) { load(isRefresh = false) }

    val allPages = remember(catalog) {
        val grouped = catalog?.groups.orEmpty().flatMap { g ->
            g.pages.map { it.copy(groupTitle = g.title) }
        }
        (grouped + catalog?.pages.orEmpty()).distinctBy { it.id }
    }
    val recentPages = remember(recentIds, allPages) {
        val byId = allPages.associateBy { it.id }
        recentIds.mapNotNull { byId[it] }
    }

    IgngAmbientCanvas {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(
                title = "IGNGmc 百科",
                subtitle = if (usingCache) "离线缓存已就绪" else "来自 docs.igng.net 官方文档",
            )
            IgngSearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = "搜索教程、指令、领地规则或插件说明…",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))

            IgngPullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { scope.launch { load(isRefresh = true) } },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    loading && catalog == null -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(4) }
                    error != null && catalog == null -> Box(Modifier.padding(16.dp)) {
                        IgngStateBlock(
                            title = "百科目录没有读到",
                            description = error,
                            isError = true,
                            actionLabel = "重试",
                            onAction = { scope.launch { load(isRefresh = false) } },
                        )
                    }
                    else -> {
                        val q = search.trim().lowercase()
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = bottomPadding.calculateBottomPadding() + 20.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (q.isEmpty() && recentPages.isNotEmpty()) {
                                item(key = "recent") {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IgngSectionHeader(title = "最近阅读")
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            recentPages.forEach { page ->
                                                IgngInteractiveTile(
                                                    onClick = { onOpenArticle(page.id) },
                                                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 10.dp),
                                                    modifier = Modifier.width(176.dp),
                                                ) {
                                                    Text(page.title, style = IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(page.groupTitle ?: "文档", style = IgngTheme.type.badge, color = colors.textSecondary, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (q.isNotEmpty()) {
                                val matched = allPages.filter {
                                    it.title.lowercase().contains(q) || (it.summary?.lowercase()?.contains(q) == true)
                                }
                                if (matched.isEmpty()) {
                                    item { IgngStateBlock(title = "没有找到相关词条", description = "换一个关键词试试。") }
                                } else {
                                    itemsIndexed(matched, key = { _, p -> p.id }) { idx, page ->
                                        WikiPageRow(page, onOpenArticle, Modifier.fillMaxWidth().igngStaggeredReveal(idx))
                                    }
                                }
                            } else {
                                val groups = catalog?.groups.orEmpty()
                                if (groups.isNotEmpty()) {
                                    groups.forEach { group ->
                                        item(key = "grp_${group.id}") {
                                            IgngSectionHeader(title = group.title, subtitle = "${group.pages.size} 篇文档")
                                        }
                                        itemsIndexed(group.pages, key = { _, p -> p.id }) { idx, page ->
                                            WikiPageRow(page.copy(groupTitle = group.title), onOpenArticle, Modifier.fillMaxWidth().igngStaggeredReveal(idx))
                                        }
                                    }
                                } else {
                                    itemsIndexed(allPages, key = { _, p -> p.id }) { idx, page ->
                                        WikiPageRow(page, onOpenArticle, Modifier.fillMaxWidth().igngStaggeredReveal(idx))
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
private fun WikiPageRow(
    page: WikiPageSummary,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    IgngInteractiveTile(
        onClick = { onOpen(page.id) },
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Book, null, tint = colors.accent, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(page.title, style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                page.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                    Spacer(Modifier.height(2.dp))
                    Text(summary, style = IgngTheme.type.caption, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary)
        }
    }
}

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullet(val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val lang: String, val code: String) : MdBlock
}

private fun parseSimpleMarkdown(raw: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = raw.replace("\r\n", "\n").split("\n")
    var i = 0
    val para = StringBuilder()

    fun flushPara() {
        val text = para.toString().trim()
        if (text.isNotEmpty()) blocks += MdBlock.Paragraph(text)
        para.setLength(0)
    }

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        when {
            trimmed.startsWith("```") -> {
                flushPara()
                val lang = trimmed.removePrefix("```").trim()
                val codeBuf = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    if (codeBuf.isNotEmpty()) codeBuf.append('\n')
                    codeBuf.append(lines[i])
                    i++
                }
                blocks += MdBlock.Code(lang, codeBuf.toString())
            }
            trimmed.startsWith("#") -> {
                flushPara()
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 4)
                val title = trimmed.drop(level).trim()
                if (title.isNotEmpty()) blocks += MdBlock.Heading(level, title)
            }
            trimmed.startsWith(">") -> {
                flushPara()
                blocks += MdBlock.Quote(trimmed.removePrefix(">").trim())
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flushPara()
                blocks += MdBlock.Bullet(trimmed.drop(2).trim())
            }
            trimmed.isEmpty() -> flushPara()
            else -> {
                if (para.isNotEmpty()) para.append('\n')
                para.append(trimmed)
            }
        }
        i++
    }
    flushPara()
    return blocks
}

@Composable
fun WikiArticleScreen(
    pageId: Int,
    wikiRepository: WikiRepository,
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    val colors = IgngTheme.colors
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var page by remember(pageId) { mutableStateOf<WikiPageDetail?>(null) }
    var loading by remember(pageId) { mutableStateOf(true) }
    var error by remember(pageId) { mutableStateOf<String?>(null) }
    var showToc by remember { mutableStateOf(false) }

    LaunchedEffect(pageId) {
        loading = true
        error = null
        settingsRepository.recordRecentWiki(pageId)
        wikiRepository.readCachedPage(pageId)?.let {
            page = it
            loading = false
        }
        runCatching { wikiRepository.fetchPage(pageId) }
            .onSuccess { page = it }
            .onFailure { if (page == null) error = it.message ?: "文档正文没有读到" }
        loading = false
    }

    val blocks = remember(page?.content) { parseSimpleMarkdown(page?.content.orEmpty()) }
    val headings = remember(blocks) {
        blocks.mapIndexedNotNull { idx, b -> if (b is MdBlock.Heading) idx to b else null }
    }

    IgngAmbientCanvas {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(
                title = page?.title ?: "百科文档",
                subtitle = page?.updatedAt?.let { "更新于 ${IgngFormatters.formatIsoDateTime(it, short = true)}" },
                onBack = onBack,
                actions = {
                    if (headings.isNotEmpty()) {
                        IgngButton(
                            text = "目录",
                            onClick = { showToc = true },
                            style = IgngButtonStyle.Secondary,
                            icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                            compact = true,
                        )
                    }
                },
            )
            when {
                loading && page == null -> IgngLoadingBlock(fill = true)
                error != null && page == null -> Box(Modifier.padding(16.dp)) {
                    IgngStateBlock(title = "读取失败", description = error, isError = true)
                }
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(blocks) { _, block ->
                        when (block) {
                            is MdBlock.Heading -> {
                                Spacer(Modifier.height(if (block.level <= 2) 6.dp else 2.dp))
                                Text(
                                    text = block.text,
                                    style = when (block.level) {
                                        1 -> IgngTheme.type.pageTitle
                                        2 -> IgngTheme.type.sectionTitle
                                        else -> IgngTheme.type.cardTitle
                                    },
                                    color = colors.textPrimary,
                                )
                            }
                            is MdBlock.Paragraph -> {
                                Text(
                                    text = block.text.replace("**", "").replace("`", ""),
                                    style = IgngTheme.type.body,
                                    color = colors.textSecondary,
                                )
                            }
                            is MdBlock.Bullet -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("•", style = IgngTheme.type.body, color = colors.accent)
                                    Text(
                                        text = block.text.replace("**", "").replace("`", ""),
                                        style = IgngTheme.type.body,
                                        color = colors.textSecondary,
                                    )
                                }
                            }
                            is MdBlock.Quote -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(IgngTheme.radii.sm)
                                        .background(colors.accentSoft)
                                        .padding(12.dp),
                                ) {
                                    Text(block.text, style = IgngTheme.type.body, color = colors.textPrimary)
                                }
                            }
                            is MdBlock.Code -> {
                                IgngTile(soft = true, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        IgngBadge(block.lang.ifBlank { "命令 / 代码" }, tone = IgngBadgeTone.Neutral)
                                        IgngCopyPill(value = block.code, label = "复制代码", mono = false)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = block.code,
                                        style = IgngTheme.type.mono,
                                        color = colors.textPrimary,
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showToc) {
        IgngDetailSheet(onDismiss = { showToc = false }, title = "文章目录") {
            headings.forEach { (blockIndex, heading) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(IgngTheme.radii.sm)
                        .clickable {
                            showToc = false
                            scope.launch { listState.animateScrollToItem(blockIndex) }
                        }
                        .padding(
                            start = ((heading.level - 1) * 14).dp,
                            top = 10.dp,
                            bottom = 10.dp,
                            end = 8.dp,
                        ),
                ) {
                    Text(
                        text = heading.text,
                        style = if (heading.level <= 2) IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.Bold) else IgngTheme.type.body,
                        color = colors.textPrimary,
                    )
                }
            }
        }
    }
}
