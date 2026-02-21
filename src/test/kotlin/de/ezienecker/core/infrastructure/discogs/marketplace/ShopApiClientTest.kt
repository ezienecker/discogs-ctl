package de.ezienecker.core.infrastructure.discogs.marketplace

import de.ezienecker.core.infrastructure.discogs.client.auth.providers.DiscogsClientConfiguration
import de.ezienecker.core.infrastructure.discogs.shop.ShopApiClient
import de.ezienecker.core.infrastructure.discogs.shop.ShopResponse
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

class ShopApiClientTest : StringSpec({

    "should return shop inventory when response is OK" {
        val inputStream = this::class.java.classLoader.getResourceAsStream("responses/shop.json")

        val mockEngine = MockEngine {
            respond(
                content = inputStream?.readAllBytes()!!,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = ShopApiClient(mockEngine, DiscogsClientConfiguration())

        val response = client.listUsersShop("test-user", 1, 10, "artist", "asc")
        response.status shouldBe HttpStatusCode.OK
        val listings = response.body<ShopResponse>().result
        listings.shouldNotBeNull()
        listings.first().id shouldBe 2237957062
        listings.first().release.title.value shouldBe "The 8: Vol. 3"
    }

    "should return empty shop inventory when response is not OK" {

        val mockEngine = MockEngine {
            respond(
                content = "",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = ShopApiClient(mockEngine, DiscogsClientConfiguration())

        val response = client.listUsersShop("test-user", 1, 10, "artist", "asc")
        response.status shouldBe HttpStatusCode.BadRequest
    }

})
