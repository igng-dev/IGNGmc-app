package net.igng.mcstatus.core.model

data class SiteEndpoints(
    val mcBaseUrl: String = "https://mc.igng.net",
    val dashBaseUrl: String = "https://dash.igng.net",
    val ssoBaseUrl: String = "https://sso.igng.net",
    val chatBaseUrl: String = "https://chat.igng.net",
    val wikiBaseUrl: String = "https://docs.igng.net",
    val mapRelayUrl: String = "https://map-api.igng.net",
    val bbsBaseUrl: String = "https://www.igngbbs.net",
    val igngBaseUrl: String = "https://www.igng.net",
) {
    val mc: String = mcBaseUrl.trimEnd('/')
    val dash: String = dashBaseUrl.trimEnd('/')
    val sso: String = ssoBaseUrl.trimEnd('/')
    val chat: String = chatBaseUrl.trimEnd('/')
    val wiki: String = wikiBaseUrl.trimEnd('/')
    val mapRelay: String = mapRelayUrl.trimEnd('/')
    val bbs: String = bbsBaseUrl.trimEnd('/')
    val igng: String = igngBaseUrl.trimEnd('/')

    fun trustedAuthHosts(): Set<String> = setOf(
        hostOf(mc),
        hostOf(dash),
        hostOf(sso),
        hostOf(chat),
        hostOf(igng),
        "account.igng.net",
    ).filter { it.isNotBlank() }.toSet()

    fun resolveDashMediaUrl(pathOrUrl: String?): String? {
        if (pathOrUrl.isNullOrBlank()) return null
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) return pathOrUrl
        return "$dash/${pathOrUrl.trimStart('/')}"
    }

    fun resolveChatMediaUrl(pathOrUrl: String?): String? {
        if (pathOrUrl.isNullOrBlank()) return null
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) return pathOrUrl
        return "$chat/${pathOrUrl.trimStart('/')}"
    }

    fun snapshotMapUrls(seasonSlug: String): List<String> = listOf(
        "$mc/map-snapshots/$seasonSlug.webp",
        "$mc/map-snapshots/$seasonSlug.png",
    )

    fun liveMapWebUrl(familySlug: String, seasonSlug: String? = null): String =
        if (seasonSlug.isNullOrBlank()) "$mc/map/$familySlug" else "$mc/map/$familySlug/$seasonSlug"

    private fun hostOf(url: String): String =
        url.removePrefix("https://").removePrefix("http://").substringBefore('/').substringBefore(':').lowercase()
}
