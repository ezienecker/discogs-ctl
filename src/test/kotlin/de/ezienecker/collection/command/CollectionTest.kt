package de.ezienecker.collection.command

import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.collection.service.CollectionService
import de.ezienecker.core.configuration.service.ConfigurationService
import de.ezienecker.core.infrastructure.discogs.collection.Artist
import de.ezienecker.core.infrastructure.discogs.collection.BasicInformation
import de.ezienecker.core.infrastructure.discogs.collection.Format
import de.ezienecker.core.infrastructure.discogs.collection.Label
import de.ezienecker.core.infrastructure.discogs.collection.Release
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

class CollectionTest : FunSpec({

    lateinit var mockCollectionService: CollectionService
    lateinit var mockShopService: ShopService
    lateinit var mockWantlistService: WantlistService
    lateinit var mockConfigurationService: ConfigurationService
    lateinit var terminal: Terminal
    lateinit var collection: Collection

    val testUsername = "test-user"

    beforeEach {
        mockCollectionService = mockk()
        mockShopService = mockk()
        mockWantlistService = mockk()
        mockConfigurationService = mockk()
        terminal = Terminal()
        collection = Collection(
            mockCollectionService,
            mockShopService,
            mockWantlistService,
            mockConfigurationService,
            terminal
        )
    }

    context("JSON output format") {
        test("should output releases as JSON when --output json is specified") {
            val testReleases = listOf(
                Release(
                    id = 1L,
                    instanceId = 100L,
                    dateAdded = "2023-01-01T00:00:00-07:00",
                    rating = 5,
                    basicInformation = BasicInformation(
                        id = 1L,
                        masterId = 10L,
                        masterUrl = "https://api.discogs.com/masters/10",
                        resourceUrl = "https://api.discogs.com/releases/1",
                        thumb = "https://example.com/thumb.jpg",
                        title = "Test Album",
                        year = 2020,
                        formats = listOf(Format(name = "LP", qty = "1", text = null, descriptions = listOf("Album"))),
                        labels = listOf(
                            Label(
                                name = "Test Label",
                                catNo = "TEST001",
                                entityType = "1",
                                entityTypeName = "Label",
                                id = 1L,
                                resourceUrl = "https://api.discogs.com/labels/1"
                            )
                        ),
                        artists = listOf(
                            Artist(
                                id = 1,
                                name = "Test Artist",
                                anv = "",
                                join = "",
                                role = "",
                                tracks = "",
                                resourceUrl = "https://api.discogs.com/artists/1"
                            )
                        ),
                        genres = listOf("Rock"),
                        styles = listOf("Alternative")
                    )
                )
            )

            coEvery { mockCollectionService.listCollectionByUser(testUsername, any(), any()) } returns Result.success(
                testReleases
            )
            coEvery { mockShopService.getIdsFromInventoryReleasesByUser(null) } returns emptySet()
            coEvery { mockWantlistService.getIdsFromWantlistReleasesByUser(null) } returns emptySet()

            val result = collection.test("--username $testUsername --output json")

            result.statusCode shouldBe 0

            val json = Json { ignoreUnknownKeys = true }
            val parsedReleases = assertDoesNotThrow {
                json.decodeFromString<List<Release>>(result.output)
            }

            parsedReleases.first().id shouldBe 1L
            parsedReleases.first().basicInformation.title shouldBe "Test Album"
            parsedReleases.first().basicInformation.artists[0].name shouldBe "Test Artist"
        }

        test("should output valid JSON structure") {
            val testReleases = listOf(
                Release(
                    id = 2L,
                    instanceId = 200L,
                    dateAdded = "2023-06-15T12:30:00-07:00",
                    rating = 4,
                    basicInformation = BasicInformation(
                        id = 2L,
                        masterId = 20L,
                        masterUrl = null,
                        resourceUrl = "https://api.discogs.com/releases/2",
                        thumb = "",
                        title = "Another Album",
                        year = 2021,
                        formats = listOf(Format(name = "CD", qty = "1", text = null, descriptions = emptyList())),
                        labels = listOf(
                            Label(
                                name = "Another Label",
                                catNo = "ANO002",
                                entityType = "1",
                                entityTypeName = "Label",
                                id = 2L,
                                resourceUrl = "https://api.discogs.com/labels/2"
                            )
                        ),
                        artists = listOf(
                            Artist(
                                id = 2,
                                name = "Another Artist",
                                anv = "",
                                join = "",
                                role = "",
                                tracks = "",
                                resourceUrl = "https://api.discogs.com/artists/2"
                            )
                        ),
                        genres = listOf("Electronic"),
                        styles = listOf("Ambient")
                    )
                )
            )

            coEvery { mockCollectionService.listCollectionByUser(testUsername, any(), any()) } returns Result.success(
                testReleases
            )
            coEvery { mockShopService.getIdsFromInventoryReleasesByUser(null) } returns emptySet()
            coEvery { mockWantlistService.getIdsFromWantlistReleasesByUser(null) } returns emptySet()

            val result = collection.test("--username $testUsername -o json")

            result.statusCode shouldBe 0

            val json = Json { ignoreUnknownKeys = true }
            val parsedReleases = assertDoesNotThrow {
                json.decodeFromString<List<Release>>(result.output)
            }

            parsedReleases.size shouldBe 1
            parsedReleases.first().id shouldBe 2L
            parsedReleases.first().rating shouldBe 4
            parsedReleases.first().basicInformation.title shouldBe "Another Album"
            parsedReleases.first().basicInformation.artists[0].name shouldBe "Another Artist"
        }

        test("should filter releases by shop when using JSON output") {
            val testReleases = listOf(
                Release(
                    id = 1L,
                    instanceId = 100L,
                    dateAdded = "2023-01-01T00:00:00-07:00",
                    rating = 5,
                    basicInformation = BasicInformation(
                        id = 100L,
                        masterId = 10L,
                        masterUrl = null,
                        resourceUrl = "https://api.discogs.com/releases/100",
                        thumb = "",
                        title = "In Shop",
                        year = 2020,
                        formats = listOf(Format(name = "LP", qty = "1", text = null, descriptions = emptyList())),
                        labels = listOf(
                            Label(
                                name = "Label",
                                catNo = "LAB001",
                                entityType = "1",
                                entityTypeName = "Label",
                                id = 1L,
                                resourceUrl = "https://api.discogs.com/labels/1"
                            )
                        ),
                        artists = listOf(
                            Artist(
                                id = 1,
                                name = "Shop Artist",
                                anv = "",
                                join = "",
                                role = "",
                                tracks = "",
                                resourceUrl = "https://api.discogs.com/artists/1"
                            )
                        ),
                        genres = emptyList(),
                        styles = emptyList()
                    )
                ),
                Release(
                    id = 2L,
                    instanceId = 200L,
                    dateAdded = "2023-02-01T00:00:00-07:00",
                    rating = 3,
                    basicInformation = BasicInformation(
                        id = 200L,
                        masterId = 20L,
                        masterUrl = null,
                        resourceUrl = "https://api.discogs.com/releases/200",
                        thumb = "",
                        title = "Not In Shop",
                        year = 2021,
                        formats = listOf(Format(name = "CD", qty = "1", text = null, descriptions = emptyList())),
                        labels = listOf(
                            Label(
                                name = "Label",
                                catNo = "LAB002",
                                entityType = "1",
                                entityTypeName = "Label",
                                id = 2L,
                                resourceUrl = "https://api.discogs.com/labels/2"
                            )
                        ),
                        artists = listOf(
                            Artist(
                                id = 2,
                                name = "Not Shop Artist",
                                anv = "",
                                join = "",
                                role = "",
                                tracks = "",
                                resourceUrl = "https://api.discogs.com/artists/2"
                            )
                        ),
                        genres = emptyList(),
                        styles = emptyList()
                    )
                )
            )

            coEvery { mockCollectionService.listCollectionByUser(testUsername, any(), any()) } returns Result.success(
                testReleases
            )
            coEvery { mockShopService.getIdsFromInventoryReleasesByUser("shop-user") } returns setOf(100L)
            coEvery { mockWantlistService.getIdsFromWantlistReleasesByUser(null) } returns emptySet()

            val result = collection.test("--username $testUsername --filtered-by-shop shop-user --output json")

            result.statusCode shouldBe 0

            val json = Json { ignoreUnknownKeys = true }
            val parsedReleases = assertDoesNotThrow {
                json.decodeFromString<List<Release>>(result.output)
            }

            parsedReleases.size shouldBe 1
            parsedReleases.first().basicInformation.id shouldBe 100L
            parsedReleases.first().basicInformation.title shouldBe "In Shop"
        }

        test("should filter releases by wantlist when using JSON output") {
            val testReleases = listOf(
                Release(
                    id = 1L,
                    instanceId = 100L,
                    dateAdded = "2023-01-01T00:00:00-07:00",
                    rating = 5,
                    basicInformation = BasicInformation(
                        id = 300L,
                        masterId = 30L,
                        masterUrl = null,
                        resourceUrl = "https://api.discogs.com/releases/300",
                        thumb = "",
                        title = "In Wantlist",
                        year = 2022,
                        formats = listOf(Format(name = "LP", qty = "1", text = null, descriptions = emptyList())),
                        labels = listOf(
                            Label(
                                name = "Label",
                                catNo = "LAB003",
                                entityType = "1",
                                entityTypeName = "Label",
                                id = 3L,
                                resourceUrl = "https://api.discogs.com/labels/3"
                            )
                        ),
                        artists = listOf(
                            Artist(
                                id = 3,
                                name = "Wanted Artist",
                                anv = "",
                                join = "",
                                role = "",
                                tracks = "",
                                resourceUrl = "https://api.discogs.com/artists/3"
                            )
                        ),
                        genres = emptyList(),
                        styles = emptyList()
                    )
                ),
                Release(
                    id = 2L,
                    instanceId = 200L,
                    dateAdded = "2023-02-01T00:00:00-07:00",
                    rating = 4,
                    basicInformation = BasicInformation(
                        id = 400L,
                        masterId = 40L,
                        masterUrl = null,
                        resourceUrl = "https://api.discogs.com/releases/400",
                        thumb = "",
                        title = "Not In Wantlist",
                        year = 2023,
                        formats = listOf(Format(name = "CD", qty = "1", text = null, descriptions = emptyList())),
                        labels = listOf(
                            Label(
                                name = "Label",
                                catNo = "LAB004",
                                entityType = "1",
                                entityTypeName = "Label",
                                id = 4L,
                                resourceUrl = "https://api.discogs.com/labels/4"
                            )
                        ),
                        artists = listOf(
                            Artist(
                                id = 4,
                                name = "Not Wanted Artist",
                                anv = "",
                                join = "",
                                role = "",
                                tracks = "",
                                resourceUrl = "https://api.discogs.com/artists/4"
                            )
                        ),
                        genres = emptyList(),
                        styles = emptyList()
                    )
                )
            )

            coEvery { mockCollectionService.listCollectionByUser(testUsername, any(), any()) } returns Result.success(
                testReleases
            )
            coEvery { mockShopService.getIdsFromInventoryReleasesByUser(null) } returns emptySet()
            coEvery { mockWantlistService.getIdsFromWantlistReleasesByUser("wantlist-user") } returns setOf(300L)

            val result = collection.test("--username $testUsername --filtered-by-wantlist wantlist-user --output json")

            result.statusCode shouldBe 0

            val json = Json { ignoreUnknownKeys = true }
            val parsedReleases = assertDoesNotThrow {
                json.decodeFromString<List<Release>>(result.output)
            }

            parsedReleases.size shouldBe 1
            parsedReleases.first().basicInformation.id shouldBe 300L
            parsedReleases.first().basicInformation.title shouldBe "In Wantlist"
        }

        test("should not output JSON when --output json is not specified") {
            val testReleases = listOf(
                Release(
                    id = 1L,
                    instanceId = 100L,
                    dateAdded = "2023-01-01T00:00:00-07:00",
                    rating = 5,
                    basicInformation = BasicInformation(
                        id = 1L,
                        masterId = 10L,
                        masterUrl = "https://api.discogs.com/masters/10",
                        resourceUrl = "https://api.discogs.com/releases/1",
                        thumb = "https://example.com/thumb.jpg",
                        title = "Test Album",
                        year = 2020,
                        formats = listOf(Format(name = "LP", qty = "1", text = null, descriptions = listOf("Album"))),
                        labels = listOf(
                            Label(
                                name = "Test Label",
                                catNo = "TEST001",
                                entityType = "1",
                                entityTypeName = "Label",
                                id = 1L,
                                resourceUrl = "https://api.discogs.com/labels/1"
                            )
                        ),
                        artists = listOf(
                            Artist(
                                id = 1,
                                name = "Test Artist",
                                anv = "",
                                join = "",
                                role = "",
                                tracks = "",
                                resourceUrl = "https://api.discogs.com/artists/1"
                            )
                        ),
                        genres = listOf("Rock"),
                        styles = listOf("Alternative")
                    )
                )
            )

            coEvery { mockCollectionService.listCollectionByUser(testUsername, any(), any()) } returns Result.success(
                testReleases
            )
            coEvery { mockShopService.getIdsFromInventoryReleasesByUser(null) } returns emptySet()
            coEvery { mockWantlistService.getIdsFromWantlistReleasesByUser(null) } returns emptySet()

            val result = collection.test("--username $testUsername")

            result.statusCode shouldBe 0

            val json = Json { ignoreUnknownKeys = true }
            val exception = assertThrows<SerializationException> {
                json.decodeFromString<List<Release>>(result.output)
            }

            exception.message shouldStartWith "Expected start of the array"
        }
    }
})
