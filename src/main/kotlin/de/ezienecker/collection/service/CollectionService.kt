package de.ezienecker.collection.service

import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shared.discogs.collection.Release
import de.ezienecker.shared.discogs.collection.listUsersCollection
import de.ezienecker.shared.discogs.shared.CollectionReleases

class CollectionService(private val discogsClient: DiscogsClient) {

    suspend fun listCollectionByUser(username: String): List<Release> {
        var collectionReleases: CollectionReleases?
        var page = 1
        val releases = mutableListOf<Release>()

        do {
            collectionReleases = discogsClient.listUsersCollection(username, page, 100)
            releases.addAll(collectionReleases.result)
            page++

        } while (collectionReleases?.pagination?.hasNext() == true)

        return releases
    }
}