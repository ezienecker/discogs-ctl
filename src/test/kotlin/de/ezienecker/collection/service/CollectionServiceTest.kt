package de.ezienecker.collection.service

import de.ezienecker.collection.infrastructure.repository.CollectionCacheService
import de.ezienecker.core.infrastructure.discogs.client.ApiError
import de.ezienecker.core.infrastructure.discogs.client.ApiException
import de.ezienecker.core.infrastructure.discogs.collection.Artist
import de.ezienecker.core.infrastructure.discogs.collection.BasicInformation
import de.ezienecker.core.infrastructure.discogs.collection.CollectionApiClient
import de.ezienecker.core.infrastructure.discogs.collection.CollectionResponse
import de.ezienecker.core.infrastructure.discogs.collection.Format
import de.ezienecker.core.infrastructure.discogs.collection.Label
import de.ezienecker.core.infrastructure.discogs.collection.Release
import de.ezienecker.core.infrastructure.discogs.shared.Pagination
import de.ezienecker.core.infrastructure.discogs.shared.Urls
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest

class CollectionServiceTest : FunSpec({

    lateinit var mockApiClient: CollectionApiClient
    lateinit var mockCacheService: CollectionCacheService
    lateinit var mockHttpResponse: HttpResponse
    lateinit var collectionService: CollectionService

    val testUsername = "test-user"

    beforeEach {
        mockApiClient = mockk()
        mockCacheService = mockk()
        mockHttpResponse = mockk()
        collectionService = CollectionService(mockApiClient, mockCacheService)
    }

    context("Cache behavior of listCollectionByUser function") {
        test("should use cache when valid cache exists") {
            val cachedReleases = listOf(createTestRelease(id = 1))
            
            every { mockCacheService.hasValidCache(testUsername) } returns true
            every { mockCacheService.getCached(testUsername) } returns cachedReleases
            
            runTest {
                val result = collectionService.listCollectionByUser(testUsername)
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe cachedReleases
            }
            
            verify { mockCacheService.hasValidCache(testUsername) }
            verify { mockCacheService.getCached(testUsername) }
            coVerify(exactly = 0) { mockApiClient.listUsersCollection(any(), any(), any()) }
        }

        test("should call API when no valid cache exists") {
            val apiReleases = listOf(createTestRelease(id = 1))
            val collectionResponse = CollectionResponse(
                result = apiReleases,
                pagination = Pagination(
                    page = 1,
                    pages = 1,
                    perPage = 100,
                    items = 1,
                    urls = Urls(),
                )
            )
            
            every { mockCacheService.hasValidCache(testUsername) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<CollectionResponse>() } returns collectionResponse
            coEvery { mockApiClient.listUsersCollection(testUsername, 1, 100) } returns mockHttpResponse
            every { mockCacheService.cache(testUsername, apiReleases) } just Runs
            
            runTest {
                val result = collectionService.listCollectionByUser(testUsername)
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe apiReleases
            }
            
            verify { mockCacheService.hasValidCache(testUsername) }
            coVerify { mockApiClient.listUsersCollection(testUsername, 1, 100) }
            verify { mockCacheService.cache(testUsername, apiReleases) }
        }

        test("should fallback to API when cache retrieval fails") {
            val apiReleases = listOf(createTestRelease(id = 1))
            val collectionResponse = CollectionResponse(
                result = apiReleases,
                pagination = Pagination(
                    page = 1,
                    pages = 1,
                    perPage = 100,
                    items = 1,
                    urls = Urls(),
                )
            )
            
            every { mockCacheService.hasValidCache(testUsername) } returns true
            every { mockCacheService.getCached(testUsername) } throws RuntimeException("Cache error")
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<CollectionResponse>() } returns collectionResponse
            coEvery { mockApiClient.listUsersCollection(testUsername, 1, 100) } returns mockHttpResponse
            every { mockCacheService.cache(testUsername, apiReleases) } just Runs
            
            runTest {
                val result = collectionService.listCollectionByUser(testUsername)
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe apiReleases
            }
            
            verify { mockCacheService.hasValidCache(testUsername) }
            verify { mockCacheService.getCached(testUsername) }
            coVerify { mockApiClient.listUsersCollection(testUsername, 1, 100) }
            verify { mockCacheService.cache(testUsername, apiReleases) }
        }

        test("should call API again after 24 hours (cache expired)") {
            val apiReleases = listOf(createTestRelease(id = 1))
            val collectionResponse = CollectionResponse(
                result = apiReleases,
                pagination = Pagination(
                    page = 1,
                    pages = 1,
                    perPage = 100,
                    items = 1,
                    urls = Urls(),
                )
            )
            
            // First call - cache is valid
            every { mockCacheService.hasValidCache(testUsername) } returns true
            every { mockCacheService.getCached(testUsername) } returns apiReleases
            
            runTest {
                val firstResult = collectionService.listCollectionByUser(testUsername)
                firstResult.isSuccess shouldBe true
            }

            // Second call - cache is expired (24 hours passed)
            every { mockCacheService.hasValidCache(testUsername) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<CollectionResponse>() } returns collectionResponse
            coEvery { mockApiClient.listUsersCollection(testUsername, 1, 100) } returns mockHttpResponse
            every { mockCacheService.cache(testUsername, apiReleases) } just Runs
            
            runTest {
                val secondResult = collectionService.listCollectionByUser(testUsername)
                secondResult.isSuccess shouldBe true
            }

            // Verify first call used cache, second call used API
            verify(exactly = 2) { mockCacheService.hasValidCache(testUsername) }
            verify(exactly = 1) { mockCacheService.getCached(testUsername) }
            coVerify(exactly = 1) { mockApiClient.listUsersCollection(testUsername, 1, 100) }
            verify(exactly = 1) { mockCacheService.cache(testUsername, apiReleases) }
        }
    }

    context("API pagination") {
        test("should handle multiple pages of results") {
            val page1Releases = listOf(createTestRelease(id = 1))
            val page2Releases = listOf(createTestRelease(id = 2))
            val allReleases = page1Releases + page2Releases
            
            val page1Response = CollectionResponse(
                result = page1Releases,
                pagination = Pagination(
                    page = 1,
                    pages = 2,
                    perPage = 1,
                    items = 2,
                    urls = Urls(
                        last = "https://api.discogs.com/users/test-user/collection/folders/0/releases?page=42&per_page=1&sort=label&sort_order=asc",
                        next = "https://api.discogs.com/users/test-user/collection/folders/0/releases?page=2&per_page=1&sort=label&sort_order=asc",
                    ),
                )
            )
            
            val page2Response = CollectionResponse(
                result = page2Releases,
                pagination = Pagination(
                    page = 2,
                    pages = 2,
                    perPage = 1,
                    items = 2,
                    urls = Urls(),
                )
            )
            
            every { mockCacheService.hasValidCache(testUsername) } returns false
            
            val mockResponse1 = mockk<HttpResponse>()
            every { mockResponse1.status } returns HttpStatusCode.OK
            coEvery { mockResponse1.body<CollectionResponse>() } returns page1Response
            coEvery { mockApiClient.listUsersCollection(testUsername, 1, 100) } returns mockResponse1
            
            val mockResponse2 = mockk<HttpResponse>()
            every { mockResponse2.status } returns HttpStatusCode.OK
            coEvery { mockResponse2.body<CollectionResponse>() } returns page2Response
            coEvery { mockApiClient.listUsersCollection(testUsername, 2, 100) } returns mockResponse2
            
            every { mockCacheService.cache(testUsername, allReleases) } just Runs
            
            runTest {
                val result = collectionService.listCollectionByUser(testUsername)

                result.isSuccess shouldBe true
                result.getOrNull()?.size shouldBe 2
                result.getOrNull() shouldBe allReleases
            }

            coVerify { mockApiClient.listUsersCollection(testUsername, 1, 100) }
            coVerify { mockApiClient.listUsersCollection(testUsername, 2, 100) }
            verify { mockCacheService.cache(testUsername, allReleases) }
        }
    }

    context("API error handling") {
        test("should handle forbidden access") {
            every { mockCacheService.hasValidCache(testUsername) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.Forbidden
            coEvery { mockApiClient.listUsersCollection(testUsername, 1, 100) } returns mockHttpResponse
            
            runTest {
                val result = collectionService.listCollectionByUser(testUsername)

                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<ApiException>()
                val exception = result.exceptionOrNull() as ApiException
                exception.error shouldBe ApiError.NoAccessToCollection
            }
        }

        test("should handle not found") {
            every { mockCacheService.hasValidCache(testUsername) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.NotFound
            coEvery { mockApiClient.listUsersCollection(testUsername, 1, 100) } returns mockHttpResponse
            
            runTest {
                val result = collectionService.listCollectionByUser(testUsername)

                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<ApiException>()
                val exception = result.exceptionOrNull() as ApiException
                exception.error shouldBe ApiError.NotFound
            }
        }

        test("should handle server errors") {
            every { mockCacheService.hasValidCache(testUsername) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.InternalServerError
            coEvery { mockApiClient.listUsersCollection(testUsername, 1, 100) } returns mockHttpResponse
            
            runTest {
                val result = collectionService.listCollectionByUser(testUsername)

                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<ApiException>()
                val exception = result.exceptionOrNull() as ApiException
                exception.error.shouldBeInstanceOf<ApiError.Server>()
            }
        }

        test("should handle unknown status codes") {
            every { mockCacheService.hasValidCache(testUsername) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.PaymentRequired
            coEvery { mockApiClient.listUsersCollection(testUsername, 1, 100) } returns mockHttpResponse
            
            runTest {
                val result = collectionService.listCollectionByUser(testUsername)

                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<ApiException>()
                val exception = result.exceptionOrNull() as ApiException
                exception.error.shouldBeInstanceOf<ApiError.Unknown>()
            }
        }
    }

    context("refreshCollectionByUser") {
        test("should clear cache and fetch from API") {
            val apiReleases = listOf(createTestRelease(id = 1))
            val collectionResponse = CollectionResponse(
                result = apiReleases,
                pagination = Pagination(
                    page = 1,
                    pages = 1,
                    perPage = 100,
                    items = 1,
                    urls = Urls(),
                )
            )
            
            every { mockCacheService.clearCache(testUsername) } just Runs
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<CollectionResponse>() } returns collectionResponse
            coEvery { mockApiClient.listUsersCollection(testUsername, 1, 100) } returns mockHttpResponse
            every { mockCacheService.cache(testUsername, apiReleases) } just Runs
            
            runTest {
                val result = collectionService.refreshCollectionByUser(testUsername)

                result.isSuccess shouldBe true
                result.getOrNull() shouldBe apiReleases
            }

            verify { mockCacheService.clearCache(testUsername) }
            coVerify { mockApiClient.listUsersCollection(testUsername, 1, 100) }
            verify { mockCacheService.cache(testUsername, apiReleases) }
        }
    }

    context("Cache sorting behavior") {
        test("should sort cached collection by title ascending") {
            val cachedReleases = listOf(
                createTestRelease(id = 1, title = "Zulu"),
                createTestRelease(id = 2, title = "Alpha")
            )

            every { mockCacheService.hasValidCache(testUsername) } returns true
            every { mockCacheService.getCached(testUsername) } returns cachedReleases

            runTest {

                val result = collectionService.listCollectionByUser(testUsername, "title", "asc")

                result.isSuccess shouldBe true
                result.getOrNull()!!.map { it.basicInformation.title } shouldBe listOf("Alpha", "Zulu")
            }

            verify { mockCacheService.hasValidCache(testUsername) }
            verify { mockCacheService.getCached(testUsername) }
            coVerify(exactly = 0) { mockApiClient.listUsersCollection(any(), any(), any(), any(), any()) }
        }

        test("should sort cached collection by artist descending") {
            val cachedReleases = listOf(
                createTestRelease(id = 1, artistName = "Beta Band"),
                createTestRelease(id = 2, artistName = "Alpha Crew")
            )

            every { mockCacheService.hasValidCache(testUsername) } returns true
            every { mockCacheService.getCached(testUsername) } returns cachedReleases

            runTest {
                val result = collectionService.listCollectionByUser(testUsername, "artist", "desc")

                result.isSuccess shouldBe true
                result.getOrNull()!!.map { release ->
                    release.basicInformation.artists.first().name
                } shouldBe listOf("Beta Band", "Alpha Crew")
            }

            verify { mockCacheService.hasValidCache(testUsername) }
            verify { mockCacheService.getCached(testUsername) }
            coVerify(exactly = 0) { mockApiClient.listUsersCollection(any(), any(), any(), any(), any()) }
        }
    }
})

private fun createTestRelease(
    id: Long = 1L,
    instanceId: Long = 100L,
    rating: Int = 5,
    title: String = "Test Album $id",
    artistName: String = "Test Artist"
): Release {
    return Release(
        id = id,
        instanceId = instanceId,
        dateAdded = "2023-01-01T12:00:00",
        rating = rating,
        basicInformation = BasicInformation(
            id = id,
            masterId = id + 1000,
            masterUrl = "https://api.discogs.com/masters/${id + 1000}",
            resourceUrl = "https://api.discogs.com/releases/$id",
            thumb = "https://example.com/thumb.jpg",
            title = title,
            year = 2023,
            formats = listOf(
                Format(
                    name = "Vinyl",
                    qty = "1",
                    text = "12\"",
                    descriptions = listOf("LP", "Album")
                )
            ),
            labels = listOf(
                Label(
                    id = 1L,
                    name = "Test Label",
                    catNo = "TEST001",
                    entityType = "1",
                    entityTypeName = "Label",
                    resourceUrl = "https://api.discogs.com/labels/1"
                )
            ),
            artists = listOf(
                Artist(
                    id = 1L,
                    name = artistName,
                    anv = "",
                    join = "",
                    role = "",
                    tracks = "",
                    resourceUrl = "https://api.discogs.com/artists/1"
                )
            ),
            genres = listOf("Hip Hop", "Rap"),
            styles = listOf("Boombap")
        )
    )
}
