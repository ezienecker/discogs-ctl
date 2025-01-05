package de.ezienecker.shared.discogs.client.auth.providers

data class DiscogsClientConfiguration(
    val authentication: DiscogsAuthentication,
)

data class DiscogsAuthentication(
    val token: String?,
) {
    fun isAuthenticationPresent() = token != null
}
