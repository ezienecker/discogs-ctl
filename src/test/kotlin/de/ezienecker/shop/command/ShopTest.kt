package de.ezienecker.shop.command

import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.core.configuration.service.ConfigurationService
import de.ezienecker.core.infrastructure.discogs.marketplace.Artist
import de.ezienecker.core.infrastructure.discogs.marketplace.Comments
import de.ezienecker.core.infrastructure.discogs.marketplace.Condition
import de.ezienecker.core.infrastructure.discogs.marketplace.Currency
import de.ezienecker.core.infrastructure.discogs.marketplace.Description
import de.ezienecker.core.infrastructure.discogs.marketplace.Format
import de.ezienecker.core.infrastructure.discogs.marketplace.Listing
import de.ezienecker.core.infrastructure.discogs.marketplace.Price
import de.ezienecker.core.infrastructure.discogs.marketplace.Release
import de.ezienecker.core.infrastructure.discogs.marketplace.Seller
import de.ezienecker.core.infrastructure.discogs.marketplace.Status
import de.ezienecker.core.infrastructure.discogs.marketplace.Title
import de.ezienecker.core.infrastructure.discogs.marketplace.Uri
import de.ezienecker.core.infrastructure.discogs.marketplace.Url
import de.ezienecker.core.infrastructure.discogs.marketplace.Username
import de.ezienecker.shop.service.ShopService
import de.ezienecker.wantlist.service.WantlistService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ShopTest : FunSpec({

    lateinit var mockShopService: ShopService
    lateinit var mockWantlistService: WantlistService
    lateinit var mockConfigurationService: ConfigurationService
    lateinit var terminal: Terminal
    lateinit var shop: Shop

    val testUsername = "test-user"

    beforeEach {
        mockShopService = mockk()
        mockWantlistService = mockk()
        mockConfigurationService = mockk()
        terminal = Terminal()
        shop = Shop(mockShopService, mockWantlistService, mockConfigurationService, terminal)
    }

    context("JSON output format") {
        test("should output listings as JSON when --output json is specified") {
            val testListings = listOf(
                Listing(
                    id = 1L,
                    resourceUrl = Url("https://api.discogs.com/marketplace/listings/1"),
                    uri = Uri("https://www.discogs.com/sell/item/1"),
                    status = Status.FOR_SALE,
                    mediaCondition = Condition.NEAR_MINT,
                    sleeveCondition = Condition.NEAR_MINT,
                    comments = Comments("Test comment"),
                    price = Price(value = 19.99, currency = Currency("EUR")),
                    seller = Seller(
                        username = Username("seller"),
                        htmlUrl = Url("https://www.discogs.com/user/seller"),
                        resourceUrl = Url("https://api.discogs.com/users/seller")
                    ),
                    release = Release(
                        description = Description("Test Artist - Test Album"),
                        artist = Artist("Test Artist"),
                        format = Format("LP"),
                        title = Title("Test Album"),
                        id = 456L
                    )
                )
            )

            coEvery { mockShopService.listInventoryByUser(testUsername, any(), any()) } returns Result.success(
                testListings
            )
            coEvery { mockWantlistService.getIdsFromWantlistReleasesByUser(null) } returns emptySet()

            val result = shop.test("--username $testUsername --output json")

            result.statusCode shouldBe 0

            val json = Json { ignoreUnknownKeys = true }
            val parsedListings = assertDoesNotThrow {
                json.decodeFromString<List<Listing>>(result.stdout)
            }

            parsedListings.first().id shouldBe 1L
            parsedListings.first().status shouldBe Status.FOR_SALE
            parsedListings.first().release.artist shouldBe Artist("Test Artist")
            parsedListings.first().release.title shouldBe Title("Test Album")
            parsedListings.first().price.value shouldBe 19.99
        }

        test("should output valid JSON structure") {
            val testListings = listOf(
                Listing(
                    id = 1L,
                    resourceUrl = Url("https://api.discogs.com/marketplace/listings/1"),
                    uri = Uri("https://www.discogs.com/sell/item/1"),
                    status = Status.FOR_SALE,
                    mediaCondition = Condition.VERY_GOOD_PLUS,
                    sleeveCondition = Condition.VERY_GOOD,
                    comments = Comments(""),
                    price = Price(value = 29.99, currency = Currency("USD")),
                    seller = Seller(
                        username = Username("another-seller"),
                        htmlUrl = Url("https://www.discogs.com/user/another-seller"),
                        resourceUrl = Url("https://api.discogs.com/users/another-seller")
                    ),
                    release = Release(
                        description = Description("Another Artist - Another Album"),
                        artist = Artist("Another Artist"),
                        format = Format("CD"),
                        title = Title("Another Album"),
                        id = 999L
                    )
                )
            )

            coEvery { mockShopService.listInventoryByUser(testUsername, any(), any()) } returns Result.success(
                testListings
            )
            coEvery { mockWantlistService.getIdsFromWantlistReleasesByUser(null) } returns emptySet()

            val result = shop.test("--username $testUsername -o json")

            result.statusCode shouldBe 0

            val json = Json { ignoreUnknownKeys = true }
            val parsedListings = assertDoesNotThrow {
                json.decodeFromString<List<Listing>>(result.output)
            }

            parsedListings.size shouldBe 1
            parsedListings.first().id shouldBe 1L
            parsedListings.first().status shouldBe Status.FOR_SALE
            parsedListings.first().release.artist shouldBe Artist("Another Artist")
            parsedListings.first().price.value shouldBe 29.99
        }

        test("should filter listings when using JSON output") {
            val testListings = listOf(
                Listing(
                    id = 1L,
                    resourceUrl = Url("https://api.discogs.com/marketplace/listings/1"),
                    uri = Uri("https://www.discogs.com/sell/item/1"),
                    status = Status.FOR_SALE,
                    mediaCondition = Condition.NEAR_MINT,
                    sleeveCondition = Condition.NEAR_MINT,
                    comments = Comments(""),
                    price = Price(value = 15.99, currency = Currency("EUR")),
                    seller = Seller(
                        username = Username("seller1"),
                        htmlUrl = Url("https://www.discogs.com/user/seller1"),
                        resourceUrl = Url("https://api.discogs.com/users/seller1")
                    ),
                    release = Release(
                        description = Description("Filtered Artist - Filtered Album"),
                        artist = Artist("Filtered Artist"),
                        format = Format("LP"),
                        title = Title("Filtered Album"),
                        id = 100L
                    )
                ),
                Listing(
                    id = 2L,
                    resourceUrl = Url("https://api.discogs.com/marketplace/listings/2"),
                    uri = Uri("https://www.discogs.com/sell/item/2"),
                    status = Status.FOR_SALE,
                    mediaCondition = Condition.MINT,
                    sleeveCondition = Condition.MINT,
                    comments = Comments(""),
                    price = Price(value = 25.99, currency = Currency("EUR")),
                    seller = Seller(
                        username = Username("seller2"),
                        htmlUrl = Url("https://www.discogs.com/user/seller2"),
                        resourceUrl = Url("https://api.discogs.com/users/seller2")
                    ),
                    release = Release(
                        description = Description("Not Filtered - Not in Wantlist"),
                        artist = Artist("Not Filtered"),
                        format = Format("CD"),
                        title = Title("Not in Wantlist"),
                        id = 200L
                    )
                )
            )

            coEvery { mockShopService.listInventoryByUser(testUsername, any(), any()) } returns Result.success(
                testListings
            )
            coEvery { mockWantlistService.getIdsFromWantlistReleasesByUser("wantlist-user") } returns setOf(100L)

            val result = shop.test("--username $testUsername --filtered-by-wantlist wantlist-user --output json")

            result.statusCode shouldBe 0

            val json = Json { ignoreUnknownKeys = true }
            val parsedListings = assertDoesNotThrow {
                json.decodeFromString<List<Listing>>(result.output)
            }

            parsedListings.size shouldBe 1
            parsedListings.first().release.id shouldBe 100L
            parsedListings.first().release.artist shouldBe Artist("Filtered Artist")
        }

        test("should not output JSON when --output json is not specified") {
            val testListings = listOf(
                Listing(
                    id = 1L,
                    resourceUrl = Url("https://api.discogs.com/marketplace/listings/1"),
                    uri = Uri("https://www.discogs.com/sell/item/1"),
                    status = Status.FOR_SALE,
                    mediaCondition = Condition.NEAR_MINT,
                    sleeveCondition = Condition.NEAR_MINT,
                    comments = Comments(""),
                    price = Price(value = 19.99, currency = Currency("EUR")),
                    seller = Seller(
                        username = Username("seller"),
                        htmlUrl = Url("https://www.discogs.com/user/seller"),
                        resourceUrl = Url("https://api.discogs.com/users/seller")
                    ),
                    release = Release(
                        description = Description("Test Artist - Test Album"),
                        artist = Artist("Test Artist"),
                        format = Format("LP"),
                        title = Title("Test Album"),
                        id = 456L
                    )
                )
            )

            coEvery { mockShopService.listInventoryByUser(testUsername, any(), any()) } returns Result.success(
                testListings
            )
            coEvery { mockWantlistService.getIdsFromWantlistReleasesByUser(null) } returns emptySet()

            val result = shop.test("--username $testUsername")

            result.statusCode shouldBe 0

            val json = Json { ignoreUnknownKeys = true }
            val exception = assertThrows<SerializationException> {
                json.decodeFromString<List<Listing>>(result.output)
            }

            exception.message shouldStartWith "Expected start of the array"
        }
    }
})
