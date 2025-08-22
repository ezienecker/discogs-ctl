package de.ezienecker.shared.discogs.collection

import de.ezienecker.shared.discogs.client.auth.providers.DiscogsClientConfiguration
import de.ezienecker.shared.discogs.shared.CollectionReleases
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

class CollectionApiClientTest : StringSpec({

    "should return collection when response is OK" {
        val inputStream = this::class.java.classLoader.getResourceAsStream("responses/collection.json")

        val mockEngine = MockEngine {
            respond(
                content = inputStream?.readAllBytes()!!,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = CollectionApiClient(mockEngine, DiscogsClientConfiguration())

        val response = client.listUsersCollection("test-user", 1, 10)
        response.status shouldBe HttpStatusCode.OK
        val releases = response.body<CollectionReleases>().result
        releases.shouldNotBeNull()
        releases.first().id shouldBe 29294428
        releases.first().basicInformation.title shouldBe "Burning Desire"
    }

    "should return empty collection when response is not OK" {

        val mockEngine = MockEngine {
            respond(
                content = "",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = CollectionApiClient(mockEngine, DiscogsClientConfiguration())

        val response = client.listUsersCollection("test-user", 1, 10)
        response.status shouldBe HttpStatusCode.BadRequest
    }

})
