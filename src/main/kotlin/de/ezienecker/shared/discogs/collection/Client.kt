package de.ezienecker.shared.discogs.collection

import co.touchlab.kermit.Logger
import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shared.discogs.shared.CollectionReleases
import de.ezienecker.shared.discogs.shared.emptyResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

private const val FOLDER_ALL = "0"

suspend fun DiscogsClient.listUsersCollection(username: String, page: Int, perPage: Int): CollectionReleases {
    Logger.i { "Fetching collection for user: [$username]." }
    val response = client.get("https://api.discogs.com/users/$username/collection/folders/$FOLDER_ALL/releases?page=$page&per_page=$perPage")

    return if (response.status == HttpStatusCode.OK) {
        Logger.d { "Successfully fetched collection for user: [$username], page: [$page], perPage: [$perPage]." }
        response.body<CollectionReleases>()
    } else {
        Logger.w { "Failed to fetch collection for user: [$username]. Status: [${response.status}]." }
        emptyResponse { pagination, _ -> CollectionReleases(pagination, emptyList()) }
    }
}
