@file:OptIn(ExperimentalTime::class)

package de.ezienecker

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.sources.PropertiesValueSource
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.collection.command.Collection
import de.ezienecker.collection.infrastructure.repository.CollectionCacheService
import de.ezienecker.collection.service.CollectionService
import de.ezienecker.config.command.Config
import de.ezienecker.core.configuration.service.ConfigurationService
import de.ezienecker.core.infrastructure.config.configureDatabaseConnection
import de.ezienecker.core.infrastructure.config.setupSchema
import de.ezienecker.core.infrastructure.discogs.collection.CollectionApiClient
import de.ezienecker.core.infrastructure.discogs.marketplace.ShopApiClient
import de.ezienecker.core.infrastructure.discogs.wantlist.WantlistApiClient
import de.ezienecker.core.version.VersionProvider
import de.ezienecker.shop.command.Shop
import de.ezienecker.shop.infrastructure.repository.ShopCacheService
import de.ezienecker.shop.service.ShopService
import de.ezienecker.wantlist.command.Wantlist
import de.ezienecker.wantlist.infrastructure.repository.WantlistCacheService
import de.ezienecker.wantlist.service.WantlistService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    val terminal = Terminal()
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val configurationService = ConfigurationService()
    configureDatabaseConnection().also {
        logger.debug { "Database connection configured successfully." }
    }

    setupSchema().also {
        logger.debug { "Database schema configured successfully." }
    }

    val setConfig = Config.Set(configurationService, terminal)
    val viewConfig = Config.View(configurationService, terminal)
    val clock = Clock.System

    val collectionService = CollectionService(
        client = CollectionApiClient(
            configuration = configurationService.getDiscogsClientConfiguration()
        ),
        cache = CollectionCacheService(clock, json),
    )
    val shopService = ShopService(
        client = ShopApiClient(configuration = configurationService.getDiscogsClientConfiguration()),
        cache = ShopCacheService(clock, json),
    )
    val wantlistService = WantlistService(
        client = WantlistApiClient(configuration = configurationService.getDiscogsClientConfiguration()),
        cache = WantlistCacheService(clock, json),
    )

    DiscogsCtl()
        .versionOption(VersionProvider.version)
        .subcommands(
            Config()
                .subcommands(
                    setConfig,
                    viewConfig
                ),
            Collection(collectionService, shopService, wantlistService, configurationService, terminal),
            Shop(shopService, wantlistService, configurationService, terminal),
            Wantlist(shopService, wantlistService, configurationService, terminal),
        ).main(args)
}

class DiscogsCtl : NoOpCliktCommand() {
    init {
        context {
            valueSource = PropertiesValueSource.from(
                "${System.getProperty("user.home")}/.discogsctl/config.properties"
            )
        }
    }

    override fun help(context: Context): String = """
        discogs-ctl makes it possible to display different inventories (shop, wantlist) and collection of a user and to compare them with each other.
    """.trimIndent()
}
