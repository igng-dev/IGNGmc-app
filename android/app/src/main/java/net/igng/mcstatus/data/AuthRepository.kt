package net.igng.mcstatus.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.Json

class AuthRepository(
    private val ssoBaseUrl: String,
    private val client: ApiClient = ApiClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    },
) {
    suspend fun captcha(): MathCaptcha =
        json.decodeFromString(client.get("$ssoBaseUrl/api/auth/math-captcha"))

    suspend fun login(
        identifier: String,
        password: String,
        duration: String,
        captcha: MathCaptcha,
        answer: String,
    ): LoginResponse {
        val body = buildJsonObject {
            put("identifier", identifier)
            put("password", password)
            put("duration", duration)
            put("deviceName", "IGNGmc Android")
            put("mathCaptchaToken", captcha.token)
            put("mathCaptchaAnswer", answer)
        }
        return json.decodeFromString(
            client.post("$ssoBaseUrl/api/mobile/auth/login", body.toString()),
        )
    }

    suspend fun me(token: String): LoginUser {
        val response: MobileMeResponse = json.decodeFromString(
            client.get("$ssoBaseUrl/api/mobile/auth/me", token),
        )
        return response.user
    }

    suspend fun logout(token: String) {
        client.post("$ssoBaseUrl/api/mobile/auth/logout", "{}", token)
    }

    @Serializable
    private data class MobileMeResponse(
        val success: Boolean = false,
        val user: LoginUser,
    )
}
