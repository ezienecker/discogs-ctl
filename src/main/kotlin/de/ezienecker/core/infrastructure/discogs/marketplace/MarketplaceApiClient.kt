package de.ezienecker.core.infrastructure.discogs.marketplace

import de.ezienecker.core.infrastructure.discogs.client.DiscogsClient
import de.ezienecker.core.infrastructure.discogs.client.auth.providers.DiscogsClientConfiguration
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

class MarketplaceApiClient(
    engine: HttpClientEngine = CIO.create(),
    configuration: DiscogsClientConfiguration,
) : DiscogsClient(engine, configuration) {

    suspend fun getListingsByReleaseId(releaseId: Long): HttpResponse {
        return client.get("https://www.discogs.com/sell/release/$releaseId")
    }
}
