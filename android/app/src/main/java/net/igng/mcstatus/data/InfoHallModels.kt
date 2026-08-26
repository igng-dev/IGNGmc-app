package net.igng.mcstatus.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class InfoHallLand(
    val id: Long = 0,
    val serverId: Int? = null,
    val serverName: String = "",
    val name: String = "",
    val ownerName: String = "",
    val world: String? = null,
    val center: InfoHallPoint = InfoHallPoint(),
    val size: InfoHallSize = InfoHallSize(),
    val corners: List<InfoHallPoint> = emptyList(),
    val playerPermissions: Map<String, JsonElement> = emptyMap(),
    val environmentPermissions: Map<String, JsonElement> = emptyMap(),
    val description: String = "",
)

@Serializable
data class InfoHallPoint(
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
)

@Serializable
data class InfoHallSize(
    val width: Double? = null,
    val height: Double? = null,
    val length: Double? = null,
)

@Serializable
data class InfoHallFakePlayer(
    val name: String = "",
    val ownerName: String = "未知",
    val serverName: String = "未命名服务器",
    val onlineSeconds: Long = 0,
)

@Serializable
data class InfoHallLandsResponse(
    val success: Boolean = false,
    val lands: List<InfoHallLand> = emptyList(),
)

@Serializable
data class InfoHallFakePlayersResponse(
    val success: Boolean = false,
    val fakePlayers: List<InfoHallFakePlayer> = emptyList(),
)
