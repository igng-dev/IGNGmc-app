package net.igng.mcstatus.data

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class ChatRepository(
    private val mcBaseUrl: String,
    private val client: OkHttpClient = OkHttpClient(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    suspend fun bootstrap(): ChatBootstrap =
        get("$mcBaseUrl/api/chatlogs/sources")

    suspend fun chatlogs(
        source: String,
        serverId: String? = null,
        groupId: String? = null,
        limit: Int = 100,
        start: String? = null,
        end: String? = null,
        senderId: String? = null,
        atAll: Boolean = false,
        messageId: String? = null,
        beforeId: String? = null,
    ): ChatLogsResponse = withContext(Dispatchers.IO) {
        val url = "$mcBaseUrl/api/chatlogs".toHttpUrl().newBuilder()
            .addQueryParameter("source", source)
            .addQueryParameter("limit", limit.coerceIn(1, 200).toString())
            .apply {
                if (source == "qq") {
                    if (!groupId.isNullOrBlank()) addQueryParameter("groupId", groupId)
                } else {
                    if (!serverId.isNullOrBlank()) addQueryParameter("serverId", serverId)
                }
                if (!start.isNullOrBlank()) addQueryParameter("start", start)
                if (!end.isNullOrBlank()) addQueryParameter("end", end)
                if (!senderId.isNullOrBlank()) addQueryParameter("senderId", senderId)
                if (source == "qq" && atAll) addQueryParameter("atAll", "1")
                if (!messageId.isNullOrBlank()) addQueryParameter("messageId", messageId)
                if (!beforeId.isNullOrBlank()) addQueryParameter("beforeId", beforeId)
            }
            .build()
        get(url.toString())
    }

    private suspend inline fun <reified T> get(url: String): T =
        withContext(Dispatchers.IO) { request(url) }

    private inline fun <reified T> request(url: String): T {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val error = runCatching {
                    json.parseToJsonElement(text).jsonObject["error"]?.toString()?.trim('"')
                }.getOrNull()
                throw IOException(error ?: "请求失败 ${response.code}")
            }
            return json.decodeFromString(text)
        }
    }
}

@Serializable
data class ChatBootstrap(
    val servers: List<ChatServerOption> = emptyList(),
    val qqGroups: List<ChatQqGroupOption> = emptyList(),
)

@Serializable
data class ChatServerOption(
    val id: Int,
    val name: String,
    val address: String? = null,
)

@Serializable
data class ChatQqGroupOption(
    val id: String,
    val name: String,
    val count: Int? = null,
)

@Serializable
data class ChatLogsResponse(
    val messages: List<ChatMessage> = emptyList(),
    val hasMore: Boolean = false,
    val focusedId: String? = null,
)

@Serializable
data class ChatMessage(
    val id: String,
    val source: String,
    val source_id: Long? = null,
    val server_id: Int? = null,
    val group_id: String? = null,
    val player_name: String,
    val sender_id: String? = null,
    val content: String,
    val sent_at: String,
    val moderation: ChatModeration? = null,
    val is_at_all: Boolean = false,
)

@Serializable
data class ChatModeration(
    val level: String = "general",
    val label: String = "已处理",
)
