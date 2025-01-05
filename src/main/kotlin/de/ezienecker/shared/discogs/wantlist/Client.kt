package de.ezienecker.shared.discogs.wantlist

import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shared.discogs.shared.Wants
import de.ezienecker.shared.discogs.shared.emptyWants
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

suspend fun DiscogsClient.listUsersWantList(username: String, page: Int, perPage: Int): Wants {
    val response = client.get("https://api.discogs.com/users/$username/wants?page=$page&per_page=$perPage")

    if (response.status == HttpStatusCode.OK) {
        return response.body<Wants>()
    } else {
        return emptyWants()
    }
}
