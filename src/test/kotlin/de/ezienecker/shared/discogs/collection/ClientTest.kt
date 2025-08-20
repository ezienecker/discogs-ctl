package de.ezienecker.shared.discogs.collection

import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shared.discogs.client.auth.providers.DiscogsClientConfiguration
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = DiscogsClient(mockEngine, DiscogsClientConfiguration())

        val collectionReleases = client.listUsersCollection("test-user", 1, 10)
        collectionReleases.result.shouldNotBeNull()
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
