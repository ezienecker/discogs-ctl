package de.ezienecker.shop.service

import de.ezienecker.shared.discogs.client.ApiError
import de.ezienecker.shared.discogs.client.ApiException
import de.ezienecker.shared.discogs.marketplace.InventoryApiClient
import de.ezienecker.shared.discogs.marketplace.Listing
import de.ezienecker.shared.discogs.shared.Listings
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

private val logger = KotlinLogging.logger {}

class InventoryService(private val client: InventoryApiClient) {

    suspend fun getIdsFromInventoryReleasesByUser(username: String?) = username?.let { user ->
        listInventoryByUser(user).fold(
            onSuccess = { inventory -> inventory.map { it.release.id }.toSet() },
            onFailure = { emptySet() }
        )
    } ?: emptySet()

    suspend fun listInventoryByUser(username: String): Result<List<Listing>> {
        logger.info { "Fetching inventory from user: [$username]." }
        var hasNext: Boolean
        var page = 1
        val listings = mutableListOf<Listing>()

        do {
            val response = client.listUsersInventory(username, page, 100)

            when (response.status) {
                HttpStatusCode.OK -> {
                    logger.debug { "Successfully fetched inventory from user: [$username]." }
                    val responseBody = response.body<Listings>()
                    listings.addAll(responseBody.result)
                    page++
                    hasNext = responseBody.pagination.hasNext()
                }

                else -> {
                    logger.warn { "Failed to fetch inventory from user: [$username]. Status: [${response.status}]." }
                    return Result.failure(ApiException(ApiError.Unknown(Exception("Unexpected status: ${response.status}"))))
                }
            }
        } while (hasNext)

        return Result.success(listings)
    }
}
