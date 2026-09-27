package net.igng.mcstatus.data

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl

class CustomerTicketRepository(
    private val mcBaseUrl: String,
    private val client: ApiClient = ApiClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    },
) {
    suspend fun list(
        token: String,
        relation: String,
        includeClosed: Boolean,
        query: String,
        page: Int,
        limit: Int = 20,
    ): TicketListResponse {
        val url = "$mcBaseUrl/api/tickets".toHttpUrl().newBuilder()
            .addQueryParameter("relation", relation)
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("limit", limit.coerceIn(1, 50).toString())
            .apply {
                if (includeClosed) addQueryParameter("all", "1")
                if (query.isNotBlank()) addQueryParameter("q", query.trim())
            }
            .build()
        return json.decodeFromString(client.get(url.toString(), token))
    }

    suspend fun options(token: String): TicketOptions =
        json.decodeFromString(
            client.get("$mcBaseUrl/api/tickets/options", token),
        )

    suspend fun searchPlayers(token: String, query: String): List<TicketPlayerOption> {
        if (query.trim().length < 2) return emptyList()
        val url = "$mcBaseUrl/api/tickets/players".toHttpUrl().newBuilder()
            .addQueryParameter("q", query.trim())
            .build()
        return json.decodeFromString<TicketPlayersResponse>(
            client.get(url.toString(), token),
        ).players
    }

    suspend fun detail(token: String, id: Int): CustomerTicketDetail =
        json.decodeFromString<CustomerTicketDetailResponse>(
            client.get("$mcBaseUrl/api/tickets/$id", token),
        ).ticket

    suspend fun create(token: String, request: CreateTicketRequest): Int =
        json.decodeFromString<CreateTicketResponse>(
            client.post(
                "$mcBaseUrl/api/tickets",
                json.encodeToString(CreateTicketRequest.serializer(), request),
                token,
            ),
        ).id

    suspend fun update(token: String, id: Int, request: UpdateTicketRequest): TicketUpdateResult =
        json.decodeFromString<TicketUpdateResponse>(
            client.patch(
                "$mcBaseUrl/api/tickets/$id",
                json.encodeToString(UpdateTicketRequest.serializer(), request),
                token,
            ),
        ).ticket

    suspend fun reply(token: String, id: Int, content: String): TicketReplyResult =
        json.decodeFromString<TicketReplyResponse>(
            client.post(
                "$mcBaseUrl/api/tickets/$id/replies",
                json.encodeToString(ReplyTicketRequest.serializer(), ReplyTicketRequest(content)),
                token,
            ),
        ).reply

    suspend fun close(token: String, id: Int): TicketCloseResponse =
        json.decodeFromString(
            client.post(
                "$mcBaseUrl/api/tickets/$id/close",
                "{}",
                token,
            ),
        )
}

@kotlinx.serialization.Serializable
data class CreateTicketRequest(
    val title: String,
    val type: String,
    val serverScope: String,
    val serverId: Int? = null,
    val accessPolicy: TicketAccessPolicy,
    val targetNames: List<String> = emptyList(),
    val content: String,
)

@kotlinx.serialization.Serializable
data class UpdateTicketRequest(
    val title: String,
    val type: String,
    val targetNames: List<String> = emptyList(),
    val accessPolicy: TicketAccessPolicy? = null,
)

@kotlinx.serialization.Serializable
data class ReplyTicketRequest(
    val content: String,
)
