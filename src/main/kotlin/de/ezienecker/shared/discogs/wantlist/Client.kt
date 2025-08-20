package de.ezienecker.shared.discogs.wantlist

import co.touchlab.kermit.Logger
import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shared.discogs.shared.Wants
import de.ezienecker.shared.discogs.shared.emptyResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

suspend fun DiscogsClient.listUsersWantList(username: String, page: Int, perPage: Int): Wants {
    Logger.i { "Fetching wants for user: [$username]." }
    val response = client.get("https://api.discogs.com/users/$username/wants?page=$page&per_page=$perPage")

    return if (response.status == HttpStatusCode.OK) {
        Logger.d { "Successfully fetched wants for user: [$username], page: [$page], perPage: [$perPage]." }
        response.body<Wants>()
    } else {
        Logger.w { "Failed to fetch wants for user: [$username]. Status: [${response.status}]." }
        emptyResponse { pagination, _ -> Wants(pagination, emptyList()) }
    }
}
