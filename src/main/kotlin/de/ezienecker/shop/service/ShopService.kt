package de.ezienecker.shop.service

import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shared.discogs.marketplace.Listing
import de.ezienecker.shared.discogs.marketplace.listUsersInventory
import de.ezienecker.shared.discogs.shared.Listings

class ShopService(private val discogsClient: DiscogsClient) {

    suspend fun listInventoryByUser(username: String): List<Listing> {

        var listings: Listings?
        var page = 1
        val listingItems = mutableListOf<Listing>()

        do {
            listings = discogsClient.listUsersInventory(username, page, 100)
            listingItems.addAll(listings.result)
            page++

        } while (listings?.pagination?.hasNext() == true)

        return listingItems
    }
}
