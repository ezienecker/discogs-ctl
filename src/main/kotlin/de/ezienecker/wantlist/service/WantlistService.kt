package de.ezienecker.de.ezienecker.wantlist.service

import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shared.discogs.shared.Wants
import de.ezienecker.shared.discogs.wantlist.Want
import de.ezienecker.shared.discogs.wantlist.listUsersWantList

class WantlistService(private val discogsClient: DiscogsClient) {

    suspend fun listInventoryByUser(username: String): List<Want> {

        var wants: Wants?
        var page = 1
        val wantlistItems = mutableListOf<Want>()

        do {
            wants = discogsClient.listUsersWantList(username, page, 100)
            wantlistItems.addAll(wants.result)
            page++

        } while (wants?.pagination?.hasNext() == true)

        return wantlistItems
    }
}
