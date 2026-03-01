package de.ezienecker.wantlist.service

import de.ezienecker.core.batch.BatchProcessor
import de.ezienecker.core.infrastructure.discogs.client.ApiError
import de.ezienecker.core.infrastructure.discogs.client.ApiException
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceApiClient
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceListing
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceSeller
import de.ezienecker.wantlist.infrastructure.repository.MarketplaceCacheService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

private val logger = KotlinLogging.logger {}

class MarketplaceService(
    private val client: MarketplaceApiClient,
    private val marketplaceListingsTransformService: MarketplaceListingTransformService,
    private val cache: MarketplaceCacheService,
    private val batchProcessor: BatchProcessor<Long, List<MarketplaceListing>>,
) {

    suspend fun getMarketplaceListingsByReleaseIds(
        releaseIds: List<Long>,
        forceUpdate: Boolean = false,
    ): Map<MarketplaceSeller, List<MarketplaceListing>> {
        logger.info { "Fetching marketplace listings for [${releaseIds.size}] releases from wantlist." }

        val (releaseIdsToFetch, cachedReleaseIds) = releaseIds.run {
            partitionReleaseIds(releaseIds, forceUpdate)
        }

        val fetchedListings = fetchListingsInBatch(releaseIdsToFetch).also {
            logger.info { "Fetched [${it.size}] marketplace listings for [${releaseIdsToFetch.size}] releases." }
        }

        val cachedListings = cache.getAllCached(cachedReleaseIds).also {
            logger.info { "Retrieved [${it.size}] cached marketplace listings for [${cachedReleaseIds.size}] releases." }
        }

        return (fetchedListings + cachedListings)
            .groupBy { it.seller }
            .also {
                logger.info { "Grouped marketplace listings by seller. Total sellers: [${it.size}]." }
            }
    }

    private fun partitionReleaseIds(
        releaseIds: List<Long>,
        forceUpdate: Boolean,
    ): Pair<List<Long>, List<Long>> = if (forceUpdate) {
        logger.info { "Force update is requested. Ignoring cache and fetching marketplace listings for all [${releaseIds.size}] release IDs." }
        releaseIds to emptyList()
    } else {
        logger.info { "Checking cache for marketplace listings of [${releaseIds.size}] release IDs." }
        val releaseIdsToFetch = cache.findByReleaseIdNotIn(releaseIds)
        releaseIdsToFetch to releaseIds.filterNot { it in releaseIdsToFetch }
    }

    private suspend fun fetchListingsInBatch(releaseIds: List<Long>): List<MarketplaceListing> {
        logger.info { "Fetching marketplace listings in batch for [${releaseIds.size}] release IDs." }

        return batchProcessor.processParallelBatch(
            items = releaseIds,
            batchSize = 20,
            concurrency = 5
        ) { batchOfReleaseIds ->
            logger.debug { "Processing batch of ${batchOfReleaseIds.size} release IDs." }
            batchOfReleaseIds.flatMap { releaseId ->
                fetchAndCacheInventory(releaseId).getOrElse { emptyList() }
            }
        }.flatten()
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
            return Result.failure(
                ApiException(
                    ApiError.Unknown(Exception("There is an client error when fetching marketplace listings for release with ID: [$releaseId]. Status: [${response.status}]."))
                )
            )
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
