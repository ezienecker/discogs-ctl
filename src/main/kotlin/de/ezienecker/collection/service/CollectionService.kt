package de.ezienecker.collection.service

import de.ezienecker.shared.discogs.client.ApiError
import de.ezienecker.shared.discogs.client.ApiException
import de.ezienecker.shared.discogs.collection.CollectionApiClient
import de.ezienecker.shared.discogs.collection.Release
import de.ezienecker.shared.discogs.shared.CollectionReleases
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

private val logger = KotlinLogging.logger {}

class CollectionService(private val client: CollectionApiClient) {

    suspend fun listCollectionByUser(username: String): Result<List<Release>> {
        logger.info { "Fetching collection from user: [$username]." }
        var hasNext: Boolean
        var page = 1
        val releases = mutableListOf<Release>()

        do {
            val response = client.listUsersCollection(username, page, 100)

            when (response.status) {
                HttpStatusCode.OK -> {
                    logger.debug { "Successfully fetched collection from user: [$username]." }

                    val responseBody = response.body<CollectionReleases>()
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

        return Result.success(releases)
    }
}
