package de.ezienecker.wantlist.service

import de.ezienecker.core.infrastructure.discogs.client.ApiError
import de.ezienecker.core.infrastructure.discogs.client.ApiException
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceApiClient
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceListing
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceSeller
import de.ezienecker.wantlist.infrastructure.repository.MarketplaceCacheService
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

class MarketplaceServiceTest : FunSpec({

    lateinit var mockApiClient: MarketplaceApiClient
    lateinit var mockTransformService: MarketplaceListingTransformService
    lateinit var mockCacheService: MarketplaceCacheService
    lateinit var mockHttpResponse: HttpResponse
    lateinit var marketplaceService: MarketplaceService

    val testReleaseId = 12345L

    beforeEach {
        mockApiClient = mockk()
        mockTransformService = mockk()
        mockCacheService = mockk()
        mockHttpResponse = mockk()
        marketplaceService = MarketplaceService(mockApiClient, mockTransformService, mockCacheService)
    }

    context("Cache behavior of getListingsByReleaseId function") {
        test("should use cache when valid cache exists") {
            val cachedListings = listOf(createTestListing(releaseId = testReleaseId))

            every { mockCacheService.hasValidCache(testReleaseId) } returns true
            every { mockCacheService.getCached(testReleaseId) } returns cachedListings

            runTest {
                val result = marketplaceService.getListingsByReleaseId(testReleaseId)
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe cachedListings
            }

            verify { mockCacheService.hasValidCache(testReleaseId) }
            verify { mockCacheService.getCached(testReleaseId) }
            coVerify(exactly = 0) { mockApiClient.getListingsByReleaseId(any()) }
            verify(exactly = 0) { mockTransformService.transformListings(any(), any()) }
        }

        test("should call API when no valid cache exists") {
            val apiListings = listOf(createTestListing(releaseId = testReleaseId))
            val htmlContent = "<html><body>Test HTML</body></html>"

            every { mockCacheService.hasValidCache(testReleaseId) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<String>() } returns htmlContent
            coEvery { mockApiClient.getListingsByReleaseId(testReleaseId) } returns mockHttpResponse
            every { mockTransformService.transformListings(testReleaseId, htmlContent) } returns apiListings
            every { mockCacheService.cache(testReleaseId, apiListings) } just Runs

            runTest {
                val result = marketplaceService.getListingsByReleaseId(testReleaseId)
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe apiListings
            }

            verify { mockCacheService.hasValidCache(testReleaseId) }
            coVerify { mockApiClient.getListingsByReleaseId(testReleaseId) }
            verify { mockTransformService.transformListings(testReleaseId, htmlContent) }
            verify { mockCacheService.cache(testReleaseId, apiListings) }
        }

        test("should fallback to API when cache retrieval fails") {
            val apiListings = listOf(createTestListing(releaseId = testReleaseId))
            val htmlContent = "<html><body>Test HTML</body></html>"

            every { mockCacheService.hasValidCache(testReleaseId) } returns true
            every { mockCacheService.getCached(testReleaseId) } throws RuntimeException("Database connection error")
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<String>() } returns htmlContent
            coEvery { mockApiClient.getListingsByReleaseId(testReleaseId) } returns mockHttpResponse
            every { mockTransformService.transformListings(testReleaseId, htmlContent) } returns apiListings
            every { mockCacheService.cache(testReleaseId, apiListings) } just Runs

            runTest {
                val result = marketplaceService.getListingsByReleaseId(testReleaseId)
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe apiListings
            }

            verify { mockCacheService.hasValidCache(testReleaseId) }
            verify { mockCacheService.getCached(testReleaseId) }
            coVerify { mockApiClient.getListingsByReleaseId(testReleaseId) }
            verify { mockTransformService.transformListings(testReleaseId, htmlContent) }
            verify { mockCacheService.cache(testReleaseId, apiListings) }
        }

        test("should handle cache expiry correctly") {
            val apiListings = listOf(createTestListing(releaseId = testReleaseId))
            val htmlContent = "<html><body>Test HTML</body></html>"

            // First call - no cache exists yet, API is called
            every { mockCacheService.hasValidCache(testReleaseId) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<String>() } returns htmlContent
            coEvery { mockApiClient.getListingsByReleaseId(testReleaseId) } returns mockHttpResponse
            every { mockTransformService.transformListings(testReleaseId, htmlContent) } returns apiListings
            every { mockCacheService.cache(testReleaseId, apiListings) } just Runs

            runTest {
                val firstResult = marketplaceService.getListingsByReleaseId(testReleaseId)
                firstResult.isSuccess shouldBe true
            }

            // Second call - cache is now valid and used
            every { mockCacheService.hasValidCache(testReleaseId) } returns true
            every { mockCacheService.getCached(testReleaseId) } returns apiListings

            runTest {
                val secondResult = marketplaceService.getListingsByReleaseId(testReleaseId)
                secondResult.isSuccess shouldBe true
            }

            // Verify first call used API, second call used cache
            verify(exactly = 2) { mockCacheService.hasValidCache(testReleaseId) }
            verify(exactly = 1) { mockCacheService.getCached(testReleaseId) }
            coVerify(exactly = 1) { mockApiClient.getListingsByReleaseId(testReleaseId) }
            verify(exactly = 1) { mockTransformService.transformListings(testReleaseId, htmlContent) }
            verify(exactly = 1) { mockCacheService.cache(testReleaseId, apiListings) }
        }
    }

    context("API error handling") {
        test("should handle non-OK HTTP status") {
            every { mockCacheService.hasValidCache(testReleaseId) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.NotFound
            coEvery { mockHttpResponse.body<String>() } returns "Not Found"
            coEvery { mockApiClient.getListingsByReleaseId(testReleaseId) } returns mockHttpResponse

            runTest {
                val result = marketplaceService.getListingsByReleaseId(testReleaseId)

                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<ApiException>()
                val exception = result.exceptionOrNull() as ApiException
                exception.error.shouldBeInstanceOf<ApiError.Unknown>()
            }

            verify { mockCacheService.hasValidCache(testReleaseId) }
            coVerify { mockApiClient.getListingsByReleaseId(testReleaseId) }
            verify(exactly = 0) { mockCacheService.cache(any(), any()) }
        }

        test("should handle forbidden access (403)") {
            every { mockCacheService.hasValidCache(testReleaseId) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.Forbidden
            coEvery { mockHttpResponse.body<String>() } returns "Forbidden"
            coEvery { mockApiClient.getListingsByReleaseId(testReleaseId) } returns mockHttpResponse

            runTest {
                val result = marketplaceService.getListingsByReleaseId(testReleaseId)

                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<ApiException>()
            }

            coVerify { mockApiClient.getListingsByReleaseId(testReleaseId) }
            verify(exactly = 0) { mockCacheService.cache(any(), any()) }
        }

        test("should handle server error (500)") {
            every { mockCacheService.hasValidCache(testReleaseId) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.InternalServerError
            coEvery { mockApiClient.getListingsByReleaseId(testReleaseId) } returns mockHttpResponse

            runTest {
                val result = marketplaceService.getListingsByReleaseId(testReleaseId)

                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<ApiException>()
            }

            coVerify { mockApiClient.getListingsByReleaseId(testReleaseId) }
            verify(exactly = 0) { mockCacheService.cache(any(), any()) }
        }

        test("should handle service unavailable (503)") {
            every { mockCacheService.hasValidCache(testReleaseId) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.ServiceUnavailable
            coEvery { mockApiClient.getListingsByReleaseId(testReleaseId) } returns mockHttpResponse

            runTest {
                val result = marketplaceService.getListingsByReleaseId(testReleaseId)

                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<ApiException>()
            }
        }

        test("should handle bad request (400)") {
            every { mockCacheService.hasValidCache(testReleaseId) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.BadRequest
            coEvery { mockHttpResponse.body<String>() } returns "Bad Request"
            coEvery { mockApiClient.getListingsByReleaseId(testReleaseId) } returns mockHttpResponse

            runTest {
                val result = marketplaceService.getListingsByReleaseId(testReleaseId)

                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<ApiException>()
            }
        }
    }

    context("Edge cases") {
        test("should handle empty marketplace listings from API") {
            val emptyListings = emptyList<MarketplaceListing>()
            val htmlContent = "<html><body></body></html>"

            every { mockCacheService.hasValidCache(testReleaseId) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<String>() } returns htmlContent
            coEvery { mockApiClient.getListingsByReleaseId(testReleaseId) } returns mockHttpResponse
            every { mockTransformService.transformListings(testReleaseId, htmlContent) } returns emptyListings
            every { mockCacheService.cache(testReleaseId, emptyListings) } just Runs

            runTest {
                val result = marketplaceService.getListingsByReleaseId(testReleaseId)
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe emptyListings
            }

            verify { mockCacheService.cache(testReleaseId, emptyListings) }
        }

        test("should handle very large listing response") {
            val largeListings = (1..1000).map { createTestListing(releaseId = testReleaseId, title = "Album $it") }
            val htmlContent = "<html><body>Large HTML content</body></html>"

            every { mockCacheService.hasValidCache(testReleaseId) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<String>() } returns htmlContent
            coEvery { mockApiClient.getListingsByReleaseId(testReleaseId) } returns mockHttpResponse
            every { mockTransformService.transformListings(testReleaseId, htmlContent) } returns largeListings
            every { mockCacheService.cache(testReleaseId, largeListings) } just Runs

            runTest {
                val result = marketplaceService.getListingsByReleaseId(testReleaseId)
                result.isSuccess shouldBe true
                result.getOrNull()?.size shouldBe 1000
            }

            verify { mockCacheService.cache(testReleaseId, largeListings) }
        }

        test("should continue successfully even if cache fails to save") {
            val apiListings = listOf(createTestListing(releaseId = testReleaseId))
            val htmlContent = "<html><body>Test HTML</body></html>"

            every { mockCacheService.hasValidCache(testReleaseId) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<String>() } returns htmlContent
            coEvery { mockApiClient.getListingsByReleaseId(testReleaseId) } returns mockHttpResponse
            every { mockTransformService.transformListings(testReleaseId, htmlContent) } returns apiListings
            every { mockCacheService.cache(testReleaseId, apiListings) } throws RuntimeException("Cache write failed")

            runTest {
                val result = marketplaceService.getListingsByReleaseId(testReleaseId)
                // Should still return success since the API call was successful
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe apiListings
            }

            verify { mockCacheService.cache(testReleaseId, apiListings) }
        }

        test("should handle multiple consecutive calls for same release ID") {
            val cachedListings = listOf(createTestListing(releaseId = testReleaseId))

            every { mockCacheService.hasValidCache(testReleaseId) } returns true
            every { mockCacheService.getCached(testReleaseId) } returns cachedListings

            runTest {
                // Make multiple calls
                repeat(5) {
                    val result = marketplaceService.getListingsByReleaseId(testReleaseId)
                    result.isSuccess shouldBe true
                    result.getOrNull() shouldBe cachedListings
                }
            }

            // All calls should use cache
            verify(exactly = 5) { mockCacheService.hasValidCache(testReleaseId) }
            verify(exactly = 5) { mockCacheService.getCached(testReleaseId) }
            coVerify(exactly = 0) { mockApiClient.getListingsByReleaseId(testReleaseId) }
        }

        test("should handle different release IDs independently") {
            val releaseId1 = 111L
            val releaseId2 = 222L
            val listings1 = listOf(createTestListing(releaseId = releaseId1))
            val listings2 = listOf(createTestListing(releaseId = releaseId2))

            every { mockCacheService.hasValidCache(releaseId1) } returns true
            every { mockCacheService.getCached(releaseId1) } returns listings1
            every { mockCacheService.hasValidCache(releaseId2) } returns true
            every { mockCacheService.getCached(releaseId2) } returns listings2

            runTest {
                val result1 = marketplaceService.getListingsByReleaseId(releaseId1)
                val result2 = marketplaceService.getListingsByReleaseId(releaseId2)

                result1.isSuccess shouldBe true
                result2.isSuccess shouldBe true
                result1.getOrNull() shouldBe listings1
                result2.getOrNull() shouldBe listings2
            }

            verify { mockCacheService.hasValidCache(releaseId1) }
            verify { mockCacheService.hasValidCache(releaseId2) }
            verify { mockCacheService.getCached(releaseId1) }
            verify { mockCacheService.getCached(releaseId2) }
        }

        test("should handle transformation service returning null values") {
            val listingsWithNulls = listOf(
                MarketplaceListing(
                    releaseId = testReleaseId,
                    title = "",
                    resourceUrl = "",
                    mediaCondition = "",
                    sleeveCondition = "",
                    price = "",
                    seller = MarketplaceSeller(""),
                    shippingLocation = ""
                )
            )
            val htmlContent = "<html><body>Malformed HTML</body></html>"

            every { mockCacheService.hasValidCache(testReleaseId) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<String>() } returns htmlContent
            coEvery { mockApiClient.getListingsByReleaseId(testReleaseId) } returns mockHttpResponse
            every { mockTransformService.transformListings(testReleaseId, htmlContent) } returns listingsWithNulls
            every { mockCacheService.cache(testReleaseId, listingsWithNulls) } just Runs

            runTest {
                val result = marketplaceService.getListingsByReleaseId(testReleaseId)
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe listingsWithNulls
            }
        }
    }

    context("Response body handling") {
        test("should handle HTML with special characters") {
            val specialHtml = "<html><body>Price: €25.99 • Seller: Test & Co.</body></html>"
            val listings = listOf(createTestListing(releaseId = testReleaseId, price = "€25.99"))

            every { mockCacheService.hasValidCache(testReleaseId) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<String>() } returns specialHtml
            coEvery { mockApiClient.getListingsByReleaseId(testReleaseId) } returns mockHttpResponse
            every { mockTransformService.transformListings(testReleaseId, specialHtml) } returns listings
            every { mockCacheService.cache(testReleaseId, listings) } just Runs

            runTest {
                val result = marketplaceService.getListingsByReleaseId(testReleaseId)
                result.isSuccess shouldBe true
            }

            verify { mockTransformService.transformListings(testReleaseId, specialHtml) }
        }

        test("should handle very large HTML response") {
            val largeHtml = "<html><body>" + "x".repeat(1000000) + "</body></html>"
            val listings = listOf(createTestListing(releaseId = testReleaseId))

            every { mockCacheService.hasValidCache(testReleaseId) } returns false
            every { mockHttpResponse.status } returns HttpStatusCode.OK
            coEvery { mockHttpResponse.body<String>() } returns largeHtml
            coEvery { mockApiClient.getListingsByReleaseId(testReleaseId) } returns mockHttpResponse
            every { mockTransformService.transformListings(testReleaseId, largeHtml) } returns listings
            every { mockCacheService.cache(testReleaseId, listings) } just Runs

            runTest {
                val result = marketplaceService.getListingsByReleaseId(testReleaseId)
                result.isSuccess shouldBe true
            }

            verify { mockTransformService.transformListings(testReleaseId, largeHtml) }
        }
    }
})

private fun createTestListing(
    releaseId: Long = 12345L,
    title: String = "Test Album",
    price: String = "$25.99",
    sellerName: String = "test-seller"
): MarketplaceListing {
    return MarketplaceListing(
        releaseId = releaseId,
        title = title,
        resourceUrl = "https://www.discogs.com/sell/release/$releaseId",
        mediaCondition = "Near Mint (NM or M-)",
        sleeveCondition = "Very Good Plus (VG+)",
        price = price,
        seller = MarketplaceSeller(sellerName),
        shippingLocation = "United States"
    )
}
