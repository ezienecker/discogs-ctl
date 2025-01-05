package de.ezienecker.shared.discogs.client.auth.providers

import co.touchlab.kermit.Logger
import de.ezienecker.shared.discogs.client.auth.providers.DiscogsAuthProvider.Companion.AUTH_SCHEME_DISCOGS
import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.AuthProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.utils.io.KtorDsl

/**
 * Installs the client's [DiscogsAuthConfig].
 */
@KtorDsl
fun AuthConfig.discogs(block: DiscogsAuthConfig.() -> Unit) {
    with(DiscogsAuthConfig().apply(block)) {
        this@discogs.providers.add(DiscogsAuthProvider(credentials, sendWithoutRequest))
    }
}

/**
 * A configuration for [DiscogsAuthConfig].
 */
@KtorDsl
class DiscogsAuthConfig {

    internal var sendWithoutRequest: (HttpRequestBuilder) -> Boolean = { false }

    /**
     * Sends credentials without waiting for [HttpStatusCode.Unauthorized] or without the server having to respond with AuthRequired.
     */
    fun sendWithoutRequest(block: (HttpRequestBuilder) -> Boolean) {
        sendWithoutRequest = block
    }

    internal lateinit var credentials: suspend () -> DiscogsAuthCredentials?

    /**
     * Allows you to specify authentication credentials.
     */
    fun credentials(block: suspend () -> DiscogsAuthCredentials?) {
        credentials = block
    }
}

interface DiscogsAuthCredentials {

    fun authValue(): String
}

/**
 * Contains credentials (token) for DiscogsAuthCredentials
 */
class DiscogsTokenAuthCredentials(
    private val token: String,
) : DiscogsAuthCredentials {
    override fun authValue(): String {
        return "token=$token"
    }
}

class DiscogsAuthProvider(
    private val credentials: suspend () -> DiscogsAuthCredentials?,
    private val sendWithoutRequestCallback: (HttpRequestBuilder) -> Boolean = { false }
) : AuthProvider {

    @Suppress("OverridingDeprecatedMember")
    @Deprecated(
        "Please use sendWithoutRequest function instead", level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith(
            "sendWithoutRequest(request)",
            "de.ezienecker.shared.discogs.client.auth.providers"
        )
    )
    override val sendWithoutRequest: Boolean
        get() = error("Deprecated")

    override fun sendWithoutRequest(request: HttpRequestBuilder): Boolean = sendWithoutRequestCallback(request)

    override fun isApplicable(auth: HttpAuthHeader): Boolean {
        return if (!AUTH_SCHEME_DISCOGS.equals(auth.authScheme, ignoreCase = true)) {
            Logger.e { "Discogs Auth Provider is not applicable for $auth" }
            false
        } else {
            true
        }
    }

    override suspend fun addRequestHeaders(request: HttpRequestBuilder, authHeader: HttpAuthHeader?) {
        credentials.invoke()?.let {
            request.headers[HttpHeaders.Authorization] = constructDiscogsAuthValue(it)
        }
    }

    companion object {
        const val AUTH_SCHEME_DISCOGS = "Discogs"
    }
}

internal fun constructDiscogsAuthValue(credentials: DiscogsAuthCredentials): String =
    "$AUTH_SCHEME_DISCOGS ${credentials.authValue()}"
