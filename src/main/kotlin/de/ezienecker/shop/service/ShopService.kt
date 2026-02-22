package de.ezienecker.shop.service

import de.ezienecker.core.infrastructure.discogs.client.ApiError
import de.ezienecker.core.infrastructure.discogs.client.ApiException
import de.ezienecker.core.infrastructure.discogs.shop.Listing
import de.ezienecker.core.infrastructure.discogs.shop.ShopApiClient
import de.ezienecker.core.infrastructure.discogs.shop.ShopResponse
import de.ezienecker.shop.infrastructure.repository.ShopCacheService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

private val logger = KotlinLogging.logger {}

class ShopService(
    private val client: ShopApiClient,
    private val cache: ShopCacheService,
) {

    suspend fun getIdsFromInventoryReleasesByUser(
        username: String?,
        forceUpdate: Boolean = false,
    ) = username?.let { user ->
        listInventoryByUser(user, forceUpdate = forceUpdate).fold(
            onSuccess = { inventory -> inventory.map { it.release.id }.toSet() },
            onFailure = { emptySet() }
        )
    } ?: emptySet()

    suspend fun listInventoryByUser(
        username: String,
        sortBy: String = "",
        sortOrder: String = "",
        forceUpdate: Boolean = false,
    ): Result<List<Listing>> {
        logger.info { "Fetching inventory for user: [$username]." }
        logger.debug { "Sort by: [$sortBy], Sort order: [$sortOrder]." }

        logger.debug { "Checking if user: [$username] has valid cache for the shop inventory." }
        val hasValidCache = cache.hasValidCache(username)
        return if (hasValidCache && !forceUpdate) {
            logger.info { "Using cached inventory data for user: [$username]." }
            try {
                val cachedListings = cache.getCached(username).run {
                    sortListings(this, sortBy, sortOrder)
                }
                Result.success(cachedListings)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to retrieve cached inventory for user: [$username]. Falling back to API." }
                fetchAndCacheInventory(username, sortBy, sortOrder)
            }
        } else {
            logger.info { "Fetching collection from API for user: [$username]." }
            logger.debug { "Either no valid cache [$hasValidCache] is found or a force update [$forceUpdate] is requested." }
            fetchAndCacheInventory(username, sortBy, sortOrder)
        }
    }

    suspend fun refreshInventoryByUser(username: String): Result<List<Listing>> {
        logger.info { "Force refreshing inventory for user: [$username]." }
        cache.clearCache(username)
        return fetchAndCacheInventory(username)
    }

    private suspend fun fetchAndCacheInventory(
        username: String,
        sortBy: String = "",
        sortOrder: String = "",
    ): Result<List<Listing>> {
        logger.info { "Fetching inventory from API for user: [$username]." }
        var hasNext: Boolean
        var page = 1
        val listings = mutableListOf<Listing>()

        do {
            val response = client.listUsersShop(username, page, 100, sortBy, sortOrder)

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

    private fun sortListings(listings: List<Listing>, sortBy: String, sortOrder: String): List<Listing> {
        if (sortBy.isBlank()) {
            return listings
        }

        val comparator = when (sortBy.lowercase()) {
            "title" -> compareBy<Listing> { it.release.title.value.lowercase() }
            "artist" -> compareBy { it.release.artist.value.lowercase() }
            else -> {
                logger.warn { "Unknown sort field: [$sortBy]. Returning unsorted list." }
                return listings
            }
        }

        return when (sortOrder.lowercase()) {
            "desc" -> listings.sortedWith(comparator.reversed())
            else -> listings.sortedWith(comparator)
        }.also {
            logger.debug { "Sorted ${listings.size} releases by [$sortBy] in [$sortOrder] order." }
        }
    }
}
