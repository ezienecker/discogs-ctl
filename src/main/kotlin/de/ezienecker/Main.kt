package de.ezienecker

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.sources.PropertiesValueSource
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.collection.command.Collection
import de.ezienecker.collection.service.CollectionService
import de.ezienecker.config.command.Config
import de.ezienecker.shared.configuration.service.ConfigurationService
import de.ezienecker.shared.discogs.collection.CollectionApiClient
import de.ezienecker.shared.discogs.marketplace.InventoryApiClient
import de.ezienecker.shared.discogs.wantlist.WantlistApiClient
import de.ezienecker.shop.command.Shop
import de.ezienecker.shop.service.InventoryService
import de.ezienecker.wantlist.command.Wantlist
import de.ezienecker.wantlist.service.WantlistService
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    val terminal = Terminal()
    val json = Json {
        prettyPrint = true
    }

    val configurationService = ConfigurationService()
    val setConfig = Config.Set(configurationService, terminal)
    val viewConfig = Config.View(configurationService, terminal)

    val collectionService = CollectionService(
        client = CollectionApiClient(configuration = configurationService.getDiscogsClientConfiguration())
    )
    val inventoryService = InventoryService(
        client = InventoryApiClient(configuration = configurationService.getDiscogsClientConfiguration())
    )
    val wantlistService = WantlistService(
        client = WantlistApiClient(configuration = configurationService.getDiscogsClientConfiguration())
    )

    DiscogsCtl()
        .subcommands(
            Config()
                .subcommands(
                    setConfig,
                    viewConfig
                ),
            Collection(collectionService, inventoryService, wantlistService, configurationService, terminal, json),
            Shop(inventoryService, wantlistService, configurationService, terminal, json),
            Wantlist(inventoryService, wantlistService, configurationService, terminal, json),
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
        discogs-ctl makes it possible to display different inventories (store, wantlist) of a user and to compare them with each other.
    """.trimIndent()
}
