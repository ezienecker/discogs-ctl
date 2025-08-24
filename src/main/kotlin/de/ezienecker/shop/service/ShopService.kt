package de.ezienecker.shop.service

import de.ezienecker.shared.database.cache.ShopCacheService
import de.ezienecker.shared.discogs.client.ApiError
import de.ezienecker.shared.discogs.client.ApiException
import de.ezienecker.shared.discogs.marketplace.Listing
import de.ezienecker.shared.discogs.marketplace.ShopApiClient
import de.ezienecker.shared.discogs.marketplace.ShopResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

private val logger = KotlinLogging.logger {}

class ShopService(
    private val client: ShopApiClient,
    private val cache: ShopCacheService,
) {

    suspend fun getIdsFromInventoryReleasesByUser(username: String?) = username?.let { user ->
        listInventoryByUser(user).fold(
            onSuccess = { inventory -> inventory.map { it.release.id }.toSet() },
            onFailure = { emptySet() }
        )
    } ?: emptySet()

    suspend fun listInventoryByUser(username: String): Result<List<Listing>> {
        logger.info { "Fetching inventory for user: [$username] with cache support." }
        
        return if (cache.hasValidCache(username)) {
            logger.info { "Using cached inventory data for user: [$username]." }
            try {
                val cachedListings = cache.getCached(username)
                Result.success(cachedListings)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to retrieve cached inventory for user: [$username]. Falling back to API." }
                fetchAndCacheInventory(username)
            }
        } else {
            logger.info { "No valid cache found for user: [$username]. Fetching from API." }
            fetchAndCacheInventory(username)
        }
    }

    private suspend fun fetchAndCacheInventory(username: String): Result<List<Listing>> {
        logger.info { "Fetching inventory from API for user: [$username]." }
        var hasNext: Boolean
        var page = 1
        val listings = mutableListOf<Listing>()

        do {
            val response = client.listUsersShop(username, page, 100)

            when (response.status) {
                HttpStatusCode.OK -> {
                    logger.debug { "Successfully fetched inventory from user: [$username]." }
                    val responseBody = response.body<ShopResponse>()
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

        cacheFetchedData(username, listings)

        return Result.success(listings)
    }

    private fun cacheFetchedData(username: String, listings: List<Listing>) {
        try {
            cache.cache(username, listings)
            logger.info { "Successfully cached inventory data for user: [$username]." }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to cache inventory data for user: [$username]." }
        }
    }

    suspend fun refreshInventoryByUser(username: String): Result<List<Listing>> {
        logger.info { "Force refreshing inventory for user: [$username]." }
        cache.clearCache(username)
        return fetchAndCacheInventory(username)
    }
}
