package de.ezienecker.shared.discogs.collection

import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shared.discogs.client.auth.providers.DiscogsClientConfiguration
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

class ClientTest : StringSpec({

    "should return collection when response is OK" {
        val inputStream = this::class.java.classLoader.getResourceAsStream("responses/collection.json")

        val mockEngine = MockEngine {
            respond(
                content = inputStream?.readAllBytes()!!,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = DiscogsClient(mockEngine, DiscogsClientConfiguration())

        val collectionReleases = client.listUsersCollection("test-user", 1, 10)

        val pagination = collectionReleases.pagination.shouldNotBeNull()
        pagination.page shouldBe 1
        pagination.pages shouldBe 1
        pagination.perPage shouldBe 50
        pagination.items shouldBe 31

        val result = collectionReleases.result.shouldNotBeNull()
        result.size shouldBe 31
        result.first().id shouldBe 29294428
        result.first().basicInformation.title shouldBe "Burning Desire"
        result.first().basicInformation.artists shouldNotBe emptyList<Artist>()
        result.first().basicInformation.formats shouldNotBe emptyList<Format>()
        result.first().basicInformation.genres shouldNotBe emptyList<String>()
    }

    "should return empty collection when response is not OK" {

        val mockEngine = MockEngine {
            respond(
                content = "",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = DiscogsClient(mockEngine, DiscogsClientConfiguration())

        val collectionReleases = client.listUsersCollection("test-user", 1, 10)
        collectionReleases.result shouldBe emptyList()
    }

})
