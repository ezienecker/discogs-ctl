package de.ezienecker.shared.discogs.marketplace

import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shared.discogs.shared.Listings
import de.ezienecker.shared.discogs.shared.emptyResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

private val logger = KotlinLogging.logger {}

suspend fun DiscogsClient.listUsersInventory(username: String, page: Int, perPage: Int): Listings {
    logger.info { "Fetching inventory for user: [$username]." }
    val response = client.get("https://api.discogs.com/users/$username/inventory?page=$page&per_page=$perPage")

    return if (response.status == HttpStatusCode.OK) {
        logger.debug { "Successfully fetched inventory for user: [$username], page: [$page], perPage: [$perPage]." }
        response.body<Listings>()
    } else {
        logger.warn { "Failed to fetch inventory for user: [$username]. Status: [${response.status}]." }
        emptyResponse { pagination, _ -> Listings(pagination, emptyList()) }
    }
}
