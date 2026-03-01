package de.ezienecker.wantlist.service

import de.ezienecker.core.batch.BatchProcessor
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceApiClient
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceListing
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceSeller
import de.ezienecker.wantlist.infrastructure.repository.MarketplaceCacheService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest

class MarketplaceServiceTest : FunSpec({

    lateinit var mockApiClient: MarketplaceApiClient
    lateinit var mockTransformService: MarketplaceListingTransformService
    lateinit var mockCacheService: MarketplaceCacheService
    lateinit var mockBatchProcessor: BatchProcessor<Long, List<MarketplaceListing>>
    lateinit var marketplaceService: MarketplaceService

    beforeEach {
        mockApiClient = mockk()
        mockTransformService = mockk()
        mockCacheService = mockk()
        mockBatchProcessor = mockk()
        marketplaceService =
            MarketplaceService(mockApiClient, mockTransformService, mockCacheService, mockBatchProcessor)
    }

    context("getMarketplaceListingsByReleaseIds") {
        test("should return empty map when given empty release ID list") {
            every { mockCacheService.findByReleaseIdNotIn(emptyList()) } returns emptyList()
            every { mockCacheService.getAllCached(emptyList()) } returns emptyList()
            coEvery {
                mockBatchProcessor.processParallelBatch(
                    items = emptyList(),
                    batchSize = 20,
                    concurrency = 5,
                    processor = any()
                )
            } returns emptyList()

            runTest {
                val result = marketplaceService.getMarketplaceListingsByReleaseIds(emptyList())
                result shouldBe emptyMap()
            }

            verify { mockCacheService.findByReleaseIdNotIn(emptyList()) }
        }

        test("should group listings by seller") {
            val releaseIds = listOf(1L, 2L, 3L)
            val listings = listOf(
                createTestListing(releaseId = 1L, sellerName = "seller-A", price = "$10.00"),
                createTestListing(releaseId = 2L, sellerName = "seller-B", price = "$20.00"),
                createTestListing(releaseId = 3L, sellerName = "seller-A", price = "$15.00")
            )

            every { mockCacheService.findByReleaseIdNotIn(releaseIds) } returns releaseIds
            every { mockCacheService.getAllCached(emptyList()) } returns emptyList()

            coEvery {
                mockBatchProcessor.processParallelBatch(
                    items = match { it.toSet() == releaseIds.toSet() },
                    batchSize = 20,
                    concurrency = 5,
                    processor = any()
                )
            } returns listOf(listings)

            runTest {
                val result = marketplaceService.getMarketplaceListingsByReleaseIds(releaseIds)

                result.size shouldBe 2
                result[MarketplaceSeller("seller-A")]?.size shouldBe 2
                result[MarketplaceSeller("seller-B")]?.size shouldBe 1
            }
        }

        test("should handle single seller with multiple listings") {
            val releaseIds = listOf(1L, 2L, 3L)
            val listings = listOf(
                createTestListing(releaseId = 1L, sellerName = "mega-seller", price = "$10.00"),
                createTestListing(releaseId = 2L, sellerName = "mega-seller", price = "$20.00"),
                createTestListing(releaseId = 3L, sellerName = "mega-seller", price = "$30.00")
            )

            every { mockCacheService.findByReleaseIdNotIn(releaseIds) } returns releaseIds
            every { mockCacheService.getAllCached(emptyList()) } returns emptyList()

            coEvery {
                mockBatchProcessor.processParallelBatch(
                    items = match { it.toSet() == releaseIds.toSet() },
                    batchSize = 20,
                    concurrency = 5,
                    processor = any()
                )
            } returns listOf(listings)

            runTest {
                val result = marketplaceService.getMarketplaceListingsByReleaseIds(releaseIds)

                result.size shouldBe 1
                result[MarketplaceSeller("mega-seller")]?.size shouldBe 3
            }
        }

        test("should handle releases with no marketplace listings") {
            val releaseIds = listOf(1L, 2L, 3L)

            every { mockCacheService.findByReleaseIdNotIn(releaseIds) } returns releaseIds
            every { mockCacheService.getAllCached(emptyList()) } returns emptyList()

            coEvery {
                mockBatchProcessor.processParallelBatch(
                    items = match { it.toSet() == releaseIds.toSet() },
                    batchSize = 20,
                    concurrency = 5,
                    processor = any()
                )
            } returns listOf(emptyList())

            runTest {
                val result = marketplaceService.getMarketplaceListingsByReleaseIds(releaseIds)
                result shouldBe emptyMap()
            }
        }

        test("should handle mix of releases with and without listings") {
            val releaseIds = listOf(1L, 2L, 3L, 4L)
            val listings = listOf(
                createTestListing(releaseId = 1L, sellerName = "seller-A"),
                createTestListing(releaseId = 3L, sellerName = "seller-B")
            )

            every { mockCacheService.findByReleaseIdNotIn(releaseIds) } returns releaseIds
            every { mockCacheService.getAllCached(emptyList()) } returns emptyList()

            coEvery {
                mockBatchProcessor.processParallelBatch(
                    items = match { it.toSet() == releaseIds.toSet() },
                    batchSize = 20,
                    concurrency = 5,
                    processor = any()
                )
            } returns listOf(listings)

            runTest {
                val result = marketplaceService.getMarketplaceListingsByReleaseIds(releaseIds)

                result.size shouldBe 2
                result.values.flatten().size shouldBe 2
            }
        }

        test("should handle large number of release IDs (batch processing)") {
            val releaseIds = (1L..100L).toList()
            val listings = releaseIds.map { id ->
                createTestListing(releaseId = id, sellerName = "seller-${id % 10}")
            }

            every { mockCacheService.findByReleaseIdNotIn(releaseIds) } returns releaseIds
            every { mockCacheService.getAllCached(emptyList()) } returns emptyList()

            coEvery {
                mockBatchProcessor.processParallelBatch(
                    items = match { it.toSet() == releaseIds.toSet() },
                    batchSize = 20,
                    concurrency = 5,
                    processor = any()
                )
            } returns listOf(listings)

            runTest {
                val result = marketplaceService.getMarketplaceListingsByReleaseIds(releaseIds)

                // Should be grouped by 10 different sellers (seller-0 to seller-9)
                result.size shouldBe 10
                result.values.flatten().size shouldBe 100
            }
        }

        test("should handle single release ID") {
            val releaseIds = listOf(42L)
            val listings = listOf(
                createTestListing(releaseId = 42L, sellerName = "solo-seller")
            )

            every { mockCacheService.findByReleaseIdNotIn(releaseIds) } returns releaseIds
            every { mockCacheService.getAllCached(emptyList()) } returns emptyList()

            coEvery {
                mockBatchProcessor.processParallelBatch(
                    items = match { it.toSet() == releaseIds.toSet() },
                    batchSize = 20,
                    concurrency = 5,
                    processor = any()
                )
            } returns listOf(listings)

            runTest {
                val result = marketplaceService.getMarketplaceListingsByReleaseIds(releaseIds)

                result.size shouldBe 1
                result[MarketplaceSeller("solo-seller")]?.size shouldBe 1
            }
        }

        test("should handle multiple listings from same seller for same release") {
            val releaseIds = listOf(1L)
            val listings = listOf(
                createTestListing(releaseId = 1L, sellerName = "seller-A", price = "$10.00"),
                createTestListing(releaseId = 1L, sellerName = "seller-A", price = "$12.00"),
                createTestListing(releaseId = 1L, sellerName = "seller-A", price = "$15.00")
            )

            every { mockCacheService.findByReleaseIdNotIn(releaseIds) } returns releaseIds
            every { mockCacheService.getAllCached(emptyList()) } returns emptyList()

            coEvery {
                mockBatchProcessor.processParallelBatch(
                    items = match { it.toSet() == releaseIds.toSet() },
                    batchSize = 20,
                    concurrency = 5,
                    processor = any()
                )
            } returns listOf(listings)

            runTest {
                val result = marketplaceService.getMarketplaceListingsByReleaseIds(releaseIds)

                result.size shouldBe 1
                result[MarketplaceSeller("seller-A")]?.size shouldBe 3
            }
        }

        test("should handle sellers with special characters in names") {
            val releaseIds = listOf(1L, 2L)
            val listings = listOf(
                createTestListing(releaseId = 1L, sellerName = "seller-ñ-ümlaut"),
                createTestListing(releaseId = 2L, sellerName = "seller-日本語")
            )

            every { mockCacheService.findByReleaseIdNotIn(releaseIds) } returns releaseIds
            every { mockCacheService.getAllCached(emptyList()) } returns emptyList()

            coEvery {
                mockBatchProcessor.processParallelBatch(
                    items = match { it.toSet() == releaseIds.toSet() },
                    batchSize = 20,
                    concurrency = 5,
                    processor = any()
                )
            } returns listOf(listings)

            runTest {
                val result = marketplaceService.getMarketplaceListingsByReleaseIds(releaseIds)

                result.size shouldBe 2
                result.keys.map { it.name } shouldBe listOf("seller-ñ-ümlaut", "seller-日本語")
            }
        }

        test("should handle very large number of sellers") {
            val releaseIds = (1L..500L).toList()
            // Each release has a unique seller
            val listings = releaseIds.map { id ->
                createTestListing(releaseId = id, sellerName = "seller-$id")
            }

            every { mockCacheService.findByReleaseIdNotIn(releaseIds) } returns releaseIds
            every { mockCacheService.getAllCached(emptyList()) } returns emptyList()

            coEvery {
                mockBatchProcessor.processParallelBatch(
                    items = match { it.toSet() == releaseIds.toSet() },
                    batchSize = 20,
                    concurrency = 5,
                    processor = any()
                )
            } returns listOf(listings)

            runTest {
                val result = marketplaceService.getMarketplaceListingsByReleaseIds(releaseIds)

                result.size shouldBe 500
                result.values.all { it.size == 1 } shouldBe true
            }
        }

        test("should preserve all listing details when grouping by seller") {
            val releaseIds = listOf(1L, 2L)
            val listing1 = createTestListing(
                releaseId = 1L,
                sellerName = "seller-A",
                title = "Album 1",
                price = "$10.00"
            )
            val listing2 = createTestListing(
                releaseId = 2L,
                sellerName = "seller-A",
                title = "Album 2",
                price = "$20.00"
            )

            every { mockCacheService.findByReleaseIdNotIn(releaseIds) } returns releaseIds
            every { mockCacheService.getAllCached(emptyList()) } returns emptyList()

            coEvery {
                mockBatchProcessor.processParallelBatch(
                    items = match { it.toSet() == releaseIds.toSet() },
                    batchSize = 20,
                    concurrency = 5,
                    processor = any()
                )
            } returns listOf(listOf(listing1, listing2))

            runTest {
                val result = marketplaceService.getMarketplaceListingsByReleaseIds(releaseIds)

                val sellerListings = result[MarketplaceSeller("seller-A")]!!
                sellerListings.size shouldBe 2
                sellerListings[0].title shouldBe "Album 1"
                sellerListings[0].price shouldBe "$10.00"
                sellerListings[1].title shouldBe "Album 2"
                sellerListings[1].price shouldBe "$20.00"
            }
        }

        test("should handle duplicate release IDs gracefully") {
            val releaseIds = listOf(1L, 1L, 2L, 2L, 3L)
            val uniqueReleaseIds = releaseIds
            val listings = listOf(
                createTestListing(releaseId = 1L, sellerName = "seller-A"),
                createTestListing(releaseId = 2L, sellerName = "seller-B"),
                createTestListing(releaseId = 3L, sellerName = "seller-C")
            )

            every { mockCacheService.findByReleaseIdNotIn(releaseIds) } returns uniqueReleaseIds
            every { mockCacheService.getAllCached(emptyList()) } returns emptyList()

            coEvery {
                mockBatchProcessor.processParallelBatch(
                    items = match { it == uniqueReleaseIds },
                    batchSize = 20,
                    concurrency = 5,
                    processor = any()
                )
            } returns listOf(listings)

            runTest {
                val result = marketplaceService.getMarketplaceListingsByReleaseIds(releaseIds)

                result.size shouldBe 3
                result.values.flatten().size shouldBe 3
            }
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
