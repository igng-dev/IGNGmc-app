package net.igng.mcstatus.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WikiCacheStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    suspend fun readCatalog(): String? = withContext(Dispatchers.IO) {
        preferences.getString(KEY_CATALOG, null)
    }

    suspend fun writeCatalog(value: String) = withContext(Dispatchers.IO) {
        preferences.edit().putString(KEY_CATALOG, value).apply()
    }

    suspend fun readPage(pageId: Int): String? = withContext(Dispatchers.IO) {
        preferences.getString(pageKey(pageId), null)
    }

    suspend fun writePage(pageId: Int, value: String) = withContext(Dispatchers.IO) {
        preferences.edit().putString(pageKey(pageId), value).apply()
    }

    private fun pageKey(pageId: Int): String = "page_$pageId"

    private companion object {
        const val PREFERENCES_NAME = "igng_wiki_cache"
        const val KEY_CATALOG = "catalog"
    }
}
