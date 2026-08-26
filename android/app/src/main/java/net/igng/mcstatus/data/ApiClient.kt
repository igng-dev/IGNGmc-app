package net.igng.mcstatus.data

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ApiException(
    val statusCode: Int,
    val errorCode: String?,
    message: String,
) : IOException(message)

class ApiClient(
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    },
) {
    suspend fun get(
        url: String,
        token: String? = null,
    ): String = request("GET", url, null, token)

    suspend fun post(
        url: String,
        body: String? = null,
        token: String? = null,
    ): String = request("POST", url, body, token)

    suspend fun patch(
        url: String,
        body: String,
        token: String? = null,
    ): String = request("PATCH", url, body, token)

    private suspend fun request(
        method: String,
        url: String,
        body: String?,
        token: String?,
    ): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .apply {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }
            .method(
                method,
                body?.toRequestBody(JSON_MEDIA_TYPE),
            )
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val error = runCatching {
                    json.parseToJsonElement(text).jsonObject
                }.getOrNull()
                val message = error?.get("error")?.jsonPrimitive?.contentOrNull
                    ?: "请求失败（${response.code}）"
                val code = error?.get("code")?.jsonPrimitive?.contentOrNull
                throw ApiException(response.code, code, message)
            }
            text
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
