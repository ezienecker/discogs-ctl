package de.ezienecker.shared.discogs.wantlist

import de.ezienecker.shared.discogs.client.auth.providers.DiscogsClientConfiguration
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

class WantlistApiClientTest : StringSpec({

    "should return wantlist when response is OK" {
        val inputStream = this::class.java.classLoader.getResourceAsStream("responses/wants.json")

        val mockEngine = MockEngine {
            respond(
                content = inputStream?.readAllBytes()!!,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = WantlistApiClient(mockEngine, DiscogsClientConfiguration())

        val response = client.listUsersWantList("test-user", 1, 10)
        response.status shouldBe HttpStatusCode.OK
        val wants = response.body<WantsResponse>().result
        wants.shouldNotBeNull()
        wants.first().id shouldBe 14622960
        wants.first().basicInformation.title shouldBe "May God Bless Your Hustle"
    }

    "should return empty wantlist when response is not OK" {

        val mockEngine = MockEngine {
            respond(
                content = "",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = WantlistApiClient(mockEngine, DiscogsClientConfiguration())

        val response = client.listUsersWantList("test-user", 1, 10)
        response.status shouldBe HttpStatusCode.BadRequest
    }

})
