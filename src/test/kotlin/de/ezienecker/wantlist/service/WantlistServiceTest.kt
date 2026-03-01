package de.ezienecker.wantlist.service

import de.ezienecker.core.infrastructure.discogs.client.ApiError
import de.ezienecker.core.infrastructure.discogs.client.ApiException
import de.ezienecker.core.infrastructure.discogs.shared.Pagination
import de.ezienecker.core.infrastructure.discogs.shared.Urls
import de.ezienecker.core.infrastructure.discogs.wantlist.Artist
import de.ezienecker.core.infrastructure.discogs.wantlist.BasicInformation
import de.ezienecker.core.infrastructure.discogs.wantlist.Format
import de.ezienecker.core.infrastructure.discogs.wantlist.Label
import de.ezienecker.core.infrastructure.discogs.wantlist.Url
import de.ezienecker.core.infrastructure.discogs.wantlist.Want
import de.ezienecker.core.infrastructure.discogs.wantlist.WantlistApiClient
import de.ezienecker.core.infrastructure.discogs.wantlist.WantsResponse
import de.ezienecker.wantlist.infrastructure.repository.WantlistCacheService
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

class WantlistServiceTest : FunSpec({

    lateinit var mockApiClient: WantlistApiClient
    lateinit var mockCacheService: WantlistCacheService
    lateinit var mockHttpResponse: HttpResponse
    lateinit var wantlistService: WantlistService

    val testUsername = "test-user"

    beforeEach {
        mockApiClient = mockk()
        mockCacheService = mockk()
        mockHttpResponse = mockk()
        wantlistService = WantlistService(mockApiClient, mockCacheService)
    }

    context("Cache behavior of listWantsByUser function") {
        test("should use cache when valid cache exists") {
            val cachedWants = listOf(createTestWant(id = 1))

            every { mockCacheService.hasValidCache(testUsername) } returns true
            every { mockCacheService.getCached(testUsername) } returns cachedWants

            runTest {
                val result = wantlistService.listWantsByUser(testUsername)
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe cachedWants
            }

            verify { mockCacheService.hasValidCache(testUsername) }
            verify { mockCacheService.getCached(testUsername) }
            coVerify(exactly = 0) { mockApiClient.listUsersWantList(any(), any(), any()) }
        }

        test("should call API when no valid cache exists") {
            val apiWants = listOf(createTestWant(id = 1))
            val wantsResponse = WantsResponse(
                result = apiWants,
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
            coEvery { mockHttpResponse.body<WantsResponse>() } returns wantsResponse
            coEvery { mockApiClient.listUsersWantList(testUsername, 1, 100) } returns mockHttpResponse
            every { mockCacheService.cache(testUsername, apiWants) } just Runs

            runTest {
                val result = wantlistService.listWantsByUser(testUsername)
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe apiWants
            }

            verify { mockCacheService.hasValidCache(testUsername) }
            coVerify { mockApiClient.listUsersWantList(testUsername, 1, 100) }
            verify { mockCacheService.cache(testUsername, apiWants) }
        }

        test("should fallback to API when cache retrieval fails") {
            val apiWants = listOf(createTestWant(id = 1))
            val wantsResponse = WantsResponse(
                result = apiWants,
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
            coEvery { mockHttpResponse.body<WantsResponse>() } returns wantsResponse
            coEvery { mockApiClient.listUsersWantList(testUsername, 1, 100) } returns mockHttpResponse
            every { mockCacheService.cache(testUsername, apiWants) } just Runs

            runTest {
                val result = wantlistService.listWantsByUser(testUsername)
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe apiWants
            }

            verify { mockCacheService.hasValidCache(testUsername) }
            verify { mockCacheService.getCached(testUsername) }
            coVerify { mockApiClient.listUsersWantList(testUsername, 1, 100) }
            verify { mockCacheService.cache(testUsername, apiWants) }
        }

        test("should call API again after 24 hours (cache expired)") {
            val apiWants = listOf(createTestWant(id = 1))
            val wantsResponse = WantsResponse(
                result = apiWants,
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
            every { mockCacheService.getCached(testUsername) } returns apiWants

            runTest {
                val firstResult = wantlistService.listWantsByUser(testUsername)
                firstResult.isSuccess shouldBe true
            }

            // Second call - cache is expired (24 hours passed)
            every { mockCacheService.hasValidCache(testUsername) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<WantsResponse>() } returns wantsResponse
            coEvery { mockApiClient.listUsersWantList(testUsername, 1, 100) } returns mockHttpResponse
            every { mockCacheService.cache(testUsername, apiWants) } just Runs

            runTest {
                val secondResult = wantlistService.listWantsByUser(testUsername)
                secondResult.isSuccess shouldBe true
            }

            // Verify first call used cache, second call used API
            verify(exactly = 2) { mockCacheService.hasValidCache(testUsername) }
            verify(exactly = 1) { mockCacheService.getCached(testUsername) }
            coVerify(exactly = 1) { mockApiClient.listUsersWantList(testUsername, 1, 100) }
            verify(exactly = 1) { mockCacheService.cache(testUsername, apiWants) }
        }
    }

    context("API pagination") {
        test("should handle multiple pages of results") {
            val page1Wants = listOf(createTestWant(id = 1))
            val page2Wants = listOf(createTestWant(id = 2))
            val allWants = page1Wants + page2Wants

            val page1Response = WantsResponse(
                result = page1Wants,
                pagination = Pagination(
                    page = 1,
                    pages = 2,
                    perPage = 1,
                    items = 2,
                    urls = Urls(
                        last = "https://api.discogs.com/users/test-user/wants?page=42&per_page=1&sort=label&sort_order=asc",
                        next = "https://api.discogs.com/users/test-user/wants?page=2&per_page=1&sort=label&sort_order=asc",
                    ),
                )
            )

            val page2Response = WantsResponse(
                result = page2Wants,
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
            coEvery { mockResponse1.body<WantsResponse>() } returns page1Response
            coEvery { mockApiClient.listUsersWantList(testUsername, 1, 100) } returns mockResponse1

            val mockResponse2 = mockk<HttpResponse>()
            every { mockResponse2.status } returns HttpStatusCode.OK
            coEvery { mockResponse2.body<WantsResponse>() } returns page2Response
            coEvery { mockApiClient.listUsersWantList(testUsername, 2, 100) } returns mockResponse2

            every { mockCacheService.cache(testUsername, allWants) } just Runs

            runTest {
                val result = wantlistService.listWantsByUser(testUsername)
                result.isSuccess shouldBe true
                result.getOrNull()?.size shouldBe 2
                result.getOrNull() shouldBe allWants
            }

            coVerify { mockApiClient.listUsersWantList(testUsername, 1, 100) }
            coVerify { mockApiClient.listUsersWantList(testUsername, 2, 100) }
            verify { mockCacheService.cache(testUsername, allWants) }
        }
    }

    context("API error handling") {
        test("should handle forbidden access") {
            every { mockCacheService.hasValidCache(testUsername) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.Forbidden
            coEvery { mockApiClient.listUsersWantList(testUsername, 1, 100) } returns mockHttpResponse

            runTest {
                val result = wantlistService.listWantsByUser(testUsername)
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<ApiException>()
                val exception = result.exceptionOrNull() as ApiException
                exception.error shouldBe ApiError.NoAccessToCollection
            }
        }

        test("should handle unknown status codes") {
            every { mockCacheService.hasValidCache(testUsername) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.InternalServerError
            coEvery { mockApiClient.listUsersWantList(testUsername, 1, 100) } returns mockHttpResponse

            runTest {
                val result = wantlistService.listWantsByUser(testUsername)
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<ApiException>()
                val exception = result.exceptionOrNull() as ApiException
                exception.error.shouldBeInstanceOf<ApiError.Unknown>()
            }
        }
    }

    context("refreshWantlistByUser") {
        test("should clear cache and fetch from API") {
            val apiWants = listOf(createTestWant(id = 1))
            val wantsResponse = WantsResponse(
                result = apiWants,
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
            coEvery { mockHttpResponse.body<WantsResponse>() } returns wantsResponse
            coEvery { mockApiClient.listUsersWantList(testUsername, 1, 100) } returns mockHttpResponse
            every { mockCacheService.cache(testUsername, apiWants) } just Runs

            runTest {
                val result = wantlistService.refreshWantlistByUser(testUsername)
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe apiWants
            }

            verify { mockCacheService.clearCache(testUsername) }
            coVerify { mockApiClient.listUsersWantList(testUsername, 1, 100) }
            verify { mockCacheService.cache(testUsername, apiWants) }
        }
    }

    context("getIdsFromWantlistReleasesByUser") {
        test("should return release IDs from wantlist") {
            val wants = listOf(
                createTestWant(id = 1, releaseId = 100),
                createTestWant(id = 2, releaseId = 200)
            )

            every { mockCacheService.hasValidCache(testUsername) } returns true
            every { mockCacheService.getCached(testUsername) } returns wants

            runTest {
                val result = wantlistService.getIdsFromWantlistReleasesByUser(testUsername)
                result shouldBe setOf(100L, 200L)
            }
        }

        test("should return empty set when username is null") {
            runTest {
                val result = wantlistService.getIdsFromWantlistReleasesByUser(null)
                result shouldBe emptySet()
            }
        }

        test("should return empty set when wantlist fetch fails") {
            every { mockCacheService.hasValidCache(testUsername) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.Forbidden
            coEvery { mockApiClient.listUsersWantList(testUsername, 1, 100) } returns mockHttpResponse

            runTest {
                val result = wantlistService.getIdsFromWantlistReleasesByUser(testUsername)
                result shouldBe emptySet()
            }
        }
    }

    context("Cache sorting behavior") {
        test("should sort cached wantlist by title ascending") {
            val cachedWants = listOf(
                createTestWant(id = 1, releaseId = 101, title = "Zulu"),
                createTestWant(id = 2, releaseId = 102, title = "Alpha")
            )

            every { mockCacheService.hasValidCache(testUsername) } returns true
            every { mockCacheService.getCached(testUsername) } returns cachedWants

            runTest {
                val result = wantlistService.listWantsByUser(testUsername, "title", "asc")
                result.isSuccess shouldBe true
                result.getOrNull()!!.map { it.basicInformation.title } shouldBe listOf("Alpha", "Zulu")
            }

            verify { mockCacheService.hasValidCache(testUsername) }
            verify { mockCacheService.getCached(testUsername) }
            coVerify(exactly = 0) { mockApiClient.listUsersWantList(any(), any(), any(), any(), any()) }
        }

        test("should sort cached wantlist by artist descending") {
            val cachedWants = listOf(
                createTestWant(id = 1, releaseId = 101, artistName = "Beta Band"),
                createTestWant(id = 2, releaseId = 102, artistName = "Alpha Crew")
            )

            every { mockCacheService.hasValidCache(testUsername) } returns true
            every { mockCacheService.getCached(testUsername) } returns cachedWants

            runTest {
                val result = wantlistService.listWantsByUser(testUsername, "artist", "desc")

                result.isSuccess shouldBe true
                result.getOrNull()!!.map { want ->
                    want.basicInformation.artists.first().name
                } shouldBe listOf("Beta Band", "Alpha Crew")
            }

            verify { mockCacheService.hasValidCache(testUsername) }
            verify { mockCacheService.getCached(testUsername) }
            coVerify(exactly = 0) { mockApiClient.listUsersWantList(any(), any(), any(), any(), any()) }
        }
    }
})

private fun createTestWant(
    id: Long = 1L,
    rating: Int = 5,
    releaseId: Long = id,
    title: String = "Test Album $id",
    artistName: String = "Test Artist"
): Want {
    return Want(
        releaseId = id,
        rating = rating,
        resourceUrl = Url("https://api.discogs.com/releases/$id"),
        basicInformation = BasicInformation(
            id = releaseId,
            title = title,
            year = 2023,
            thumb = "https://example.com/thumb.jpg",
            coverImage = "https://example.com/cover.jpg",
            resourceUrl = Url("https://api.discogs.com/releases/$releaseId"),
            formats = listOf(
                Format(
                    name = "Vinyl",
                    qty = "1",
                    descriptions = listOf("LP", "Album"),
                    text = "12\""
                )
            ),
            labels = listOf(
                Label(
                    id = 1,
                    name = "Test Label",
                    catalogNumber = "TEST001",
                    entityType = "1",
                    resourceUrl = Url("https://api.discogs.com/labels/1")
                )
            ),
            artists = listOf(
                Artist(
                    id = 1,
                    name = artistName,
                    anv = "",
                    join = "",
                    role = "",
                    tracks = "",
                    resourceUrl = Url("https://api.discogs.com/artists/1")
                )
            )
        )
    )
}


