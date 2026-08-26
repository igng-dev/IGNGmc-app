package net.igng.mcstatus.data

import kotlinx.serialization.json.Json

class InfoHallRepository(
    private val mcBaseUrl: String,
    private val client: ApiClient = ApiClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    },
) {
    suspend fun fetchLands(): List<InfoHallLand> =
        json.decodeFromString<InfoHallLandsResponse>(
            client.get("$mcBaseUrl/api/hall/lands"),
        ).lands

    suspend fun fetchFakePlayers(): List<InfoHallFakePlayer> =
        json.decodeFromString<InfoHallFakePlayersResponse>(
            client.get("$mcBaseUrl/api/hall/fake-players"),
        ).fakePlayers
}
