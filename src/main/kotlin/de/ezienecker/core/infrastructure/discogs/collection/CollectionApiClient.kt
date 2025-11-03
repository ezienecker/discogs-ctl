package de.ezienecker.core.infrastructure.discogs.collection

import de.ezienecker.core.infrastructure.discogs.client.DiscogsClient
import de.ezienecker.core.infrastructure.discogs.client.auth.providers.DiscogsClientConfiguration
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse

private const val FOLDER_ALL = "0"

class CollectionApiClient(
    engine: HttpClientEngine = CIO.create(),
    configuration: DiscogsClientConfiguration,
) : DiscogsClient(engine, configuration) {

    suspend fun listUsersCollection(
        username: String,
        page: Int,
        perPage: Int,
        sortBy: String = "",
        sortOrder: String = "",
    ): HttpResponse {
        return client.get("https://api.discogs.com/users/$username/collection/folders/$FOLDER_ALL/releases") {
            parameter("page", page)
            parameter("per_page", perPage)
            parameter("sort", sortBy)
            parameter("sort_order", sortOrder)
        }
    }
}
