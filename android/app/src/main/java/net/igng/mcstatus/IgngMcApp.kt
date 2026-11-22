package net.igng.mcstatus

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.igng.mcstatus.core.auth.AuthRepository
import net.igng.mcstatus.core.auth.CustomTabsLauncher
import net.igng.mcstatus.core.auth.PkceGenerator
import net.igng.mcstatus.core.data.IgngAppContainer
import net.igng.mcstatus.core.data.ServerFamilyDto
import net.igng.mcstatus.core.designsystem.IgngBadge
import net.igng.mcstatus.core.designsystem.IgngBadgeTone
import net.igng.mcstatus.core.designsystem.IgngButton
import net.igng.mcstatus.core.designsystem.IgngButtonStyle
import net.igng.mcstatus.core.designsystem.IgngDetailSheet
import net.igng.mcstatus.core.designsystem.IgngFloatingNavBar
import net.igng.mcstatus.core.designsystem.IgngLoadingVeil
import net.igng.mcstatus.core.designsystem.IgngMotion
import net.igng.mcstatus.core.designsystem.IgngNavTabItem
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.model.AppPreferences
import net.igng.mcstatus.feature.admin.AdminPocketConsoleScreen
import net.igng.mcstatus.feature.chat.ChatScreen
import net.igng.mcstatus.feature.explore.CommunityHallScreen
import net.igng.mcstatus.feature.explore.ExploreScreen
import net.igng.mcstatus.feature.explore.GlobalSearchScreen
import net.igng.mcstatus.feature.explore.McCharacterPublicScreen
import net.igng.mcstatus.feature.explore.PerfectRosterScreen
import net.igng.mcstatus.feature.explore.PlayerPassportScreen
import net.igng.mcstatus.feature.explore.ServerFamilyDetailScreen
import net.igng.mcstatus.feature.map.WorldMapScreen
import net.igng.mcstatus.feature.player.CharactersScreen
import net.igng.mcstatus.feature.player.LoginActivityScreen
import net.igng.mcstatus.feature.player.MyFakePlayersScreen
import net.igng.mcstatus.feature.player.MyLandsScreen
import net.igng.mcstatus.feature.player.MyMedalsScreen
import net.igng.mcstatus.feature.player.MyTeleportsScreen
import net.igng.mcstatus.feature.player.PermissionsScreen
import net.igng.mcstatus.feature.player.PersonalPerfectScreen
import net.igng.mcstatus.feature.player.PlayerCenterScreen
import net.igng.mcstatus.feature.player.PlayerDestination
import net.igng.mcstatus.feature.player.VanillaStatsScreen
import net.igng.mcstatus.feature.player.VaultScreen
import net.igng.mcstatus.feature.status.StatusWallScreen
import net.igng.mcstatus.feature.ticket.CreateTicketScreen
import net.igng.mcstatus.feature.ticket.TicketDetailScreen
import net.igng.mcstatus.feature.ticket.TicketsScreen
import net.igng.mcstatus.feature.wiki.WikiArticleScreen
import net.igng.mcstatus.feature.wiki.WikiCatalogScreen

