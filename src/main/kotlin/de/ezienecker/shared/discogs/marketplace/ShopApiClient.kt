package de.ezienecker.shared.discogs.marketplace

import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shared.discogs.client.auth.providers.DiscogsClientConfiguration
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

class ShopApiClient(
    engine: HttpClientEngine = CIO.create(),
    configuration: DiscogsClientConfiguration
) : DiscogsClient(engine, configuration) {

    suspend fun listUsersShop(username: String, page: Int, perPage: Int): HttpResponse {
        return client.get("https://api.discogs.com/users/$username/inventory?page=$page&per_page=$perPage")
    }
}
