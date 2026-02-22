package de.ezienecker.collection.service

import de.ezienecker.collection.infrastructure.repository.CollectionCacheService
import de.ezienecker.core.infrastructure.discogs.client.ApiError
import de.ezienecker.core.infrastructure.discogs.client.ApiException
import de.ezienecker.core.infrastructure.discogs.collection.CollectionApiClient
import de.ezienecker.core.infrastructure.discogs.collection.CollectionResponse
import de.ezienecker.core.infrastructure.discogs.collection.Release
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

private val logger = KotlinLogging.logger {}

class CollectionService(
    private val client: CollectionApiClient,
    private val cache: CollectionCacheService,
) {

    suspend fun listCollectionByUser(
        username: String,
        sortBy: String = "",
        sortOrder: String = "",
        forceUpdate: Boolean = false,
    ): Result<List<Release>> {
        logger.info { "Fetching collection for user: [$username]." }
        logger.debug { "Sort by: [$sortBy], Sort order: [$sortOrder]." }

        logger.debug { "Checking if user: [$username] has valid cache for the collection." }
        val hasValidCache = cache.hasValidCache(username)
        return if (hasValidCache && !forceUpdate) {
            logger.info { "Using cached collection data for user: [$username]." }
            try {
                val cachedReleases = cache.getCached(username).run {
                    sortReleases(this, sortBy, sortOrder)
                }
                Result.success(cachedReleases)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to retrieve cached collection for user: [$username]. Falling back to API." }
                fetchAndCacheCollection(username, sortBy, sortOrder)
            }
        } else {
            logger.info { "Fetching collection from API for user: [$username]." }
            logger.debug { "Either no valid cache [$hasValidCache] is found or a force update [$forceUpdate] is requested." }
            fetchAndCacheCollection(username, sortBy, sortOrder)
        }
    }

    private suspend fun fetchAndCacheCollection(
        username: String,
        sortBy: String = "",
        sortOrder: String = "",
    ): Result<List<Release>> {
        logger.info { "Fetching collection from API for user: [$username]." }
        var hasNext: Boolean
        var page = 1
        val releases = mutableListOf<Release>()

        do {
            val response = client.listUsersCollection(username, page, 100, sortBy, sortOrder)

            when (response.status) {
                HttpStatusCode.OK -> {
                    logger.debug { "Successfully fetched collection from user: [$username]." }
                    val responseBody = response.body<CollectionResponse>()
                    releases.addAll(responseBody.result)
                    page++
                    hasNext = responseBody.pagination.hasNext()
                }

                HttpStatusCode.Forbidden -> {
                    logger.warn { "Unauthorized access to collection for user: [$username]." }
                    return Result.failure(ApiException(ApiError.NoAccessToCollection))
                }

                HttpStatusCode.NotFound -> {
                    logger.warn { "Unauthorized access to collection for user: [$username]." }
                    return Result.failure(ApiException(ApiError.NotFound))
                }

                in HttpStatusCode.InternalServerError..HttpStatusCode.GatewayTimeout -> {
                    logger.warn { "Service unavailable while fetching collection for user: [$username]." }
                    return Result.failure(ApiException(ApiError.Server(response.status.value)))
                }

                else -> {
                    logger.warn { "Failed to fetch collection from user: [$username]. Status: [${response.status}]." }
                    return Result.failure(ApiException(ApiError.Unknown(Exception("Unexpected status: ${response.status}"))))
                }
            }
        } while (hasNext)

        cacheFetchedData(username, releases)

        return Result.success(releases)
    }

    private fun cacheFetchedData(username: String, releases: List<Release>) {
        try {
            cache.cache(username, releases)
            logger.info { "Successfully cached collection data for user: [$username]." }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to cache collection data for user: [$username]." }
        }
    }

    /**
     * Force refresh collection data from API and update cache
     */
    suspend fun refreshCollectionByUser(username: String): Result<List<Release>> {
        logger.info { "Force refreshing collection for user: [$username]." }
        cache.clearCache(username)
        return fetchAndCacheCollection(username)
    }

    private fun sortReleases(releases: List<Release>, sortBy: String, sortOrder: String): List<Release> {
        if (sortBy.isBlank()) {
            return releases
        }

        val comparator = when (sortBy.lowercase()) {
            "title" -> compareBy<Release> { it.basicInformation.title.lowercase() }
            "artist" -> compareBy { release ->
                release.basicInformation.artists.firstOrNull()?.name?.lowercase() ?: ""
            }
            else -> {
                logger.warn { "Unknown sort field: [$sortBy]. Returning unsorted list." }
                return releases
            }
        }

        return when (sortOrder.lowercase()) {
            "desc" -> releases.sortedWith(comparator.reversed())
            else -> releases.sortedWith(comparator)
        }.also {
            logger.debug { "Sorted ${releases.size} releases by [$sortBy] in [$sortOrder] order." }
        }
    }
}
