package de.ezienecker.config.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.pair
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.core.configuration.model.Token
import de.ezienecker.core.configuration.model.Username
import de.ezienecker.core.configuration.service.ConfigurationService
import de.ezienecker.core.configuration.service.ConfigurationService.Companion.COLLECTION_CACHE_DURATION
import de.ezienecker.core.configuration.service.ConfigurationService.Companion.MARKETPLACE_LISTINGS_CACHE_DURATION
import de.ezienecker.core.configuration.service.ConfigurationService.Companion.SHOP_CACHE_DURATION
import de.ezienecker.core.configuration.service.ConfigurationService.Companion.TOKEN
import de.ezienecker.core.configuration.service.ConfigurationService.Companion.USERNAME
import de.ezienecker.core.configuration.service.ConfigurationService.Companion.WANTLIST_CACHE_DURATION
import kotlin.time.Duration

class Config : NoOpCliktCommand() {

    override fun help(context: Context) = """
        Display or modify discogs-ctl config file.
        
        Display the config file:
        discogs-ctl config view
        
        Modify the config file:
        discogs-ctl config set <property> <value>
        
        Show available properties:
        discogs-ctl config set --help
        
        The configuration file is located under ${'$'}{HOME}/.discogsctl/
        """.trimIndent()

    class Set(
        private val service: ConfigurationService,
        private val terminal: Terminal
    ) : CliktCommand() {

        override fun help(context: Context) = """
            Set an individual value in a discogs-ctl config file.
            
            # Set default username to John-Doe:
            discogs-ctl config set $USERNAME John-Doe
            
            # Set token so that the requests to the discogs server are authenticated:
            discogs-ctl config set $USERNAME John-Doe
            
            # Set the collection cache duration to 7 days:
            discogs-ctl config set $COLLECTION_CACHE_DURATION 7d
            
            # Set the marketplace listings cache duration to 1 hour:
            discogs-ctl config set $MARKETPLACE_LISTINGS_CACHE_DURATION 1h
            
            Properties:
            
            * `$USERNAME` (Default username whose collection, shop inventory and wantlist is used.), 
            
            * `$TOKEN` (Token used for authentication.),
            
            * `$COLLECTION_CACHE_DURATION` (Duration for which the collection is cached. If the cache duration is exceeded, the collection inventory is fetched from the discogs server again.),
            
            * `$MARKETPLACE_LISTINGS_CACHE_DURATION` (Duration for which the marketplace listings are cached. If the cache duration is exceeded, the marketplace listings are fetched from the discogs server again.),
            
            * `$SHOP_CACHE_DURATION` (Duration for which the shop inventory is cached. If the cache duration is exceeded, the shop inventory is fetched from the discogs server again.),
            
            * `$WANTLIST_CACHE_DURATION` (Duration for which the wantlist inventory is cached. If the cache duration is exceeded, the wantlist inventory is fetched from the discogs server again.)
            """.trimIndent()

        private val propertyPair by argument().pair()

        override fun run() {
            runCatching {
                when (propertyPair.first) {
                    USERNAME -> service.configureDefaultUser(Username(propertyPair.second))
                    TOKEN -> service.configureToken(Token(propertyPair.second))
                    COLLECTION_CACHE_DURATION -> service.configureDuration(
                        COLLECTION_CACHE_DURATION,
                        Duration.parse(propertyPair.second)
                    )

                    MARKETPLACE_LISTINGS_CACHE_DURATION -> service.configureDuration(
                        MARKETPLACE_LISTINGS_CACHE_DURATION,
                        Duration.parse(propertyPair.second)
                    )

                    SHOP_CACHE_DURATION -> service.configureDuration(
                        SHOP_CACHE_DURATION,
                        Duration.parse(propertyPair.second)
                    )

                    WANTLIST_CACHE_DURATION -> service.configureDuration(
                        WANTLIST_CACHE_DURATION,
                        Duration.parse(propertyPair.second)
                    )

                    else -> terminal.println(TextColors.brightRed("${propertyPair.first} is an unknown property"))
                }
            }.onFailure { exception ->
                terminal.println(TextColors.brightRed("Failed to set [${propertyPair.first}]: ${exception.message}"))
            }
        }
    }

    class View(
        private val service: ConfigurationService,
        private val terminal: Terminal
    ) : CliktCommand() {

        override fun help(context: Context) = """
            Displays the discogs-ctl config file.
            
            # Show all configured properties:
            discogs-ctl config view
            """.trimIndent()

        override fun run() {
            val properties = service.getConfiguration()

            if (properties.isEmpty()) {
                terminal.println("Nothing is currently configured")
            } else {
                terminal.println(table {
                    header {
                        cellBorders = Borders.NONE

                        row("Key", "Value")
                    }
                    body {
                        cellBorders = Borders.NONE

                        properties
                            .toSortedMap()
                            .forEach { (key, value) ->
                                row(key, value)
                            }
                    }
                })
            }
        }
    }
}
