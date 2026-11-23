package net.igng.mcstatus.core.network

import java.io.IOException

class ApiException(
    val statusCode: Int,
    val errorCode: String?,
    override val message: String,
    val retryAfterSeconds: Int? = null,
) : IOException(message) {
    val isUnauthorized: Boolean get() = statusCode == 401

    /**
     * Distinguishes OAuth session failures (missing/expired/invalid/revoked access token) from
     * business-level HTTP 401 errors such as `mc.password_invalid` when resetting an in-game password.
     */
    val isSessionAuthFailure: Boolean
        get() = statusCode == 401 && (
            errorCode.isNullOrBlank() || (
                errorCode.startsWith("auth.", ignoreCase = true) &&
                    !errorCode.equals("auth.credential_invalid", ignoreCase = true)
                )
            )
    val isForbidden: Boolean get() = statusCode == 403
    val isNotFound: Boolean get() = statusCode == 404
    val isRateLimited: Boolean get() = statusCode == 429
    val isServerTransient: Boolean get() = statusCode in 502..504
}
