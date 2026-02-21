package de.ezienecker.wantlist.service

import de.ezienecker.core.infrastructure.discogs.client.ApiError
import de.ezienecker.core.infrastructure.discogs.client.ApiException
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceApiClient
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceListing
import de.ezienecker.wantlist.infrastructure.repository.MarketplaceCacheService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

private val logger = KotlinLogging.logger {}

class MarketplaceService(
    private val client: MarketplaceApiClient,
    private val marketplaceListingsTransformService: MarketplaceListingTransformService,
    private val cache: MarketplaceCacheService,
) {

    suspend fun getListingsByReleaseId(releaseId: Long): Result<List<MarketplaceListing>> {
        logger.info { "Fetching marketplace listings for release with ID: [$releaseId]." }

        logger.debug { "Checking if release with ID: [$releaseId] has valid cache entry." }
        return if (cache.hasValidCache(releaseId)) {
            logger.info { "Using cached marketplace listings data for release with ID: [$releaseId]." }
            try {
                val cachedMarketplaceListings = cache.getCached(releaseId)
                Result.success(cachedMarketplaceListings)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to retrieve cached marketplace listings for release with ID: [$releaseId]. Falling back to API." }
                fetchAndCacheInventory(releaseId)
            }
        } else {
            logger.info { "No valid cache found for release with ID: [$releaseId]. Fetching from API." }
            fetchAndCacheInventory(releaseId)
        }
    }

    private suspend fun fetchAndCacheInventory(releaseId: Long): Result<List<MarketplaceListing>> {
        logger.info { "Fetching marketplace listings from API for release with ID: [$releaseId]." }

        val response = client.getListingsByReleaseId(releaseId)
        if (response.status == HttpStatusCode.OK) {
            logger.debug { "Successfully fetched marketplace listings for release with ID: [$releaseId]." }
            val marketplaceListings = marketplaceListingsTransformService.transformListings(
                releaseId = releaseId,
                marketplaceListingsHtml = response.body<String>(),
            )

            cacheFetchedData(releaseId, marketplaceListings)
            return Result.success(marketplaceListings)
        } else if (response.status.value in 400..499) {
            val body = response.body<String>()
            logger.warn { "Failed to fetch marketplace listings for release ID: [$releaseId]. Status: [${response.status}]." }
            logger.debug { "Message: [$body]" }
            return Result.failure(ApiException(
                ApiError.Unknown(Exception("There is an client error when fetching marketplace listings for release with ID: [$releaseId]. Status: [${response.status}]."))
            ))
        } else {
            logger.warn { "Failed to fetch marketplace listings for release ID: [$releaseId]. Status: [${response.status}]." }
            return Result.failure(ApiException(ApiError.Unknown(Exception("Unexpected status: ${response.status}"))))
        }
    }

    private fun cacheFetchedData(releaseId: Long, marketplaceListings: List<MarketplaceListing>) {
        try {
            cache.cache(releaseId, marketplaceListings)
            logger.info { "Successfully cached marketplace listings for release with ID: [$releaseId]." }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to cache marketplace listings for release ID: [$releaseId]." }
        }
    }
}
