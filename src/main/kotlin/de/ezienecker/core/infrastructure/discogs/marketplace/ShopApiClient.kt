package de.ezienecker.core.infrastructure.discogs.marketplace

import de.ezienecker.core.infrastructure.discogs.client.DiscogsClient
import de.ezienecker.core.infrastructure.discogs.client.auth.providers.DiscogsClientConfiguration
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse

class ShopApiClient(
    engine: HttpClientEngine = CIO.create(),
    configuration: DiscogsClientConfiguration
) : DiscogsClient(engine, configuration) {

    suspend fun listUsersShop(
        username: String,
        page: Int,
        perPage: Int,
        sortBy: String = "",
        sortOrder: String = "",
    ): HttpResponse {
        return client.get("https://api.discogs.com/users/$username/inventory") {
            parameter("page", page)
            parameter("per_page", perPage)
            parameter("sort", sortBy)
            parameter("sort_order", sortOrder)
        }
    }
}
