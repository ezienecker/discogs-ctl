package de.ezienecker.shared.discogs.wantlist

import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shared.discogs.shared.Wants
import de.ezienecker.shared.discogs.shared.emptyResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

private val logger = KotlinLogging.logger {}

suspend fun DiscogsClient.listUsersWantList(username: String, page: Int, perPage: Int): Wants {
    logger.info { "Fetching wants for user: [$username]." }
    val response = client.get("https://api.discogs.com/users/$username/wants?page=$page&per_page=$perPage")

    return if (response.status == HttpStatusCode.OK) {
        logger.debug { "Successfully fetched wants for user: [$username], page: [$page], perPage: [$perPage]." }
        response.body<Wants>()
    } else {
        logger.warn { "Failed to fetch wants for user: [$username]. Status: [${response.status}]." }
        emptyResponse { pagination, _ -> Wants(pagination, emptyList()) }
    }
}
