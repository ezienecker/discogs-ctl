package de.ezienecker.shared.discogs.client

import co.touchlab.kermit.Logger
import de.ezienecker.shared.discogs.client.auth.providers.DiscogsAuthCredentials
import de.ezienecker.shared.discogs.client.auth.providers.DiscogsAuthentication
import de.ezienecker.shared.discogs.client.auth.providers.DiscogsClientConfiguration
import de.ezienecker.shared.discogs.client.auth.providers.DiscogsTokenAuthCredentials
import de.ezienecker.shared.discogs.client.auth.providers.discogs
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

@Suppress("unused")
private typealias KtorLogger = io.ktor.client.plugins.logging.Logger

class DiscogsClient(private val configuration: DiscogsClientConfiguration) {

    internal val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(Logging) {
            logger = KtorLogger.DEFAULT
            level = LogLevel.ALL
        }

        if (configuration.authentication.isAuthenticationPresent()) {
            install(Auth) {
                discogs {
                    credentials {
                        configuration.authentication.discogsAuthCredentials()
                    }

                    sendWithoutRequest {
                        true
                    }
                }
            }
        } else {
            Logger.i { "No authentication set. All requests are unauthenticated." }
        }
    }
}

fun DiscogsAuthentication.discogsAuthCredentials(): DiscogsAuthCredentials =
    token?.let {
        DiscogsTokenAuthCredentials(token)
    } ?: throw IllegalStateException("No credentials set.")
