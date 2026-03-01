package de.ezienecker.wantlist.command

import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.core.configuration.service.ConfigurationService
import de.ezienecker.core.infrastructure.discogs.wantlist.Artist
import de.ezienecker.core.infrastructure.discogs.wantlist.BasicInformation
import de.ezienecker.core.infrastructure.discogs.wantlist.Format
import de.ezienecker.core.infrastructure.discogs.wantlist.Label
import de.ezienecker.core.infrastructure.discogs.wantlist.Url
import de.ezienecker.core.infrastructure.discogs.wantlist.Want
import de.ezienecker.shop.service.ShopService
import de.ezienecker.wantlist.service.MarketplaceService
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

class WantlistTest : FunSpec({

    lateinit var mockShopService: ShopService
    lateinit var mockWantlistService: WantlistService
    lateinit var mockMarketplaceService: MarketplaceService
    lateinit var mockConfigurationService: ConfigurationService
    lateinit var terminal: Terminal
    lateinit var wantlist: Wantlist

    val testUsername = "test-user"

    beforeEach {
        mockShopService = mockk()
        mockWantlistService = mockk()
        mockMarketplaceService = mockk()
        mockConfigurationService = mockk()
        terminal = Terminal()
        wantlist = Wantlist(
            mockShopService,
            mockWantlistService,
            mockMarketplaceService,
            mockConfigurationService,
            terminal,
        )
    }

    context("JSON output format") {
        test("should output wants as JSON when --output json is specified") {
            val testWants = listOf(
                Want(
                    rating = 5,
                    basicInformation = BasicInformation(
                        formats = listOf(Format(text = null, qty = "1", descriptions = listOf("Album"), name = "LP")),
                        thumb = "https://example.com/thumb.jpg",
                        coverImage = "https://example.com/cover.jpg",
                        title = "Wanted Album",
                        labels = listOf(
                            Label(
                                resourceUrl = Url("https://api.discogs.com/labels/1"),
                                entityType = "1",
                                catalogNumber = "WANT001",
                                id = 1,
                                name = "Wanted Label"
                            )
                        ),
                        year = 2020,
                        artists = listOf(
                            Artist(
                                join = "",
                                name = "Wanted Artist",
                                anv = "",
                                tracks = "",
                                role = "",
                                resourceUrl = Url("https://api.discogs.com/artists/1"),
                                id = 1
                            )
                        ),
                        resourceUrl = Url("https://api.discogs.com/releases/1"),
                        id = 1L
                    ),
                    resourceUrl = Url("https://api.discogs.com/users/test-user/wants/1"),
                    releaseId = 100L
                )
            )

            coEvery {
                mockWantlistService.listWantsByUser(
                    testUsername,
                    any(),
                    any()
                )
            } returns Result.success(testWants)
            coEvery { mockShopService.getIdsFromInventoryReleasesByUser(null) } returns emptySet()

            val result = wantlist.test("--username $testUsername --output json")

            result.statusCode shouldBe 0

            val json = Json { ignoreUnknownKeys = true }
            val wants = assertDoesNotThrow {
                json.decodeFromString<List<Want>>(result.output)
            }

            wants.first().releaseId shouldBe 100L
            wants.first().rating shouldBe 5
            wants.first().basicInformation.title shouldBe "Wanted Album"
            wants.first().basicInformation.artists[0].name shouldBe "Wanted Artist"
        }

        test("should output valid JSON structure") {
            val testWants = listOf(
                Want(
                    rating = 4,
                    basicInformation = BasicInformation(
                        formats = listOf(
                            Format(
                                text = "Reissue",
                                qty = "2",
                                descriptions = listOf("LP", "Album"),
                                name = "Vinyl"
                            )
                        ),
                        thumb = "",
                        coverImage = "",
                        title = "Another Wanted Album",
                        labels = listOf(
                            Label(
                                resourceUrl = Url("https://api.discogs.com/labels/2"),
                                entityType = "1",
                                catalogNumber = "WANT002",
                                id = 2,
                                name = "Another Label"
                            )
                        ),
                        year = 2021,
                        artists = listOf(
                            Artist(
                                join = "",
                                name = "Another Wanted Artist",
                                anv = "",
                                tracks = "",
                                role = "",
                                resourceUrl = Url("https://api.discogs.com/artists/2"),
                                id = 2
                            )
                        ),
                        resourceUrl = Url("https://api.discogs.com/releases/2"),
                        id = 2L
                    ),
                    resourceUrl = Url("https://api.discogs.com/users/test-user/wants/2"),
                    releaseId = 200L
                )
            )

            coEvery {
                mockWantlistService.listWantsByUser(
                    testUsername,
                    any(),
                    any()
                )
            } returns Result.success(testWants)
            coEvery { mockShopService.getIdsFromInventoryReleasesByUser(null) } returns emptySet()

            val result = wantlist.test("--username $testUsername -o json")

            result.statusCode shouldBe 0

            val json = Json { ignoreUnknownKeys = true }
            val parsedWants = assertDoesNotThrow {
                json.decodeFromString<List<Want>>(result.output)
            }

            parsedWants.size shouldBe 1
            parsedWants.first().releaseId shouldBe 200L
            parsedWants.first().rating shouldBe 4
            parsedWants.first().basicInformation.title shouldBe "Another Wanted Album"
            parsedWants.first().basicInformation.artists[0].name shouldBe "Another Wanted Artist"
        }

        test("should filter wants by shop when using JSON output") {
            val testWants = listOf(
                Want(
                    rating = 5,
                    basicInformation = BasicInformation(
                        formats = listOf(Format(text = null, qty = "1", descriptions = listOf("Album"), name = "LP")),
                        thumb = "",
                        coverImage = "",
                        title = "In Shop",
                        labels = listOf(
                            Label(
                                resourceUrl = Url("https://api.discogs.com/labels/3"),
                                entityType = "1",
                                catalogNumber = "SHOP001",
                                id = 3,
                                name = "Shop Label"
                            )
                        ),
                        year = 2022,
                        artists = listOf(
                            Artist(
                                join = "",
                                name = "Shop Artist",
                                anv = "",
                                tracks = "",
                                role = "",
                                resourceUrl = Url("https://api.discogs.com/artists/3"),
                                id = 3
                            )
                        ),
                        resourceUrl = Url("https://api.discogs.com/releases/100"),
                        id = 100L
                    ),
                    resourceUrl = Url("https://api.discogs.com/users/test-user/wants/100"),
                    releaseId = 1000L
                ),
                Want(
                    rating = 3,
                    basicInformation = BasicInformation(
                        formats = listOf(Format(text = null, qty = "1", descriptions = listOf("Album"), name = "CD")),
                        thumb = "",
                        coverImage = "",
                        title = "Not In Shop",
                        labels = listOf(
                            Label(
                                resourceUrl = Url("https://api.discogs.com/labels/4"),
                                entityType = "1",
                                catalogNumber = "NOSHOP001",
                                id = 4,
                                name = "No Shop Label"
                            )
                        ),
                        year = 2023,
                        artists = listOf(
                            Artist(
                                join = "",
                                name = "Not Shop Artist",
                                anv = "",
                                tracks = "",
                                role = "",
                                resourceUrl = Url("https://api.discogs.com/artists/4"),
                                id = 4
                            )
                        ),
                        resourceUrl = Url("https://api.discogs.com/releases/200"),
                        id = 200L
                    ),
                    resourceUrl = Url("https://api.discogs.com/users/test-user/wants/200"),
                    releaseId = 2000L
                )
            )

            coEvery {
                mockWantlistService.listWantsByUser(
                    testUsername,
                    any(),
                    any()
                )
            } returns Result.success(testWants)
            coEvery { mockShopService.getIdsFromInventoryReleasesByUser("shop-user") } returns setOf(100L)

            val result = wantlist.test("--username $testUsername --filtered-by-shop shop-user --output json")

            result.statusCode shouldBe 0

            val json = Json { ignoreUnknownKeys = true }
            val parsedWants = assertDoesNotThrow {
                json.decodeFromString<List<Want>>(result.output)
            }

            parsedWants.size shouldBe 1
            parsedWants.first().basicInformation.id shouldBe 100L
            parsedWants.first().basicInformation.title shouldBe "In Shop"
        }

        test("should return empty array when no wants match filter") {
            val testWants = listOf(
                Want(
                    rating = 5,
                    basicInformation = BasicInformation(
                        formats = listOf(Format(text = null, qty = "1", descriptions = listOf("Album"), name = "LP")),
                        thumb = "",
                        coverImage = "",
                        title = "Not Matching",
                        labels = listOf(
                            Label(
                                resourceUrl = Url("https://api.discogs.com/labels/5"),
                                entityType = "1",
                                catalogNumber = "NOMATCH001",
                                id = 5,
                                name = "No Match Label"
                            )
                        ),
                        year = 2024,
                        artists = listOf(
                            Artist(
                                join = "",
                                name = "No Match Artist",
                                anv = "",
                                tracks = "",
                                role = "",
                                resourceUrl = Url("https://api.discogs.com/artists/5"),
                                id = 5
                            )
                        ),
                        resourceUrl = Url("https://api.discogs.com/releases/500"),
                        id = 500L
                    ),
                    resourceUrl = Url("https://api.discogs.com/users/test-user/wants/500"),
                    releaseId = 5000L
                )
            )

            coEvery {
                mockWantlistService.listWantsByUser(
                    testUsername,
                    any(),
                    any()
                )
            } returns Result.success(testWants)
            coEvery { mockShopService.getIdsFromInventoryReleasesByUser("shop-user") } returns setOf(999L)

            val result = wantlist.test("--username $testUsername --filtered-by-shop shop-user --output json")

            result.statusCode shouldBe 0

            val json = Json { ignoreUnknownKeys = true }
            val parsedWants = json.decodeFromString<List<Want>>(result.output)

            parsedWants.size shouldBe 0
        }

        test("should not output JSON when --output json is not specified") {
            val testWants = listOf(
                Want(
                    rating = 5,
                    basicInformation = BasicInformation(
                        formats = listOf(Format(text = null, qty = "1", descriptions = listOf("Album"), name = "LP")),
                        thumb = "",
                        coverImage = "",
                        title = "Test Album",
                        labels = listOf(
                            Label(
                                resourceUrl = Url("https://api.discogs.com/labels/1"),
                                entityType = "1",
                                catalogNumber = "TEST001",
                                id = 1,
                                name = "Test Label"
                            )
                        ),
                        year = 2020,
                        artists = listOf(
                            Artist(
                                join = "",
                                name = "Test Artist",
                                anv = "",
                                tracks = "",
                                role = "",
                                resourceUrl = Url("https://api.discogs.com/artists/1"),
                                id = 1
                            )
                        ),
                        resourceUrl = Url("https://api.discogs.com/releases/1"),
                        id = 1L
                    ),
                    resourceUrl = Url("https://api.discogs.com/users/test-user/wants/1"),
                    releaseId = 100L
                )
            )

            coEvery {
                mockWantlistService.listWantsByUser(
                    testUsername,
                    any(),
                    any()
                )
            } returns Result.success(testWants)
            coEvery { mockShopService.getIdsFromInventoryReleasesByUser(null) } returns emptySet()

            val result = wantlist.test("--username $testUsername")

            result.statusCode shouldBe 0

            val json = Json { ignoreUnknownKeys = true }
            val exception = assertThrows<SerializationException> {
                json.decodeFromString<List<Want>>(result.output)
            }

            exception.message shouldStartWith "Expected start of the array"
        }
    }
})
