@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.igng.mcstatus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import android.content.ClipData
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import net.igng.mcstatus.data.WikiCatalogNode
import net.igng.mcstatus.data.WikiPage
import net.igng.mcstatus.data.WikiPageSummary
import net.igng.mcstatus.data.toDisplayTime

@Composable
fun TutorialScreen(
    uiState: TutorialUiState,
    onQueryChanged: (String) -> Unit,
    onToggleNode: (Int) -> Unit,
    onRefresh: () -> Unit,
    onOpenPage: (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("教程")
                        Text(
                            text = "IGNGmc 知识库",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null)
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !uiState.isRefreshing) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新教程目录")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "tutorial-intro") {
                TutorialIntroCard(uiState, onOpenPage)
            }

            item(key = "tutorial-search") {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("搜索教程标题或目录") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "清除搜索")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search,
                    ),
                )
            }

            if (uiState.isLoading && uiState.catalog == null) {
                item(key = "tutorial-loading") { TutorialLoadingState() }
            } else if (uiState.catalog == null) {
                item(key = "tutorial-error") {
                    TutorialErrorState(
                        message = uiState.errorMessage ?: "教程目录暂时不可用",
                        onRetry = onRefresh,
                    )
                }
            } else if (uiState.query.isNotBlank()) {
                item(key = "tutorial-search-heading") {
                    TutorialSectionHeading("搜索结果", "${uiState.visiblePages.size} 篇文章")
                }
                if (uiState.visiblePages.isEmpty()) {
                    item(key = "tutorial-search-empty") {
                        TutorialEmptyState("没有找到匹配的教程")
                    }
                } else {
                    items(uiState.visiblePages, key = { "search-page-${it.id}" }) { page ->
                        TutorialPageRow(page = page, onClick = { onOpenPage(page.id) })
                    }
                }
            } else {
                item(key = "tutorial-recent-heading") {
                    TutorialSectionHeading("最近更新", "站点内容实时同步")
                }
                items(
                    uiState.allPages
                        .sortedByDescending { it.updatedAt.orEmpty() }
                        .take(3),
                    key = { "recent-page-${it.id}" },
                ) { page ->
                    TutorialPageRow(page = page, onClick = { onOpenPage(page.id) })
                }

                item(key = "tutorial-catalog-heading") {
                    TutorialSectionHeading("教程目录", "按主题浏览")
                }
                items(uiState.nodes, key = { "catalog-node-${it.id}" }) { node ->
                    TutorialNodeView(
                        node = node,
                        expandedNodeIds = uiState.expandedNodeIds,
                        onToggleNode = onToggleNode,
                        onOpenPage = onOpenPage,
                    )
                }
            }

            if (uiState.errorMessage != null && uiState.catalog != null) {
                item(key = "tutorial-inline-error") {
                    TutorialSyncWarning(
                        message = uiState.errorMessage,
                        onRetry = onRefresh,
                    )
                }
            }
        }
    }
}