@Composable
fun IgngMcApp(
    container: IgngAppContainer,
    incomingIntentFlow: StateFlow<Intent?>,
    onIntentConsumed: () -> Unit,
) {
    val preferences by container.settingsRepository.preferences.collectAsState(initial = AppPreferences())
    val activeAccount by container.activeAccount.collectAsState()
    val accounts by container.accounts.collectAsState()
    val dailyExp by container.dailyExp.collectAsState()
    val bootstrap by container.bootstrap.collectAsState()
    val incomingIntent by incomingIntentFlow.collectAsState()

    IgngTheme(
        themeMode = preferences.themeMode,
        motionIntensity = preferences.motionIntensity,
        hapticsEnabled = preferences.hapticsEnabled,
    ) {
        val navController = rememberNavController()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val colors = IgngTheme.colors
        val reduced = IgngTheme.motionReduced

        var authExchanging by remember { mutableStateOf(false) }
        var authError by remember { mutableStateOf<String?>(null) }
        var sessionExpiredPrompt by remember { mutableStateOf(false) }
        var selectedFamilyCache by remember { mutableStateOf<Map<String, ServerFamilyDto>>(emptyMap()) }

        fun startOAuth(forceLogin: Boolean) {
            authError = null
            val pair = PkceGenerator.generate()
            container.oauthPendingStore.save(pair)
            val uri = container.authRepository.buildAuthorizeUri(pair, forceLogin = forceLogin)
            CustomTabsLauncher.launchOAuth(context, uri, colors.isDark)
        }

        // Handle OAuth callbacks & App Shortcut intents
        LaunchedEffect(incomingIntent) {
            val intent = incomingIntent ?: return@LaunchedEffect
            onIntentConsumed()
            val data = intent.data
            if (data != null && data.scheme == AuthRepository.CALLBACK_SCHEME && data.host == AuthRepository.CALLBACK_HOST) {
                val err = data.getQueryParameter("error")
                val code = data.getQueryParameter("code")
                val state = data.getQueryParameter("state")
                if (!err.isNullOrBlank()) {
                    container.oauthPendingStore.clear()
                    authError = if (err == "access_denied") "你已取消本次授权" else "授权未完成（$err）"
                    return@LaunchedEffect
                }
                if (code.isNullOrBlank() || state.isNullOrBlank()) {
                    authError = "回调参数不完整，请重新登录"
                    return@LaunchedEffect
                }
                val verifier = container.oauthPendingStore.consume(state)
                if (verifier == null) {
                    authError = "登录状态校验失败或已过期，请重新授权"
                    return@LaunchedEffect
                }
                authExchanging = true
                runCatching { container.authRepository.exchangeAuthorizationCode(code, verifier) }
                    .onSuccess { account ->
                        container.saveAndActivateAccount(account)
                        sessionExpiredPrompt = false
                    }
                    .onFailure { authError = it.message ?: "换取登录会话失败" }
                authExchanging = false
                return@LaunchedEffect
            }

            when (intent.getStringExtra(EXTRA_SHORTCUT_ACTION)) {
                "status" -> navController.navigate(AppRoute.StatusWall) { launchSingleTop = true }
                "map" -> navController.navigate(AppRoute.WorldMap()) { launchSingleTop = true }
                "chat" -> navController.navigate(AppRoute.Chat) { launchSingleTop = true }
                "create_ticket" -> {
                    if (container.activeAccount.value != null) {
                        navController.navigate(AppRoute.CreateTicket) { launchSingleTop = true }
                    } else {
                        navController.navigate(AppRoute.PlayerCenter) { launchSingleTop = true }
                    }
                }
            }
        }

        // Refresh bootstrap state, user metadata, admin role & settle daily login EXP whenever active user changes
        LaunchedEffect(activeAccount?.userId) {
            val acc = activeAccount
            if (acc == null) {
                runCatching { container.authRepository.fetchBootstrap(token = null) }
                    .onSuccess { container.updateBootstrap(it) }
                return@LaunchedEffect
            }
            runCatching { container.authRepository.fetchBootstrap(acc.token) }.onSuccess { boot ->
                container.updateBootstrap(boot)
                val viewerUser = boot.viewer.user
                if (boot.viewer.authenticated && viewerUser != null) {
                    val updated = acc.copy(
                        username = viewerUser.username.ifBlank { acc.username },
                        nickname = viewerUser.nickname ?: acc.nickname,
                        avatarUrl = viewerUser.avatarUrl ?: acc.avatarUrl,
                        adminRoleWire = boot.viewer.adminRole ?: acc.adminRoleWire,
                    )
                    if (updated != acc) {
                        container.saveAndActivateAccount(updated, resetDailyExp = false)
                    }
                }
            }
            val exp = container.authRepository.settleDailyLogin(acc.token)
            container.updateDailyExp(exp)
        }

        // Global 401 listener: offer seamless re-auth without destroying user's current screen/draft
        LaunchedEffect(Unit) {
            container.sessionExpiredNotifier.events.collect {
                if (container.activeAccount.value != null) {
                    sessionExpiredPrompt = true
                }
            }
        }

        val backStackEntry by navController.currentBackStackEntryAsState()
        val dest = backStackEntry?.destination
        val activeTabId = when {
            dest?.hasRoute<AppRoute.Explore>() == true -> "explore"
            dest?.hasRoute<AppRoute.Chat>() == true -> "chat"
            dest?.hasRoute<AppRoute.Wiki>() == true -> "wiki"
            dest?.hasRoute<AppRoute.PlayerCenter>() == true -> "player"
            else -> null
        }

        val navItems = remember {
            listOf(
                IgngNavTabItem("explore", "探索", Icons.Rounded.Explore),
                IgngNavTabItem("chat", "聊天", Icons.Rounded.Forum),
                IgngNavTabItem("wiki", "百科", Icons.Rounded.AutoStories),
                IgngNavTabItem("player", "我的", Icons.Rounded.Person),
            )
        }

        val bottomBarPadding = PaddingValues(bottom = if (activeTabId != null) 88.dp else 0.dp)

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = colors.canvas,
                bottomBar = {},
            ) { _ ->
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Explore,
                    enterTransition = {
                        fadeIn(IgngMotion.normalTween(reduced)) +
                            slideInHorizontally(IgngMotion.standardSpring(reduced)) { it / 14 }
                    },
                    exitTransition = { fadeOut(IgngMotion.microTween(reduced)) },
                    popEnterTransition = {
                        fadeIn(IgngMotion.normalTween(reduced)) +
                            slideInHorizontally(IgngMotion.standardSpring(reduced)) { -it / 16 }
                    },
                    popExitTransition = {
                        fadeOut(IgngMotion.microTween(reduced)) +
                            slideOutHorizontally(IgngMotion.standardSpring(reduced)) { it / 14 }
                    },
                ) {
                    composable<AppRoute.Explore> {
                        ExploreScreen(
                            exploreRepository = container.exploreRepository,
                            statusRepository = container.statusRepository,
                            activeAccount = activeAccount,
                            bottomPadding = bottomBarPadding,
                            onOpenSearch = { navController.navigate(AppRoute.GlobalSearch) },
                            onOpenStatusWall = { navController.navigate(AppRoute.StatusWall) },
                            onOpenMap = { fam, season -> navController.navigate(AppRoute.WorldMap(fam, season)) },
                            onOpenFamilyDetail = { fam ->
                                selectedFamilyCache = selectedFamilyCache + (fam.slug to fam)
                                navController.navigate(AppRoute.FamilyDetail(fam.slug))
                            },
                            onOpenCommunityHall = { navController.navigate(AppRoute.CommunityHall) },
                            onOpenPerfectWall = { navController.navigate(AppRoute.PerfectRoster) },
                            onOpenMyProfile = {
                                navController.navigate(AppRoute.PlayerCenter) {
                                    popUpTo(AppRoute.Explore) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }

                    composable<AppRoute.Chat> {
                        ChatScreen(
                            endpoints = container.endpoints,
                            chatRepository = container.chatRepository,
                            bottomPadding = bottomBarPadding,
                        )
                    }

                    composable<AppRoute.Wiki> {
                        WikiCatalogScreen(
                            wikiRepository = container.wikiRepository,
                            settingsRepository = container.settingsRepository,
                            bottomPadding = bottomBarPadding,
                            onOpenArticle = { pageId -> navController.navigate(AppRoute.WikiArticle(pageId)) },
                        )
                    }

                    composable<AppRoute.PlayerCenter> {
                        PlayerCenterScreen(
                            activeAccount = activeAccount,
                            dailyExp = dailyExp,
                            playerRepository = container.playerRepository,
                            ticketRepository = container.ticketRepository,
                            bottomPadding = bottomBarPadding,
                            onSignIn = { startOAuth(forceLogin = false) },
                            onOpenPassport = { id -> navController.navigate(AppRoute.PlayerPassport(id)) },
                            onNavigate = { target ->
                                when (target) {
                                    PlayerDestination.CHARACTERS -> navController.navigate(AppRoute.Characters)
                                    PlayerDestination.TICKETS -> navController.navigate(AppRoute.Tickets)
                                    PlayerDestination.CREATE_TICKET -> navController.navigate(AppRoute.CreateTicket)
                                    PlayerDestination.VAULT -> navController.navigate(AppRoute.Vault)
                                    PlayerDestination.ACTIVITY -> navController.navigate(AppRoute.LoginActivity)
                                    PlayerDestination.LANDS -> navController.navigate(AppRoute.Lands)
                                    PlayerDestination.TELEPORTS -> navController.navigate(AppRoute.Teleports)
                                    PlayerDestination.FAKE_PLAYERS -> navController.navigate(AppRoute.FakePlayers)
                                    PlayerDestination.STATS -> navController.navigate(AppRoute.VanillaStats)
                                    PlayerDestination.MEDALS -> navController.navigate(AppRoute.Medals)
                                    PlayerDestination.PERFECT -> navController.navigate(AppRoute.PersonalPerfect)
                                    PlayerDestination.PERMISSIONS -> navController.navigate(AppRoute.Permissions)
                                    PlayerDestination.ADMIN_CONSOLE -> navController.navigate(AppRoute.AdminConsole)
                                    PlayerDestination.SETTINGS -> navController.navigate(AppRoute.Settings)
                                }
                            },
                        )
                    }

                    composable<AppRoute.GlobalSearch> {
                        GlobalSearchScreen(
                            exploreRepository = container.exploreRepository,
                            onBack = { navController.popBackStack() },
                            onOpenPlayerPassport = { id -> navController.navigate(AppRoute.PlayerPassport(id)) },
                            onOpenMcCharacter = { name -> navController.navigate(AppRoute.McCharacter(name)) },
                        )
                    }

                    composable<AppRoute.StatusWall> {
                        StatusWallScreen(
                            statusRepository = container.statusRepository,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<AppRoute.WorldMap> { entry ->
                        val route = entry.toRoute<AppRoute.WorldMap>()
                        WorldMapScreen(
                            endpoints = container.endpoints,
                            exploreRepository = container.exploreRepository,
                            initialFamilySlug = route.familySlug,
                            initialSeasonSlug = route.seasonSlug,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<AppRoute.FamilyDetail> { entry ->
                        val route = entry.toRoute<AppRoute.FamilyDetail>()
                        val fam = selectedFamilyCache[route.familySlug]
                        if (fam != null) {
                            ServerFamilyDetailScreen(
                                family = fam,
                                onBack = { navController.popBackStack() },
                                onOpenMap = { fSlug, sSlug -> navController.navigate(AppRoute.WorldMap(fSlug, sSlug)) },
                            )
                        } else {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        }
                    }

                    composable<AppRoute.PlayerPassport> { entry ->
                        val route = entry.toRoute<AppRoute.PlayerPassport>()
                        PlayerPassportScreen(
                            igngId = route.igngId,
                            exploreRepository = container.exploreRepository,
                            onBack = { navController.popBackStack() },
                            onOpenMcCharacter = { name -> navController.navigate(AppRoute.McCharacter(name)) },
                        )
                    }

                    composable<AppRoute.McCharacter> { entry ->
                        val route = entry.toRoute<AppRoute.McCharacter>()
                        McCharacterPublicScreen(
                            username = route.username,
                            exploreRepository = container.exploreRepository,
                            onBack = { navController.popBackStack() },
                            onOpenPassport = { id -> navController.navigate(AppRoute.PlayerPassport(id)) },
                        )
                    }

                    composable<AppRoute.CommunityHall> {
                        CommunityHallScreen(
                            exploreRepository = container.exploreRepository,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<AppRoute.PerfectRoster> {
                        PerfectRosterScreen(
                            exploreRepository = container.exploreRepository,
                            onBack = { navController.popBackStack() },
                            onOpenPassport = { id -> navController.navigate(AppRoute.PlayerPassport(id)) },
                        )
                    }

                    composable<AppRoute.WikiArticle> { entry ->
                        val route = entry.toRoute<AppRoute.WikiArticle>()
                        WikiArticleScreen(
                            pageId = route.pageId,
                            wikiRepository = container.wikiRepository,
                            settingsRepository = container.settingsRepository,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<AppRoute.Characters> {
                        CharactersScreen(container.endpoints, container.playerRepository) { navController.popBackStack() }
                    }
                    composable<AppRoute.Vault> {
                        VaultScreen(container.playerRepository) { navController.popBackStack() }
                    }
                    composable<AppRoute.Lands> {
                        MyLandsScreen(container.playerRepository) { navController.popBackStack() }
                    }
                    composable<AppRoute.Teleports> {
                        MyTeleportsScreen(container.playerRepository) { navController.popBackStack() }
                    }
                    composable<AppRoute.FakePlayers> {
                        MyFakePlayersScreen(container.playerRepository) { navController.popBackStack() }
                    }
                    composable<AppRoute.VanillaStats> {
                        VanillaStatsScreen(container.playerRepository) { navController.popBackStack() }
                    }
                    composable<AppRoute.Medals> {
                        MyMedalsScreen(container.playerRepository) { navController.popBackStack() }
                    }
                    composable<AppRoute.PersonalPerfect> {
                        PersonalPerfectScreen(container.playerRepository) { navController.popBackStack() }
                    }
                    composable<AppRoute.Permissions> {
                        PermissionsScreen(container.playerRepository) { navController.popBackStack() }
                    }
                    composable<AppRoute.LoginActivity> {
                        LoginActivityScreen(bootstrap = bootstrap) { navController.popBackStack() }
                    }

                    composable<AppRoute.Tickets> {
                        TicketsScreen(
                            ticketRepository = container.ticketRepository,
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { id -> navController.navigate(AppRoute.TicketDetail(id)) },
                            onCreateTicket = { navController.navigate(AppRoute.CreateTicket) },
                        )
                    }

                    composable<AppRoute.TicketDetail> { entry ->
                        val route = entry.toRoute<AppRoute.TicketDetail>()
                        TicketDetailScreen(
                            ticketId = route.ticketId,
                            endpoints = container.endpoints,
                            ticketRepository = container.ticketRepository,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<AppRoute.CreateTicket> {
                        CreateTicketScreen(
                            endpoints = container.endpoints,
                            ticketRepository = container.ticketRepository,
                            onBack = { navController.popBackStack() },
                            onCreated = { id ->
                                navController.popBackStack()
                                navController.navigate(AppRoute.TicketDetail(id))
                            },
                        )
                    }

                    composable<AppRoute.AdminConsole> {
                        val role = activeAccount?.adminRole
                        if (role != null) {
                            AdminPocketConsoleScreen(
                                role = role,
                                endpoints = container.endpoints,
                                adminRepository = container.adminRepository,
                                onBack = { navController.popBackStack() },
                            )
                        } else {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        }
                    }

                    composable<AppRoute.Settings> {
                        SettingsScreen(
                            container = container,
                            preferences = preferences,
                            activeAccount = activeAccount,
                            accounts = accounts,
                            onBack = { navController.popBackStack() },
                            onStartOAuthLogin = { force -> startOAuth(forceLogin = force) },
                            onSwitchAccount = { uid -> container.switchAccount(uid) },
                            onLogoutActive = {
                                val token = container.activeAccount.value?.token
                                scope.launch {
                                    if (!token.isNullOrBlank()) {
                                        runCatching { container.authRepository.logout(token) }
                                    }
                                    container.removeActiveAccount()
                                }
                            },
                        )
                    }
                }
            }

            // Floating Glass Bottom Navigation Pill (visible only on the 4 top-level tabs)
            AnimatedVisibility(
                visible = activeTabId != null,
                enter = slideInVertically(IgngMotion.standardSpring(reduced)) { it } + fadeIn(IgngMotion.microTween(reduced)),
                exit = slideOutVertically(IgngMotion.standardSpring(reduced)) { it } + fadeOut(IgngMotion.microTween(reduced)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                IgngFloatingNavBar(
                    items = navItems,
                    selectedId = activeTabId ?: "explore",
                    onSelect = { id ->
                        val route: AppRoute = when (id) {
                            "chat" -> AppRoute.Chat
                            "wiki" -> AppRoute.Wiki
                            "player" -> AppRoute.PlayerCenter
                            else -> AppRoute.Explore
                        }
                        navController.navigate(route) {
                            popUpTo(AppRoute.Explore) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            // Full-screen Folded Core veil during OAuth PKCE token exchange
            IgngLoadingVeil(visible = authExchanging)

            if (authError != null) {
                IgngDetailSheet(
                    onDismiss = { authError = null },
                    title = "登录未完成",
                ) {
                    IgngBadge(authError!!, tone = IgngBadgeTone.Danger)
                    Spacer(Modifier.height(14.dp))
                    IgngButton(
                        text = "重新通过 IGNG SSO 登录",
                        onClick = {
                            authError = null
                            startOAuth(forceLogin = false)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (sessionExpiredPrompt) {
                IgngDetailSheet(
                    onDismiss = { sessionExpiredPrompt = false },
                    title = "登录会话已失效",
                    subtitle = "重新授权后即可继续，你正在编辑的内容不会被清除。",
                ) {
                    Text(
                        "当前设备的 IGNG 登录会话可能已过期或在账户中心被撤销。",
                        style = IgngTheme.type.body,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(14.dp))
                    IgngButton(
                        text = "重新授权登录",
                        onClick = {
                            sessionExpiredPrompt = false
                            startOAuth(forceLogin = false)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    IgngButton(
                        text = "稍后再说",
                        onClick = { sessionExpiredPrompt = false },
                        style = IgngButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

const val EXTRA_SHORTCUT_ACTION = "igng_shortcut_action"
