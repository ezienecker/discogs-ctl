package de.ezienecker.wantlist.service

import de.ezienecker.shared.discogs.client.ApiError
import de.ezienecker.shared.discogs.client.ApiException
import de.ezienecker.shared.discogs.shared.Wants
import de.ezienecker.shared.discogs.wantlist.Want
import de.ezienecker.shared.discogs.wantlist.WantlistApiClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

private val logger = KotlinLogging.logger {}

class WantlistService(private val client: WantlistApiClient) {

    suspend fun getIdsFromWantlistReleasesByUser(username: String?) = username?.let { user ->
        listWantsByUser(user).fold(
            onSuccess = { wants -> wants.map { it.basicInformation.id }.toSet() },
            onFailure = { emptySet() }
        )
    } ?: emptySet()

    suspend fun listWantsByUser(username: String): Result<List<Want>> {
        logger.info { "Fetching wantlist from user: [$username]." }
        var hasNext: Boolean
        var page = 1
        val wants = mutableListOf<Want>()

        do {
            val response = client.listUsersWantList(username, page, 100)

            when (response.status) {
                HttpStatusCode.OK -> {
                    logger.debug { "Successfully fetched wantlist from user: [$username]." }
                    val responseBody = response.body<Wants>()
                    wants.addAll(responseBody.result)
                    page++
                    hasNext = responseBody.pagination.hasNext()
                }

                HttpStatusCode.Forbidden -> {
                    logger.warn { "Unauthorized access to collection for user: [$username]." }
                    return Result.failure(ApiException(ApiError.NoAccessToCollection))
                }

                else -> {
                    logger.warn { "Failed to fetch inventory from user: [$username]. Status: [${response.status}]." }
                    return Result.failure(ApiException(ApiError.Unknown(Exception("Unexpected status: ${response.status}"))))
                }
            }
        } while (hasNext)

        return Result.success(wants)
    }
}
