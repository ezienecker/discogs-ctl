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
import de.ezienecker.shared.discogs.client.DiscogsClient
import de.ezienecker.shop.command.Shop
import de.ezienecker.shop.service.ShopService
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

    val discogsClient = DiscogsClient(configuration = configurationService.getDiscogsClientConfiguration())
    val collectionService = CollectionService(discogsClient)
    val shopService = ShopService(discogsClient)
    val wantlistService = WantlistService(discogsClient)

    DiscogsCtl()
        .subcommands(
            Config()
                .subcommands(
                    setConfig,
                    viewConfig
                ),
            Collection(collectionService, shopService, wantlistService, configurationService, terminal, json),
            Shop(shopService, wantlistService, configurationService, terminal, json),
            Wantlist(shopService, wantlistService, configurationService, terminal, json),
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
