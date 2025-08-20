package de.ezienecker.shared.discogs.client.auth.providers

import de.ezienecker.shared.discogs.client.auth.providers.DiscogsAuthentication.Companion.NO_AUTHENTICATION

data class DiscogsClientConfiguration(
    val authentication: DiscogsAuthentication = NO_AUTHENTICATION,
)

data class DiscogsAuthentication(
    val token: String?,
) {
    fun isAuthenticationPresent() = token != null

    companion object {
        val NO_AUTHENTICATION: DiscogsAuthentication = DiscogsAuthentication(null)
    }
}