@Composable
fun TutorialArticleScreen(
    uiState: WikiArticleUiState,
    neighbors: TutorialPageNeighbors,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenPage: (Int) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val context = LocalContext.current
    val page = uiState.page
    val blocks = remember(page?.id, page?.currentVersion, page?.content) {
        page?.let {
            if (it.format.equals("markdown", ignoreCase = true)) {
                parseWikiMarkdown(it.content)
            } else {
                listOf(WikiBlock.Paragraph(it.content))
            }
        }.orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = page?.title ?: "教程文章",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回教程目录")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !uiState.isRefreshing) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新文章")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (uiState.isRefreshing && page != null) {
                item(key = "article-progress") {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            if (page == null && uiState.isLoading) {
                item(key = "article-loading") { TutorialLoadingState() }
            } else if (page == null) {
                item(key = "article-error") {
                    TutorialErrorState(
                        message = uiState.errorMessage ?: "教程文章暂时不可用",
                        onRetry = onRefresh,
                    )
                }
            } else {
                item(key = "article-header") {
                    TutorialArticleHeader(page, uiState.fromCache)
                }
                if (blocks.isEmpty()) {
                    item(key = "article-empty") {
                        TutorialEmptyState("这篇文章暂时没有可显示的正文")
                    }
                } else {
                    itemsIndexedStable(blocks) { index, block ->
                        WikiBlockView(
                            block = block,
                            onLinkClick = onOpenLink,
                            onCopyCode = {
                                val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                manager?.setPrimaryClip(ClipData.newPlainText("IGNG 教程代码", it))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (uiState.errorMessage != null) {
                    item(key = "article-sync-warning") {
                        TutorialSyncWarning(uiState.errorMessage, onRefresh)
                    }
                }
                item(key = "article-neighbors") {
                    TutorialNeighborRow(neighbors, onOpenPage)
                }
            }
        }
    }
}

@Composable
private fun TutorialIntroCard(
    uiState: TutorialUiState,
    onOpenPage: (Int) -> Unit,
) {
    val rootPage = uiState.rootPage
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(42.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(9.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.catalog?.knowledgeBase?.name ?: "IGNGmc",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${uiState.allPages.size} 篇文章 · ${syncLabel(uiState)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
            Text(
                text = uiState.catalog?.knowledgeBase?.description ?: "服务器规则、功能说明与插件使用教程。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (rootPage != null) {
                Button(onClick = { onOpenPage(rootPage.id) }) {
                    Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, modifier = Modifier.size(17.dp))
                    Text("阅读知识库首页", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun TutorialNodeView(
    node: WikiCatalogNode,
    expandedNodeIds: Set<Int>,
    onToggleNode: (Int) -> Unit,
    onOpenPage: (Int) -> Unit,
    depth: Int = 0,
) {
    val hasChildren = node.children.isNotEmpty()
    val hasPage = node.page != null
    val expanded = node.id in expandedNodeIds

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 10).dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (hasChildren || hasPage) onToggleNode(node.id)
                    }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = if (hasChildren) Icons.Rounded.Folder else Icons.AutoMirrored.Rounded.Article,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = node.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (hasChildren || hasPage) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开",
                    )
                }
            }
            if (expanded) {
                node.page?.let { page ->
                    TutorialPageRow(
                        page = page,
                        onClick = { onOpenPage(page.id) },
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    )
                }
                node.children.forEach { child ->
                    TutorialNodeView(child, expandedNodeIds, onToggleNode, onOpenPage, depth + 1)
                }
            }
        }
    }
}

@Composable
private fun TutorialPageRow(
    page: WikiPageSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = page.updatedAt?.toDisplayTime()?.let { "更新于 $it" } ?: "站点文章",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = "打开文章", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TutorialArticleHeader(page: WikiPage, fromCache: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(page.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = page.path.substringAfter('/', page.path),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Update, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                text = page.updatedAt?.toDisplayTime()?.let { "最后更新 $it" } ?: "更新时间未知",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (fromCache) {
                Text(
                    text = "缓存内容",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun TutorialNeighborRow(
    neighbors: TutorialPageNeighbors,
    onOpenPage: (Int) -> Unit,
) {
    if (neighbors.previous == null && neighbors.next == null) return
    Row(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (neighbors.previous != null) {
            Surface(
                modifier = Modifier.weight(1f).clickable { onOpenPage(neighbors.previous.id) },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("上一页", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(neighbors.previous.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        if (neighbors.next != null) {
            Surface(
                modifier = Modifier.weight(1f).clickable { onOpenPage(neighbors.next.id) },
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.End) {
                    Text("下一页", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                    Text(neighbors.next.title, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.End, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TutorialSectionHeading(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TutorialLoadingState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Text("正在同步教程目录…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TutorialErrorState(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            Button(onClick = onRetry) { Text("重试") }
        }
    }
}

@Composable
private fun TutorialSyncWarning(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Update, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onTertiaryContainer, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onRetry, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("重试") }
        }
    }
}

@Composable
private fun TutorialEmptyState(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(message, modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun syncLabel(uiState: TutorialUiState): String = when {
    uiState.fromCache -> "上次同步内容"
    uiState.lastSyncAt != null -> "已同步"
    else -> "等待同步"
}

private fun <T> androidx.compose.foundation.lazy.LazyListScope.itemsIndexedStable(
    items: List<T>,
    itemContent: @Composable (index: Int, item: T) -> Unit,
) {
    items(items.size, key = { "article-block-$it" }) { index ->
        itemContent(index, items[index])
    }
}
