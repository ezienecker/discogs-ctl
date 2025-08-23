package de.ezienecker.shared.discogs.collection

import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shared.discogs.client.auth.providers.DiscogsClientConfiguration
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

private const val FOLDER_ALL = "0"

class CollectionApiClient(
    engine: HttpClientEngine = CIO.create(),
    configuration: DiscogsClientConfiguration,
) : DiscogsClient(engine, configuration) {

    /**
     * Fetches the collection of a user from Discogs.
     *
     * @param username The username of the user whose collection is to be fetched.
     * @param page The page number to fetch.
     * @param perPage The number of items per page.
     * @return An [HttpResponse] containing the user's collection.
     */
    suspend fun listUsersCollection(
        username: String,
        page: Int,
        perPage: Int
    ): HttpResponse {
        return client.get("https://api.discogs.com/users/$username/collection/folders/$FOLDER_ALL/releases?page=$page&per_page=$perPage")
    }
}
