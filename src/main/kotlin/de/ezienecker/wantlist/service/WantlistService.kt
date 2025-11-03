package de.ezienecker.wantlist.service

import de.ezienecker.core.infrastructure.discogs.client.ApiError
import de.ezienecker.core.infrastructure.discogs.client.ApiException
import de.ezienecker.core.infrastructure.discogs.wantlist.Want
import de.ezienecker.core.infrastructure.discogs.wantlist.WantlistApiClient
import de.ezienecker.core.infrastructure.discogs.wantlist.WantsResponse
import de.ezienecker.wantlist.infrastructure.repository.WantlistCacheService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

private val logger = KotlinLogging.logger {}

class WantlistService(
    private val client: WantlistApiClient,
    private val cache: WantlistCacheService,
) {

    suspend fun getIdsFromWantlistReleasesByUser(username: String?) = username?.let { user ->
        listWantsByUser(user).fold(
            onSuccess = { wants -> wants.map { it.basicInformation.id }.toSet() },
            onFailure = { emptySet() }
        )
    } ?: emptySet()

    suspend fun listWantsByUser(
        username: String,
        sortBy: String = "",
        sortOrder: String = "",
    ): Result<List<Want>> {
        logger.info { "Fetching wantlist for user: [$username] with cache support." }

        return if (cache.hasValidCache(username)) {
            logger.info { "Using cached wantlist data for user: [$username]." }
            try {
                val cachedWants = cache.getCached(username)
                Result.success(cachedWants)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to retrieve cached wantlist for user: [$username]. Falling back to API." }
                fetchAndCacheWantlist(username, sortBy, sortOrder)
            }
        } else {
            logger.info { "No valid cache found for user: [$username]. Fetching from API." }
            fetchAndCacheWantlist(username, sortBy, sortOrder)
        }
    }

    private suspend fun fetchAndCacheWantlist(
        username: String,
        sortBy: String = "",
        sortOrder: String = "",
    ): Result<List<Want>> {
        logger.info { "Fetching wantlist from API for user: [$username]." }
        var hasNext: Boolean
        var page = 1
        val wants = mutableListOf<Want>()

        do {
            val response = client.listUsersWantList(username, page, 100, sortBy, sortOrder)

            when (response.status) {
                HttpStatusCode.OK -> {
                    logger.debug { "Successfully fetched wantlist from user: [$username]." }
                    val responseBody = response.body<WantsResponse>()
                    wants.addAll(responseBody.result)
                    page++
                    hasNext = responseBody.pagination.hasNext()
                }

                HttpStatusCode.Forbidden -> {
                    logger.warn { "Unauthorized access to wantlist for user: [$username]." }
                    return Result.failure(ApiException(ApiError.NoAccessToCollection))
                }

                else -> {
                    logger.warn { "Failed to fetch wantlist from user: [$username]. Status: [${response.status}]." }
                    return Result.failure(ApiException(ApiError.Unknown(Exception("Unexpected status: ${response.status}"))))
                }
            }
        } while (hasNext)

        cacheFetchedData(username, wants)

        return Result.success(wants)
    }

    private fun cacheFetchedData(username: String, wants: List<Want>) {
        try {
            cache.cache(username, wants)
            logger.info { "Successfully cached wantlist data for user: [$username]." }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to cache wantlist data for user: [$username]." }
        }
    }

    suspend fun refreshWantlistByUser(username: String): Result<List<Want>> {
        logger.info { "Force refreshing wantlist for user: [$username]." }
        cache.clearCache(username)
        return fetchAndCacheWantlist(username)
    }
}
