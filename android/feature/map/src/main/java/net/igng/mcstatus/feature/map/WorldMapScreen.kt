package net.igng.mcstatus.feature.map

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import net.igng.mcstatus.core.auth.CustomTabsLauncher
import net.igng.mcstatus.core.data.ExploreRepository
import net.igng.mcstatus.core.data.ServerFamilyDto
import net.igng.mcstatus.core.data.ServerSeasonDto
import net.igng.mcstatus.core.designsystem.IgngAmbientCanvas
import net.igng.mcstatus.core.designsystem.IgngBadge
import net.igng.mcstatus.core.designsystem.IgngBadgeTone
import net.igng.mcstatus.core.designsystem.IgngDetailSheet
import net.igng.mcstatus.core.designsystem.IgngInteractiveTile
import net.igng.mcstatus.core.designsystem.IgngLoadingBlock
import net.igng.mcstatus.core.designsystem.IgngStateBlock
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.model.IgngFormatters
import net.igng.mcstatus.core.model.SiteEndpoints

data class MapTargetItem(
    val family: ServerFamilyDto,
    val season: ServerSeasonDto,
    val isSnapshot: Boolean,
)

@Composable
fun WorldMapScreen(
    endpoints: SiteEndpoints,
    exploreRepository: ExploreRepository,
    initialFamilySlug: String?,
    initialSeasonSlug: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val colors = IgngTheme.colors
    var targets by remember { mutableStateOf<List<MapTargetItem>>(emptyList()) }
    var selected by remember { mutableStateOf<MapTargetItem?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showSwitcher by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        loading = true
        val families = runCatching { exploreRepository.fetchHomeFamilies() }
            .getOrElse { exploreRepository.readCachedFamilies() }
        val built = families.flatMap { fam ->
            fam.allKnownSeasons
                .filter { it.hasMap || it.mapMode != "none" || it.state == "retired" }
                .map { season ->
                    MapTargetItem(
                        family = fam,
                        season = season,
                        isSnapshot = season.state == "retired" || season.mapMode == "snapshot",
                    )
                }
        }
        targets = built
        selected = built.firstOrNull {
            it.family.slug == initialFamilySlug && (initialSeasonSlug == null || it.season.slug == initialSeasonSlug)
        } ?: built.firstOrNull { !it.isSnapshot } ?: built.firstOrNull()
        loading = false
    }

    IgngAmbientCanvas {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading -> IgngLoadingBlock(fill = true)
                selected == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    IgngStateBlock(title = "暂无可查看的服务器地图", description = "当前没有已配置的实时地图或历史周目快照。")
                }
                selected!!.isSnapshot -> {
                    NativeSnapshotMapCanvas(
                        endpoints = endpoints,
                        target = selected!!,
                    )
                }
                else -> {
                    LiveConfluxMapWebView(
                        url = endpoints.liveMapWebUrl(selected!!.family.slug, selected!!.season.slug),
                        reloadKey = reloadKey,
                    )
                }
            }

            // IGNG Native Floating Top Island matching `igng-map-island`
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = IgngTheme.radii.pill,
                    color = colors.glassBar,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.lineStrong),
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(40.dp).clip(IgngTheme.radii.pill).clickable(onClick = onBack),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = colors.textPrimary, modifier = Modifier.size(19.dp))
                    }
                }

                Surface(
                    shape = IgngTheme.radii.pill,
                    color = colors.glassBar,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.lineStrong),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clip(IgngTheme.radii.pill)
                        .clickable(enabled = targets.isNotEmpty()) { showSwitcher = true },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selected?.let { "${it.family.name} · ${it.season.label.ifBlank { it.season.slug }}" } ?: "选择地图",
                                style = IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (selected?.isSnapshot == true) "历史快照（双指缩放）" else "实时 Conflux 地图",
                                style = IgngTheme.type.badge,
                                color = colors.textSecondary,
                            )
                        }
                        Icon(Icons.Rounded.ExpandMore, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                if (selected != null && !selected!!.isSnapshot) {
                    Surface(
                        shape = IgngTheme.radii.pill,
                        color = colors.glassBar,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.lineStrong),
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(40.dp).clip(IgngTheme.radii.pill).clickable { reloadKey++ },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Refresh, "刷新", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Surface(
                        shape = IgngTheme.radii.pill,
                        color = colors.glassBar,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.lineStrong),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(IgngTheme.radii.pill)
                            .clickable {
                                CustomTabsLauncher.openExternalUrl(
                                    context,
                                    endpoints.liveMapWebUrl(selected!!.family.slug, selected!!.season.slug),
                                    colors.isDark,
                                )
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.OpenInBrowser, "浏览器打开", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showSwitcher) {
        IgngDetailSheet(
            onDismiss = { showSwitcher = false },
            title = "切换服务器地图",
            subtitle = "实时地图与历史周目快照",
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(targets, key = { "${it.family.slug}:${it.season.slug}" }) { item ->
                    val active = item == selected
                    IgngInteractiveTile(
                        onClick = {
                            selected = item
                            showSwitcher = false
                        },
                        accentBorder = if (active) colors.accent else null,
                        contentPadding = PaddingValues(13.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${item.family.name} · ${item.season.label.ifBlank { item.season.slug }}", style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                Text(
                                    listOfNotNull(item.season.version, IgngFormatters.seasonStateLabel(item.season.state)).joinToString(" · "),
                                    style = IgngTheme.type.caption,
                                    color = colors.textSecondary,
                                )
                            }
                            IgngBadge(
                                text = if (item.isSnapshot) "历史快照" else "实时地图",
                                tone = if (item.isSnapshot) IgngBadgeTone.Neutral else IgngBadgeTone.Success,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeSnapshotMapCanvas(
    endpoints: SiteEndpoints,
    target: MapTargetItem,
) {
    val urls = remember(target.season.slug) { endpoints.snapshotMapUrls(target.season.slug) }
    var sourceIdx by remember(target.season.slug) { mutableIntStateOf(0) }
    var failed by remember(target.season.slug) { mutableStateOf(false) }
    var scale by remember(target.season.slug) { mutableFloatStateOf(1f) }
    var offset by remember(target.season.slug) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IgngTheme.colors.canvas)
            .pointerInput(target.season.slug) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1.2f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = 2.6f
                    }
                })
            }
            .pointerInput(target.season.slug) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val next = (scale * zoom).coerceIn(1f, 6f)
                    scale = next
                    offset = if (next <= 1.02f) Offset.Zero else offset + pan
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (failed) {
            Box(Modifier.padding(24.dp)) {
                IgngStateBlock(
                    title = "历史地图快照尚未生成",
                    description = "${target.family.name} ${target.season.label} 暂无静态快照图像。",
                )
            }
        } else {
            AsyncImage(
                model = urls[sourceIdx],
                contentDescription = "${target.family.name} 历史快照",
                contentScale = ContentScale.Fit,
                onError = {
                    if (sourceIdx < urls.lastIndex) sourceIdx++ else failed = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LiveConfluxMapWebView(
    url: String,
    reloadKey: Int,
) {
    var pageLoading by remember(url, reloadKey) { mutableStateOf(true) }
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val targetUrl = request?.url?.toString().orEmpty()
                            return !targetUrl.startsWith("https://mc.igng.net")
                        }

                        override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                            pageLoading = false
                            // Hide duplicate web top header so native floating island owns chrome cleanly
                            view?.evaluateJavascript(
                                "(function(){var s=document.createElement('style');s.innerHTML='.igng-sh,.igng-map-island--snapshot,.igng-map-island{display:none!important;}';document.head.appendChild(s);})();",
                                null,
                            )
                        }
                    }
                    loadUrl(url)
                }
            },
            update = { webView ->
                if (webView.url != url || reloadKey > 0) {
                    webView.loadUrl(url)
                }
            },
        )
        if (pageLoading) {
            IgngLoadingBlock(fill = true)
        }
    }
}
