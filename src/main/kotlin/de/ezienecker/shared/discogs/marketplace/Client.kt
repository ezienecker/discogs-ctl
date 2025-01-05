package de.ezienecker.shared.discogs.marketplace

import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shared.discogs.shared.Listings
import de.ezienecker.shared.discogs.shared.emptyListing
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

suspend fun DiscogsClient.listUsersInventory(username: String, page: Int, perPage: Int): Listings {
    val response = client.get("https://api.discogs.com/users/$username/inventory?page=$page&per_page=$perPage")

    if (response.status == HttpStatusCode.OK) {
        return response.body<Listings>()
    } else {
        return emptyListing()
    }
}
