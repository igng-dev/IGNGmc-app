package net.igng.mcstatus.data

import android.net.Uri
import kotlinx.serialization.json.Json

class WikiRepository(
    baseUrl: String,
    private val client: ApiClient = ApiClient(),
    private val cache: WikiCacheStore? = null,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    },
) {
    private val wikiBaseUrl = baseUrl.trimEnd('/')

    suspend fun fetchCatalog(): WikiCatalogResponse {
        val raw = client.get(
            "$wikiBaseUrl/api/wiki/catalog?kb=${Uri.encode(KNOWLEDGE_BASE_SLUG)}",
        )
        cache?.writeCatalog(raw)
        return json.decodeFromString(raw)
    }

    suspend fun readCachedCatalog(): WikiCatalogResponse? =
        cache?.readCatalog()?.let { raw -> runCatching { json.decodeFromString<WikiCatalogResponse>(raw) }.getOrNull() }

    suspend fun fetchPage(pageId: Int): WikiPage {
        val raw = client.get(
            "$wikiBaseUrl/api/wiki/page?kb=${Uri.encode(KNOWLEDGE_BASE_SLUG)}&pageId=$pageId",
        )
        cache?.writePage(pageId, raw)
        return json.decodeFromString<WikiPageResponse>(raw).page
    }

    suspend fun readCachedPage(pageId: Int): WikiPage? =
        cache?.readPage(pageId)?.let { raw ->
            runCatching { json.decodeFromString<WikiPageResponse>(raw).page }.getOrNull()
        }

    private companion object {
        const val KNOWLEDGE_BASE_SLUG = "IGNGmc"
    }
}
