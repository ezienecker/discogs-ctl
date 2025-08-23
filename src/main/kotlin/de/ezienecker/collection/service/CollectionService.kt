package de.ezienecker.collection.service

import de.ezienecker.shared.database.cache.CollectionCacheService
import de.ezienecker.shared.discogs.client.ApiError
import de.ezienecker.shared.discogs.client.ApiException
import de.ezienecker.shared.discogs.collection.CollectionApiClient
import de.ezienecker.shared.discogs.collection.CollectionResponse
import de.ezienecker.shared.discogs.collection.Release
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

private val logger = KotlinLogging.logger {}

class CollectionService(
    private val client: CollectionApiClient,
    private val cache: CollectionCacheService,
) {

    suspend fun listCollectionByUser(username: String): Result<List<Release>> {
        logger.info { "Fetching collection for user: [$username] with cache support." }

        return if (cache.hasValidCache(username)) {
            logger.info { "Using cached collection data for user: [$username]." }
            try {
                val cachedReleases = cache.getCached(username)
                Result.success(cachedReleases)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to retrieve cached collection for user: [$username]. Falling back to API." }
                fetchAndCacheCollection(username)
            }
        } else {
            logger.info { "No valid cache found for user: [$username]. Fetching from API." }
            fetchAndCacheCollection(username)
        }
    }

    private suspend fun fetchAndCacheCollection(username: String): Result<List<Release>> {
        logger.info { "Fetching collection from API for user: [$username]." }
        var hasNext: Boolean
        var page = 1
        val releases = mutableListOf<Release>()

        do {
            val response = client.listUsersCollection(username, page, 100)

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
}
