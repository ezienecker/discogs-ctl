package de.ezienecker.collection.service

import io.kotest.core.spec.style.StringSpec

class CollectionServiceTest : StringSpec({

    /*"should return empty collection when no releases are found".config(enabled = false) {
        val discogsClient = mockk<DiscogsClient>()
        val collectionService = CollectionService(discogsClient)
        val username = "test-user"

        coEvery { discogsClient.listUsersCollection(username, any(), any()) } returns ApiResult.Success(
            CollectionReleases(
                result = emptyList(),
                pagination = Pagination(
                    page = 1,
                    pages = 1,
                    perPage = 50,
                    items = 0
                )
            )
        )

        val releases = collectionService.listCollectionByUser(username)

        releases shouldBe emptyList()
    }*/
})
