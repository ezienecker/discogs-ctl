package de.ezienecker.core.infrastructure.discogs.wantlist

import de.ezienecker.core.infrastructure.discogs.client.DiscogsClient
import de.ezienecker.core.infrastructure.discogs.client.auth.providers.DiscogsClientConfiguration
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

class WantlistApiClient(
    engine: HttpClientEngine = CIO.create(),
    configuration: DiscogsClientConfiguration
) : DiscogsClient(engine, configuration) {

    suspend fun listUsersWantList(username: String, page: Int, perPage: Int): HttpResponse {
        return client.get("https://api.discogs.com/users/$username/wants?page=$page&per_page=$perPage")
    }
}
