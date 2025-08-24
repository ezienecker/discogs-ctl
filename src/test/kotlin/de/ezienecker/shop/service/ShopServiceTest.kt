package de.ezienecker.shop.service

import de.ezienecker.shared.database.cache.ShopCacheService
import de.ezienecker.shared.discogs.client.ApiError
import de.ezienecker.shared.discogs.client.ApiException
import de.ezienecker.shared.discogs.marketplace.Artist
import de.ezienecker.shared.discogs.marketplace.Comments
import de.ezienecker.shared.discogs.marketplace.Condition
import de.ezienecker.shared.discogs.marketplace.Currency
import de.ezienecker.shared.discogs.marketplace.Description
import de.ezienecker.shared.discogs.marketplace.Format
import de.ezienecker.shared.discogs.marketplace.Listing
import de.ezienecker.shared.discogs.marketplace.Price
import de.ezienecker.shared.discogs.marketplace.Release
import de.ezienecker.shared.discogs.marketplace.Seller
import de.ezienecker.shared.discogs.marketplace.ShopApiClient
import de.ezienecker.shared.discogs.marketplace.ShopResponse
import de.ezienecker.shared.discogs.marketplace.Status
import de.ezienecker.shared.discogs.marketplace.Title
import de.ezienecker.shared.discogs.marketplace.Uri
import de.ezienecker.shared.discogs.marketplace.Url
import de.ezienecker.shared.discogs.marketplace.Username
import de.ezienecker.shared.discogs.shared.Pagination
import de.ezienecker.shared.discogs.shared.Urls
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
import kotlinx.coroutines.runBlocking

