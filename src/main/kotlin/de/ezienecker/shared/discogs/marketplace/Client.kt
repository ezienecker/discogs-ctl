package de.ezienecker.shared.discogs.marketplace

import co.touchlab.kermit.Logger
import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shared.discogs.shared.Listings
import de.ezienecker.shared.discogs.shared.emptyResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

suspend fun DiscogsClient.listUsersInventory(username: String, page: Int, perPage: Int): Listings {
    Logger.i { "Fetching inventory for user: [$username]." }
    val response = client.get("https://api.discogs.com/users/$username/inventory?page=$page&per_page=$perPage")

    return if (response.status == HttpStatusCode.OK) {
        Logger.d { "Successfully fetched inventory for user: [$username], page: [$page], perPage: [$perPage]." }
        response.body<Listings>()
    } else {
        Logger.w { "Failed to fetch inventory for user: [$username]. Status: [${response.status}]." }
        emptyResponse { pagination, _ -> Listings(pagination, emptyList()) }
    }
}
