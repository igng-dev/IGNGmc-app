package net.igng.mcstatus

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    // 4 primary floating bottom-bar tabs
    @Serializable data object Explore : AppRoute
    @Serializable data object Chat : AppRoute
    @Serializable data object Wiki : AppRoute
    @Serializable data object PlayerCenter : AppRoute

    // Public discovery destinations
    @Serializable data object GlobalSearch : AppRoute
    @Serializable data object StatusWall : AppRoute
    @Serializable data class WorldMap(val familySlug: String? = null, val seasonSlug: String? = null) : AppRoute
    @Serializable data class FamilyDetail(val familySlug: String) : AppRoute
    @Serializable data class PlayerPassport(val igngId: Int) : AppRoute
    @Serializable data class McCharacter(val username: String) : AppRoute
    @Serializable data object CommunityHall : AppRoute
    @Serializable data object PerfectRoster : AppRoute
    @Serializable data class WikiArticle(val pageId: Int) : AppRoute

    // Chat conversation detail (hides bottom floating nav bar for full-screen reading)
    @Serializable data class ChatConversation(
        val source: String,
        val targetId: String,
        val platform: String,
        val isPrivate: Boolean = false,
        val title: String = "",
        val avatarUrl: String? = null,
        val isAdminGroup: Boolean = false,
        val isTechGroup: Boolean = false,
        val personaLabel: String? = null,
        val personaDescription: String? = null,
        val initialFocusMessageId: String? = null,
    ) : AppRoute

    // Dash Player Center destinations
    @Serializable data object Characters : AppRoute
    @Serializable data object Vault : AppRoute
    @Serializable data object Lands : AppRoute
    @Serializable data object Teleports : AppRoute
    @Serializable data object FakePlayers : AppRoute
    @Serializable data object VanillaStats : AppRoute
    @Serializable data object Medals : AppRoute
    @Serializable data object PersonalPerfect : AppRoute
    @Serializable data object Permissions : AppRoute
    @Serializable data object LoginActivity : AppRoute

    // Tickets
    @Serializable data object Tickets : AppRoute
    @Serializable data class TicketDetail(val ticketId: Int) : AppRoute
    @Serializable data object CreateTicket : AppRoute

    // Admin pocket console & Settings
    @Serializable data object AdminConsole : AppRoute
    @Serializable data object Settings : AppRoute
}