class ShopServiceTest : FunSpec({

    lateinit var mockApiClient: ShopApiClient
    lateinit var mockCacheService: ShopCacheService
    lateinit var mockHttpResponse: HttpResponse
    lateinit var shopService: ShopService

    val testUsername = "test-user"

    beforeEach {
        mockApiClient = mockk()
        mockCacheService = mockk()
        mockHttpResponse = mockk()
        shopService = ShopService(mockApiClient, mockCacheService)
    }

    context("Cache behavior of listInventoryByUser function") {
        test("should use cache when valid cache exists") {
            val cachedListings = listOf(createTestListing(id = 1))

            every { mockCacheService.hasValidCache(testUsername) } returns true
            every { mockCacheService.getCached(testUsername) } returns cachedListings

            val result = runBlocking { shopService.listInventoryByUser(testUsername) }

            result.isSuccess shouldBe true
            result.getOrNull() shouldBe cachedListings

            verify { mockCacheService.hasValidCache(testUsername) }
            verify { mockCacheService.getCached(testUsername) }
            coVerify(exactly = 0) { mockApiClient.listUsersShop(any(), any(), any()) }
        }

        test("should call API when no valid cache exists") {
            val apiListings = listOf(createTestListing(id = 1))
            val shopResponse = ShopResponse(
                result = apiListings,
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
            coEvery { mockHttpResponse.body<ShopResponse>() } returns shopResponse
            coEvery { mockApiClient.listUsersShop(testUsername, 1, 100) } returns mockHttpResponse
            every { mockCacheService.cache(testUsername, apiListings) } just Runs

            val result = runBlocking { shopService.listInventoryByUser(testUsername) }

            result.isSuccess shouldBe true
            result.getOrNull() shouldBe apiListings

            verify { mockCacheService.hasValidCache(testUsername) }
            coVerify { mockApiClient.listUsersShop(testUsername, 1, 100) }
            verify { mockCacheService.cache(testUsername, apiListings) }
        }

        test("should fallback to API when cache retrieval fails") {
            val apiListings = listOf(createTestListing(id = 1))
            val shopResponse = ShopResponse(
                result = apiListings,
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
            coEvery { mockHttpResponse.body<ShopResponse>() } returns shopResponse
            coEvery { mockApiClient.listUsersShop(testUsername, 1, 100) } returns mockHttpResponse
            every { mockCacheService.cache(testUsername, apiListings) } just Runs

            val result = runBlocking { shopService.listInventoryByUser(testUsername) }

            result.isSuccess shouldBe true
            result.getOrNull() shouldBe apiListings

            verify { mockCacheService.hasValidCache(testUsername) }
            verify { mockCacheService.getCached(testUsername) }
            coVerify { mockApiClient.listUsersShop(testUsername, 1, 100) }
            verify { mockCacheService.cache(testUsername, apiListings) }
        }

        test("should handle cache expiry correctly") {
            val apiListings = listOf(createTestListing(id = 1))
            val shopResponse = ShopResponse(
                result = apiListings,
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
            every { mockCacheService.getCached(testUsername) } returns apiListings

            val firstResult = runBlocking { shopService.listInventoryByUser(testUsername) }
            firstResult.isSuccess shouldBe true

            // Second call - cache is expired (24 hours passed)
            every { mockCacheService.hasValidCache(testUsername) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<ShopResponse>() } returns shopResponse
            coEvery { mockApiClient.listUsersShop(testUsername, 1, 100) } returns mockHttpResponse
            every { mockCacheService.cache(testUsername, apiListings) } just Runs

            val secondResult = runBlocking { shopService.listInventoryByUser(testUsername) }
            secondResult.isSuccess shouldBe true

            // Verify first call used cache, second call used API
            verify(exactly = 2) { mockCacheService.hasValidCache(testUsername) }
            verify(exactly = 1) { mockCacheService.getCached(testUsername) }
            coVerify(exactly = 1) { mockApiClient.listUsersShop(testUsername, 1, 100) }
            verify(exactly = 1) { mockCacheService.cache(testUsername, apiListings) }
        }
    }

    context("API Handling") {
        test("should handle pagination correctly") {
            val page1Listings = listOf(createTestListing(id = 1))
            val page2Listings = listOf(createTestListing(id = 2))
            val allListings = page1Listings + page2Listings

            val page1Response = ShopResponse(
                result = page1Listings,
                pagination = Pagination(
                    page = 1,
                    pages = 2,
                    perPage = 1,
                    items = 2,
                    urls = Urls(
                        last = "https://api.discogs.com/users/test-user/inventory?page=42&per_page=1&sort=label&sort_order=asc",
                        next = "https://api.discogs.com/users/test-user/inventory?page=2&per_page=1&sort=label&sort_order=asc",
                    ),
                )
            )

            val page2Response = ShopResponse(
                result = page2Listings,
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
            coEvery { mockResponse1.body<ShopResponse>() } returns page1Response
            coEvery { mockApiClient.listUsersShop(testUsername, 1, 100) } returns mockResponse1

            val mockResponse2 = mockk<HttpResponse>()
            every { mockResponse2.status } returns HttpStatusCode.OK
            coEvery { mockResponse2.body<ShopResponse>() } returns page2Response
            coEvery { mockApiClient.listUsersShop(testUsername, 2, 100) } returns mockResponse2

            every { mockCacheService.cache(testUsername, allListings) } just Runs

            val result = runBlocking { shopService.listInventoryByUser(testUsername) }

            result.isSuccess shouldBe true
            result.getOrNull()?.size shouldBe 2
            result.getOrNull() shouldBe allListings

            coVerify { mockApiClient.listUsersShop(testUsername, 1, 100) }
            coVerify { mockApiClient.listUsersShop(testUsername, 2, 100) }
            verify { mockCacheService.cache(testUsername, allListings) }
        }

        test("should handle API errors gracefully") {
            every { mockCacheService.hasValidCache(testUsername) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.Forbidden
            coEvery { mockApiClient.listUsersShop(testUsername, 1, 100) } returns mockHttpResponse

            val result = runBlocking { shopService.listInventoryByUser(testUsername) }

            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<ApiException>()
            val exception = result.exceptionOrNull() as ApiException
            exception.error.shouldBeInstanceOf<ApiError.Unknown>()
        }

        test("should handle server errors") {
            every { mockCacheService.hasValidCache(testUsername) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.InternalServerError
            coEvery { mockApiClient.listUsersShop(testUsername, 1, 100) } returns mockHttpResponse

            val result = runBlocking { shopService.listInventoryByUser(testUsername) }

            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<ApiException>()
            val exception = result.exceptionOrNull() as ApiException
            exception.error.shouldBeInstanceOf<ApiError.Unknown>()
        }
    }


    context("refreshInventoryByUser") {
        test("should clear cache and fetch fresh data") {
            val apiListings = listOf(createTestListing(id = 1))
            val shopResponse = ShopResponse(
                result = apiListings,
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
            coEvery { mockHttpResponse.body<ShopResponse>() } returns shopResponse
            coEvery { mockApiClient.listUsersShop(testUsername, 1, 100) } returns mockHttpResponse
            every { mockCacheService.cache(testUsername, apiListings) } just Runs

            val result = runBlocking { shopService.refreshInventoryByUser(testUsername) }

            result.isSuccess shouldBe true
            result.getOrNull() shouldBe apiListings

            verify { mockCacheService.clearCache(testUsername) }
            coVerify { mockApiClient.listUsersShop(testUsername, 1, 100) }
            verify { mockCacheService.cache(testUsername, apiListings) }
        }
    }

    context("getIdsFromInventoryReleasesByUser") {
        test("should return release IDs from inventory") {
            val listings = listOf(
                createTestListing(id = 1, releaseId = 100),
                createTestListing(id = 2, releaseId = 200)
            )

            every { mockCacheService.hasValidCache(testUsername) } returns true
            every { mockCacheService.getCached(testUsername) } returns listings

            val result = runBlocking { shopService.getIdsFromInventoryReleasesByUser(testUsername) }

            result shouldBe setOf(100L, 200L)
        }

        test("should return empty set for null username") {
            val result = runBlocking { shopService.getIdsFromInventoryReleasesByUser(null) }

            result shouldBe emptySet()
        }

        test("should return empty set when API fails") {
            every { mockCacheService.hasValidCache(testUsername) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.Forbidden
            coEvery { mockApiClient.listUsersShop(testUsername, 1, 100) } returns mockHttpResponse

            val result = runBlocking { shopService.getIdsFromInventoryReleasesByUser(testUsername) }

            result shouldBe emptySet()
        }
    }
})

private fun createTestListing(
    id: Long = 1L,
    releaseId: Long = 100L,
    title: String = "Test Album"
): Listing {
    return Listing(
        id = id,
        resourceUrl = Url("https://api.discogs.com/marketplace/listings/$id"),
        uri = Uri("/marketplace/listings/$id"),
        status = Status.FOR_SALE,
        mediaCondition = Condition.NEAR_MINT,
        sleeveCondition = Condition.VERY_GOOD_PLUS,
        comments = Comments("Test listing in excellent condition"),
        price = Price(
            value = 25.99,
            currency = Currency("USD")
        ),
        seller = Seller(
            username = Username("test-seller"),
            htmlUrl = Url("https://www.discogs.com/user/test-seller"),
            resourceUrl = Url("https://api.discogs.com/users/test-seller")
        ),
        release = Release(
            id = releaseId,
            description = Description("Test Album - Hip Hop - 2023 - Vinyl LP"),
            artist = Artist("Test Artist"),
            format = Format("Vinyl LP"),
            title = Title(title)
        )
    )
}
